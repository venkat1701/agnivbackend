package io.github.venkat1701.agnivbackend.service.skill;

import io.github.venkat1701.agnivbackend.embeddings.SkillEmbedding;
import io.github.venkat1701.agnivbackend.model.Skill;
import io.github.venkat1701.agnivbackend.model.User;
import io.github.venkat1701.agnivbackend.repository.embeddings.SkillEmbeddingRepository;
import io.github.venkat1701.agnivbackend.repository.auth.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;

@Service
public class SkillEmbeddingService {

    private final ChatClient chatClient;
    private final Map<String, List<Float>> skillEmbeddings = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final SkillEmbeddingRepository skillEmbeddingRepository;

    @Autowired
    public SkillEmbeddingService(ChatClient chatClient, UserRepository userRepository, SkillEmbeddingRepository skillEmbeddingRepository) {
        this.chatClient = chatClient;
        this.userRepository = userRepository;
        this.skillEmbeddingRepository = skillEmbeddingRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateEmbeddingsOnStartup() {
        loadSkillsAndGenerateEmbeddings("data/skills.yaml");
    }

    private void loadSkillsAndGenerateEmbeddings(String yamlFile) {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(yamlFile)) {
            if (inputStream == null) {
                throw new FileNotFoundException("YAML file not found: " + yamlFile);
            }
            Map<String, List<Map<String, String>>> data = yaml.load(inputStream);
            List<Map<String, String>> skills = data.get("skills");

            for (Map<String, String> skill : skills) {
                String name = skill.get("name");
                String category = skill.get("category");
                String level = skill.get("level");

                // Check if embedding exists in DB
                skillEmbeddingRepository.findBySkillName(name).ifPresentOrElse(
                        skillEmbedding -> skillEmbeddings.put(name, skillEmbedding.getEmbedding()),
                        () -> {
                            // Generate and store in DB if not found
                            List<Float> embedding = generateEmbeddingUsingLLM(name, category, level);
                            skillEmbeddings.put(name, embedding);
                            saveSkillEmbedding(name, embedding);
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Float> generateEmbeddingUsingLLM(String name, String category, String level) {
        String prompt = String.format(
                "Generate a 3-dimensional embedding for the skill %s in the category %s at level %s" +
                        "The embedding should represent the skill's importance, complexity, and versatility on a scale of 0 to 1." +
                        "Return only the three float values separated by commas, without any additional text or explanation.",
                name, category, level
        );

        ChatResponse response = this.chatClient.prompt(new Prompt(prompt)).call().chatResponse();
        String content = response.getResult().getOutput().getContent().trim();

        return parseEmbedding(content);
    }

    private List<Float> parseEmbedding(String content) {
        try {
            String[] values = content.split(",");
            return asList(
                    Float.parseFloat(values[0].trim()),
                    Float.parseFloat(values[1].trim()),
                    Float.parseFloat(values[2].trim())
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Arrays.asList(0.5f, 0.5f, 0.5f);
        }
    }

    private void saveSkillEmbedding(String skillName, List<Float> embedding) {
        SkillEmbedding skillEmbedding = new SkillEmbedding(skillName, embedding);
        skillEmbeddingRepository.save(skillEmbedding);
    }

    public List<Float> generateCurrentUserEmbedding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Float> embedding = new ArrayList<>();
        for (Skill skill : user.getSkillList()) {
            embedding.addAll(getOrGenerateSkillEmbedding(skill.getSkill()));
        }
        return normalizeEmbedding(embedding);
    }

    private List<Float> normalizeEmbedding(List<Float> embedding) {
        List<Float> normalizedEmbedding = new ArrayList<>();
        float sum = 0.0f;
        for (float value : embedding) {
            sum += value * value;
        }
        float magnitude = (float) Math.sqrt(sum);
        for (float value : embedding) {
            normalizedEmbedding.add(value / magnitude);
        }
        return normalizedEmbedding;
    }

    private List<Float> getOrGenerateSkillEmbedding(String skillName) {
        return skillEmbeddings.computeIfAbsent(skillName, name -> {
            // First check the DB before generating
            return skillEmbeddingRepository.findBySkillName(name)
                    .map(SkillEmbedding::getEmbedding)
                    .orElseGet(() -> {
                        List<Float> embedding = generateEmbeddingUsingLLM(name, "unknown", "unknown");
                        saveSkillEmbedding(name, embedding);
                        return embedding;
                    });
        });
    }

    public List<Float> getSkillEmbedding(String skillName) {
        return skillEmbeddings.getOrDefault(skillName, Collections.emptyList());
    }

    public List<String> findSimilarSkills(List<Float> targetEmbedding, int topN) {
        PriorityQueue<Map.Entry<String, Float>> queue = new PriorityQueue<>(
                (a, b) -> Float.compare(b.getValue(), a.getValue())
        );
        for (Map.Entry<String, List<Float>> entry : skillEmbeddings.entrySet()) {
            String name = entry.getKey();
            float similarity = cosineSimilarity(targetEmbedding, entry.getValue());
            queue.offer(new AbstractMap.SimpleEntry<>(entry.getKey(), similarity));
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < topN && !queue.isEmpty(); i++) {
            result.add(queue.poll().getKey());
        }

        return result;
    }

    private float cosineSimilarity(List<Float> v1, List<Float> v2) {
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += v1.get(i) * v1.get(i);
            normB += v2.get(i) * v2.get(i);
        }
        normA = (float) Math.sqrt(normA);
        normB = (float) Math.sqrt(normB);
        return (dotProduct / (normA * normB));
    }
}

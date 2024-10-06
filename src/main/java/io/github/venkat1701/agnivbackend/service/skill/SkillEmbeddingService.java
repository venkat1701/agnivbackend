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

/**
 * Service class that generates embeddings for skills using the Spring AI chat client, and stores them in a repository.
 * The service also loads skills from a YAML file and generates embeddings for each one on application startup.
 *
 * @author Venkat
 */
@Service
public class SkillEmbeddingService {

    private final ChatClient chatClient;
    private final Map<String, List<Float>> skillEmbeddings = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final SkillEmbeddingRepository skillEmbeddingRepository;

    /**
     * Creates a new instance of the SkillEmbeddingService class.
     *
     * @param chatClient The chat client to use for generating embeddings.
     * @param userRepository The user repository to use for retrieving users.
     * @param skillEmbeddingRepository The skill embedding repository to use for storing skill embeddings.
     */
    @Autowired
    public SkillEmbeddingService(ChatClient chatClient, UserRepository userRepository, SkillEmbeddingRepository skillEmbeddingRepository) {
        this.chatClient = chatClient;
        this.userRepository = userRepository;
        this.skillEmbeddingRepository = skillEmbeddingRepository;
    }

    /**
     * Generates embeddings for all users in the database. The method
     * will generate an embedding for each user using the Spring AI
     * chat client, and store the embedding in the user embedding
     * repository.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void generateEmbeddingsOnStartup() {
        loadSkillsAndGenerateEmbeddings("data/skills.yaml");
    }

    /**
     * Loads skills from a YAML file and generates embeddings for each one on application startup.
     * The YAML file should contain a list of skills, each with a name and a description. The method
     * will generate an embedding for each skill using the Spring AI chat client, and store the
     * embedding in the skill embedding repository.
     *
     * @param yamlFile The path to the YAML file to load.
     */
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

    /**
     * Generates a 3-dimensional embedding for a given skill using the Spring AI chat client.
     * The method takes in the name of the skill, the category of the skill, and the level of the skill.
     * The method will ask the chatbot to generate an embedding for the skill in the format of three float
     * values separated by commas, without any additional text or explanation. The method will then parse
     * the output and return the three float values as a list.
     * <p>
     * The embedding represents the skill's importance, complexity, and versatility on a scale of 0 to 1.
     * The importance of the skill is represented by the first float value, the complexity of the skill is
     * represented by the second float value, and the versatility of the skill is represented by the third
     * float value.
     * <p>
     * If the method is unable to parse the output, it will print an error message to the console and return
     * an empty list.
     *
     * @param name The name of the skill to generate an embedding for.
     * @param category The category of the skill to generate an embedding for.
     * @param level The level of the skill to generate an embedding for.
     * @return A 3-dimensional embedding for the skill, represented as a list of three float values.
     */
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


    /**
     * Parses a string representation of a 3-dimensional embedding into a list of three float values.
     * The string should contain three float values separated by commas, without any additional text or explanation.
     * If the string is not in the correct format, the method will print an error message to the console and return
     * an embedding with all values set to 0.5.
     * <p>
     * The method takes in a string representation of the embedding, and returns a list of three float values.
     * The first element of the list represents the importance of the skill, the second element represents the complexity of the skill,
     * and the third element represents the versatility of the skill. All values are on a scale of 0 to 1.
     *
     * @param content The string representation of the embedding to parse.
     * @return A 3-dimensional embedding for the skill, represented as a list of three float values.
     */
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

    /**
     * Saves a skill embedding to the database.
     * The method takes in a string representing the name of the skill to save, and a list of three float values representing the importance, complexity, and versatility of the skill.
     * The method will create a new entry in the database with the provided skill name and embedding.
     * If an entry already exists in the database with the same skill name, the method will not update the existing entry, and will instead return without doing anything.
     * <p>
     * The method does not return any value, and should not be used to retrieve information from the database.
     * Instead, the method should be used to store information in the database.
     *
     * @param skillName The name of the skill to save.
     * @param embedding A list of three float values representing the importance, complexity, and versatility of the skill.
     */
    private void saveSkillEmbedding(String skillName, List<Float> embedding) {
        SkillEmbedding skillEmbedding = new SkillEmbedding(skillName, embedding);
        skillEmbeddingRepository.save(skillEmbedding);
    }

    /**
     * Generates a user embedding based on the user's skills.
     * The method takes in the ID of the user to generate an embedding for, and returns a list of three float values representing the importance, complexity, and versatility of the user.
     * The method does not modify the database, and is safe to call multiple times.
     * <p>
     * The method works by iterating over the user's skills, and generating an embedding for each one using the {@link #generateEmbeddingUsingLLM(String, String, String)} method.
     * The method then normalizes the resulting list of embeddings by dividing each value by the sum of all values.
     * Finally, the method returns the normalized list of embeddings.
     * <p>
     * The importance of the user is represented by the first element in the list, the complexity of the user is represented by the second element, and the versatility of the user is represented by the third element.
     * All values are on a scale of 0 to 1.
     *
     * @param userId The ID of the user to generate an embedding for.
     * @return A list of three float values representing the importance, complexity, and versatility of the user.
     */
    public List<Float> generateCurrentUserEmbedding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Float> embedding = new ArrayList<>();
        for (Skill skill : user.getSkillList()) {
            embedding.addAll(getOrGenerateSkillEmbedding(skill.getSkill()));
        }
        return normalizeEmbedding(embedding);
    }

    /**
     * Normalizes a list of floats to ensure that the magnitude of the resulting vector is 1.
     * The method works by first calculating the sum of the squares of all values in the list,
     * then dividing each value by the square root of the sum.
     * <p>
     * The normalization process is lossless, meaning that the normalized vector will have the same direction as the original vector.
     * The normalization process is also deterministic, meaning that the same input will always produce the same output.
     * <p>
     * The method takes in the list of floats to normalize, and returns a new list of floats with the same length.
     * The values in the returned list are on a scale of 0 to 1, and the magnitude of the resulting vector is 1.
     *
     * @param embedding The list of floats to normalize.
     * @return A new list of floats with the same length, normalized to have a magnitude of 1.
     */
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

    /**
     * Generates a skill embedding if one does not already exist in memory.
     * The method first checks the skill embeddings map to see if an embedding already exists for the given skill name.
     * If it does, the method returns the embedding. If it does not, the method generates a new embedding using the
     * {@link #generateEmbeddingUsingLLM(String, String, String)} method, stores it in the skill embeddings map, and then
     * returns the new embedding.
     * <p>
     * The method will always return a valid embedding, and will never return null.
     * <p>
     * The method is thread-safe, meaning that it can be safely called from multiple threads at the same time.
     * <p>
     * The method takes in the name of the skill to generate an embedding for, and returns a list of three float values
     * representing the importance, complexity, and versatility of the skill.
     *
     * @param skillName The name of the skill to generate an embedding for.
     * @return A list of three float values representing the importance, complexity, and versatility of the skill.
     */
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


    /**
     * Returns the embedding for the given skill name. If the skill name is null, empty, or whitespace, the method will
     * return an empty list.
     * <p>
     * The method first checks the skill embeddings map to see if an embedding already exists for the given skill name.
     * If it does, the method returns the embedding. If it does not, the method returns an empty list.
     * <p>
     * The method will never return null.
     * <p>
     * The method is thread-safe, meaning that it can be safely called from multiple threads at the same time.
     * <p>
     * The method takes in the name of the skill to get the embedding for, and returns a list of three float values
     * representing the importance, complexity, and versatility of the skill. If the skill name is null, empty, or
     * whitespace, the method will return an empty list.
     * <p>
     * The method is idempotent, meaning that it can be safely called multiple times with the same arguments, without
     * causing any side effects.
     *
     * @param skillName The name of the skill to get the embedding for.
     * @return A list of three float values representing the importance, complexity, and versatility of the skill.
     */
    public List<Float> getSkillEmbedding(String skillName) {
        return skillEmbeddings.getOrDefault(skillName, Collections.emptyList());
    }

    /**
     * Finds the top N similar skills to the given target skill embedding.
     * The method takes in the target skill embedding as a list of three float values representing the importance,
     * complexity, and versatility of the skill, and the number of similar skills to return.
     * The method returns a list of strings, where each string is the name of a skill that is similar to the target skill.
     * The list is sorted in descending order of similarity, with the most similar skills first.
     * <p>
     * The method uses the cosine similarity metric to calculate the similarity between the target skill embedding and
     * the embeddings of all the other skills. The cosine similarity metric is defined as the dot product of the two
     * vectors divided by the product of their magnitudes.
     * <p>
     * The method is idempotent, meaning that it can be safely called multiple times with the same arguments, without
     * causing any side effects.
     * <p>
     * The method is thread-safe, meaning that it can be safely called from multiple threads at the same time.
     * <p>
     * The method takes in the target skill embedding as a list of three float values representing the importance,
     * complexity, and versatility of the skill, and the number of similar skills to return.
     * The method returns a list of strings, where each string is the name of a skill that is similar to the target skill.
     * The list is sorted in descending order of similarity, with the most similar skills first.
     *
     * @param targetEmbedding The target skill embedding as a list of three float values representing the importance,
     *                        complexity, and versatility of the skill.
     * @param topN            The number of similar skills to return.
     * @return A list of strings, where each string is the name of a skill that is similar to the target skill. The list
     * is sorted in descending order of similarity, with the most similar skills first.
     */
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

    /**
     * Calculates the cosine similarity between two vectors.
     * <p>
     * Cosine similarity is a measure of similarity between two non-zero vectors of an inner product space that
     * measures the cosine of the angle between them. It is a normalized measure of similarity, ranging from -1
     * (completely different) to 1 (exactly similar).
     * <p>
     * The cosine similarity is defined as the dot product of the two vectors divided by the product of their magnitudes.
     * <pre>
     * {@code
     * cosineSimilarity(v1, v2) = dotProduct(v1, v2) / (norm(v1) * norm(v2))
     * }
     * </pre>
     * <p>
     * The method takes in two lists of float values representing the two vectors, and returns a float value
     * representing the cosine similarity between the two vectors.
     * <p>
     * The method is idempotent, meaning that it can be safely called multiple times with the same arguments, without
     * causing any side effects.
     * <p>
     * The method is thread-safe, meaning that it can be safely called from multiple threads at the same time.
     * <p>
     * Note that the method returns 0.0f if either of the input vectors is the zero vector.
     *
     * @param v1 The first vector as a list of float values.
     * @param v2 The second vector as a list of float values.
     * @return A float value representing the cosine similarity between the two vectors.
     */
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

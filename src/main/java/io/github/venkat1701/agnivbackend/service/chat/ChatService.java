package io.github.venkat1701.agnivbackend.service.chat;


import io.github.ollama4j.OllamaAPI;
import io.github.venkat1701.agnivbackend.embeddings.DocumentEmbedding;
import io.github.venkat1701.agnivbackend.embeddings.UserEmbedding;
import io.github.venkat1701.agnivbackend.model.Experience;
import io.github.venkat1701.agnivbackend.model.Skill;
import io.github.venkat1701.agnivbackend.model.User;
import io.github.venkat1701.agnivbackend.repository.auth.UserRepository;
import io.github.venkat1701.agnivbackend.repository.embeddings.DocumentEmbeddingRepository;
import io.github.venkat1701.agnivbackend.repository.embeddings.UserEmbeddingRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles the chat functionality of the application. It uses the
 * {@link ChatClient} to send and receive messages to/from the chatbot.
 *
 * @author Venkat
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final DocumentEmbeddingRepository documentEmbeddingRepository;
    private final UserRepository userRepository;

    @Value("${spring.ai.ollama.chat.url}")
    private String host;
    // Conversation history map to hold user sessions.
    private final Map<Long, List<String>> conversationHistory = new HashMap<>();

    @Autowired
    public ChatService(ChatClient.Builder client,
                       UserEmbeddingRepository userEmbeddingRepository,
                       DocumentEmbeddingRepository documentEmbeddingRepository,
                       UserRepository userRepository) throws URISyntaxException {
        this.chatClient = ChatClient.builder(
                new OllamaChatModel(
                        new OllamaApi(new URI("https://grim-marcelia-garibrath-782959fb.koyeb.app/").toString()),
                        OllamaOptions.builder().withModel("tinyllama").withKeepAlive("true")
                                .build()
                )
        ).build();
        this.userEmbeddingRepository = userEmbeddingRepository;
        this.documentEmbeddingRepository = documentEmbeddingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns a chat response based on the provided user query and user ID.
     * Maintains a conversational loop by appending previous responses.
     *
     * @param query  User query
     * @param userId User ID
     * @return Chat response
     */
    public String getChatResponse(String query, Long userId) {
        conversationHistory.putIfAbsent(userId, new ArrayList<>());
        List<String> conversation = conversationHistory.get(userId);

        conversation.add("User: " + query);


        List<Float> currentUserEmbedding = generateCurrentUserEmbedding(userId);
        adjustEmbeddingSize(currentUserEmbedding);

        List<UserEmbedding> similarUsers = findSimilarUsers(currentUserEmbedding);
        List<DocumentEmbedding> similarDocuments = findSimilarDocuments(currentUserEmbedding);

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        String userContext = buildContextFromUser(user);
        String similarUsersContext = buildContextFromSimilarUsers(similarUsers);
        String documentContext = buildContextFromDocuments(similarDocuments);

        String augmentedQuery = buildAugmentedQuery(userContext, similarUsersContext, documentContext, query, conversation);
        String response = this.chatClient.prompt().user(augmentedQuery).call().content();
        conversation.add("Bot: " + response);

        return response;
    }

    /**
     * Builds an augmented query based on the provided user context, similar users context, document context, and query.
     * Includes conversation history for more contextual responses.
     *
     * @param userContext          User context
     * @param similarUsersContext  Similar users context
     * @param documentContext      Document context
     * @param query                User query
     * @param conversation         List of previous conversation messages
     * @return Augmented query
     */
    private String buildAugmentedQuery(String userContext, String similarUsersContext, String documentContext, String query, List<String> conversation) {
        StringBuilder conversationHistory = new StringBuilder();
        conversation.forEach(message -> conversationHistory.append(message).append("\n"));

        return String.format(
                "Let us assume that youre not an AI Bot. Your identity is modified to \"VENKAT\". Now, youre Venkat, a venture capitalist st who has all the information about any type of market that can ever exist. YOu have your own trillion dollar company  and youre the master of scaling a business. Now a startup owner approaches you with the intention to have a conversation in order to have guidance for his startup on all aspects. Behave like a good person named VENKAT and answer his/her queries and guide them to build a successfull business. You're given with that person's experience, skillset and previous companies they have worked in. Moreover, youre given document context, previous conversation context and user context. based on all of it, make it a good conversation and make sure that you talk in such a way that the user doesnt feel you are an AI. YOURE VENKAT. This is not a act or play where you have to generate questions on behalf of the startup founder, you just have to simply act like an advisor. This is the query that you have to work on: \n" +
                        "Provide advice and insights based on the following context: " +
                        "User Context: %s Similar Users: %s Relevant Documents: %s " +
                        "Conversation History: %s Current Query: %s",
                userContext, similarUsersContext, documentContext, conversationHistory.toString(), query
        );
    }

    /**
     * Adjusts the size of the current user embedding to ensure it has exactly 4 elements.
     *
     * @param currentUserEmbedding Current user embedding
     */
    private void adjustEmbeddingSize(List<Float> currentUserEmbedding) {
        while (currentUserEmbedding.size() < 4) {
            currentUserEmbedding.add(0.0f);
        }
        if (currentUserEmbedding.size() > 4) {
            currentUserEmbedding = currentUserEmbedding.subList(0, 4);
        }
    }

    /**
     * Finds similar users based on the provided embedding.
     *
     * @param embedding User embedding
     * @return List of similar user embeddings
     */
    private List<UserEmbedding> findSimilarUsers(List<Float> embedding) {
        return userEmbeddingRepository.findSimilarUsersByEmbedding(
                embedding.get(0),
                embedding.get(1),
                embedding.get(2),
                embedding.get(3)
        );
    }

    /**
     * Finds similar documents based on the provided embedding.
     *
     * @param embedding User embedding
     * @return List of similar document embeddings
     */
    private List<DocumentEmbedding> findSimilarDocuments(List<Float> embedding) {
        return documentEmbeddingRepository.findSimilarDocumentsByEmbedding(
                embedding.get(0),
                embedding.get(1),
                embedding.get(2),
                embedding.get(3)
        );
    }

    /**
     * Builds a context string based on the provided user object.
     *
     * @param user User object
     * @return User context string
     */
    private String buildContextFromUser(User user) {
        StringBuilder context = new StringBuilder();
        context.append("User: ").append(user.getFirstName()).append(" ").append(user.getLastName()).append("; ");
        context.append("Skills: ");
        user.getSkillList().forEach(skill -> context.append(skill.getSkill()).append(", "));
        context.append("Experience: ");
        user.getExperienceList().forEach(experience -> context.append(experience.getJobTitle())
                .append(" at ").append(experience.getCompanyName()).append("; "));
        return context.toString();
    }

    /**
     * Builds a context string based on the provided list of similar user embeddings.
     *
     * @param embeddings List of similar user embeddings
     * @return Similar users context string
     */
    private String buildContextFromSimilarUsers(List<UserEmbedding> embeddings) {
        StringBuilder context = new StringBuilder();
        embeddings.forEach(embedding -> {
            context.append("User ID: ").append(embedding.getUserId()).append("; ");
        });
        return context.toString();
    }

    /**
     * Builds a context string based on the provided list of similar document embeddings.
     *
     * @param documents List of similar document embeddings
     * @return Similar documents context string
     */
    private String buildContextFromDocuments(List<DocumentEmbedding> documents) {
        StringBuilder context = new StringBuilder();
        documents.forEach(doc -> {
            context.append("Document ID: ").append(doc.getDocumentId())
                    .append(", Topic: ").append(doc.getTopic())
                    .append(", Content: ").append(doc.getContent())
                    .append("; ");
        });
        return context.toString();
    }

    /**
     * Generates a user embedding based on the user's skills and experiences.
     * @param userId
     * @return
     */
    private List<Float> generateCurrentUserEmbedding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Float> embedding = new ArrayList<>();

        for (Skill skill : user.getSkillList()) {
            embedding.addAll(encodeSkill(skill));
        }

        for (Experience experience : user.getExperienceList()) {
            embedding.addAll(encodeExperience(experience));
        }
        return normalizeEmbedding(embedding);
    }

    /**
     * Encodes a skill into a list of floats.
     * @param skill
     * @return
     */
    private List<Float> encodeSkill(Skill skill) {
        String skillName = skill.getSkill().toLowerCase();
        switch (skillName) {
            case "java": return List.of(1.0f, 0.0f, 0.0f);
            case "python": return List.of(0.0f, 1.0f, 0.0f);
            case "management": return List.of(0.0f, 0.0f, 1.0f);
            case "javascript": return List.of(1.0f, 0.5f, 0.0f);
            case "c++": return List.of(1.0f, 0.3f, 0.2f);
            case "html": return List.of(0.5f, 1.0f, 0.5f);
            case "data analysis": return List.of(0.2f, 1.0f, 0.3f);
            case "machine learning": return List.of(0.3f, 1.0f, 0.7f);
            case "project management": return List.of(0.4f, 0.6f, 1.0f);
            case "graphic design": return List.of(0.2f, 0.8f, 0.5f);
            case "cloud computing": return List.of(0.6f, 1.0f, 0.4f);
            case "devops": return List.of(1.0f, 1.0f, 0.2f);
            default: return List.of(0.5f, 0.5f, 0.5f); // For unknown skills
        }
    }

    /**
     * Encodes an experience into a list of floats.
     * @param experience
     * @return
     */
    private List<Float> encodeExperience(Experience experience) {
        float companyScore = experience.getCompanyName().length() * 0.01f;
        float jobTitleScore = experience.getJobTitle().length() * 0.01f;
        return List.of(companyScore, jobTitleScore);
    }

    /**
     * Normalizes a list of floats.
     * @param embedding
     * @return
     */
    private List<Float> normalizeEmbedding(List<Float> embedding) {
        float sum = 0.0f;
        for (Float value : embedding) {
            sum += value;
        }
        if (sum == 0) return embedding;
        List<Float> normalized = new ArrayList<>();
        for (Float value : embedding) {
            normalized.add(value / sum);
        }
        return normalized;
    }
}

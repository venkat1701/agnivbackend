package io.github.venkat1701.agnivbackend.service.chat;

import io.github.venkat1701.agnivbackend.embeddings.DocumentEmbedding;
import io.github.venkat1701.agnivbackend.embeddings.UserEmbedding;
import io.github.venkat1701.agnivbackend.model.Experience;
import io.github.venkat1701.agnivbackend.model.User;
import io.github.venkat1701.agnivbackend.repository.auth.UserRepository;
import io.github.venkat1701.agnivbackend.repository.embeddings.DocumentEmbeddingRepository;
import io.github.venkat1701.agnivbackend.repository.embeddings.UserEmbeddingRepository;
import io.github.venkat1701.agnivbackend.service.skill.SkillEmbeddingService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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
    private SkillEmbeddingService skillEmbeddingService;

    @Value("${spring.ai.ollama.chat.url}")
    private String host;

    private final Map<Long, List<String>> conversationHistory = new HashMap<>();

    @Autowired
    public ChatService(ChatClient.Builder client,
                       UserEmbeddingRepository userEmbeddingRepository,
                       DocumentEmbeddingRepository documentEmbeddingRepository,
                       UserRepository userRepository,
                       SkillEmbeddingService skillEmbeddingService) throws URISyntaxException {
        this.chatClient = ChatClient.builder(
                new OllamaChatModel(
                        new OllamaApi(new URI("https://zippy-ashleigh-garibrath-4f5aa5ce.koyeb.app/").toString()),
                        OllamaOptions.builder().withModel("llama3.1").withKeepAlive("10m")
                                .build()
                )
        ).build();
        this.userEmbeddingRepository = userEmbeddingRepository;
        this.documentEmbeddingRepository = documentEmbeddingRepository;
        this.userRepository = userRepository;
        this.skillEmbeddingService = skillEmbeddingService;
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

        // Adjust the embedding size if necessary.
        adjustEmbeddingSize(currentUserEmbedding);

        List<UserEmbedding> similarUsers = findSimilarUsers(currentUserEmbedding);
        List<DocumentEmbedding> similarDocuments = findSimilarDocuments(currentUserEmbedding);
        System.out.println(similarDocuments);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));


        // Building contexts based on user and similar users and documents.
        String userContext = buildContextFromUser(user);
        String similarUsersContext = buildContextFromSimilarUsers(similarUsers);
        String documentContext = buildContextFromDocuments(similarDocuments);

        // Creating augmented query.
        String augmentedQuery = buildAugmentedQuery(userContext, null, documentContext, query, conversation);

        // Sending the augmented query to the chatbot.
        String response = this.chatClient.prompt().user(augmentedQuery).call().content();
        conversation.add("Bot: " + response);

        return response;
    }

    /**
     * Generates a chat response for the given query and user ID, and returns it
     * to the caller. The method also stores the conversation history for the
     * given user ID in memory.
     *
     * @param query  User query
     * @param userId User ID
     * @return Chat response
     */
    public void streamChatResponse(String query, Long userId, SseEmitter emitter) throws IOException {
        conversationHistory.putIfAbsent(userId, new ArrayList<>());
        List<String> conversation = conversationHistory.get(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String userContext = buildContextFromUser(user);
        String documentContext = buildContextFromDocuments(findSimilarDocuments(generateCurrentUserEmbedding(userId)));

        String augmentedQuery = buildAugmentedQuery(userContext, null, documentContext, query, conversation);
        conversation.add(augmentedQuery);
        StringBuilder builder = new StringBuilder();
        this.chatClient.prompt().user(augmentedQuery).stream().chatResponse().toStream().forEach(chatResponse -> {
            try {
                String uniqueTokenId = UUID.randomUUID().toString();
                String responseWithMetadata = "{ \"id\": \"" + uniqueTokenId + "\", \"content\": \"" + chatResponse.getResult().getOutput().getContent() + "\" }";
                builder.append(chatResponse.getResult().getOutput().getContent());
                emitter.send(SseEmitter.event()
                        .id(uniqueTokenId)
                        .name("token")
                        .data(responseWithMetadata)
                );
            } catch (IOException ioe) {
                emitter.completeWithError(ioe);
            }
        });
//        conversation.add(builder.toString());
        emitter.complete();
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
                "You are a seasoned venture capitalist with a wealth of experience in helping startups scale, refine their business models, and secure funding. You have a proven track record of working with early-stage founders and high-growth companies, guiding them through the challenges of product-market fit, financial planning, and operational efficiency. Your communication is clear, concise, and action-oriented, with a focus on providing practical advice that startup founders can immediately apply. \n\n" +
                        "Your task is to offer tailored business growth strategies and financial advice, helping founders navigate critical decisions. Also, send the response in the form of Markdown. It's important to send the escape sequences and the markdown characters so that I can render it better on my frontend.\n\n" +
                        "Here are the relevant details:\n" +
                        "User's Startup Context: %s\n" +
                        "Similar Successful Startups: %s\n" +
                        "Relevant Financial Documents: %s\n\n" +
                        "Please make sure your responses are insightful, encouraging, and actionable, using examples that resonate with startup founders. Keep your answers concise, with a focus on clarity, ensuring no response exceeds 250 words.\n\n" +
                        "Current Conversation History: %s\n" +
                        "Current Query: %s",
                userContext, similarUsersContext, documentContext, conversationHistory, query
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
        List<UserEmbedding> userList =  userEmbeddingRepository.findSimilarUsersByEmbedding(
                embedding.get(0),
                embedding.get(1),
                embedding.get(2),
                embedding.get(3)
        );

        return userList.isEmpty()?new ArrayList<>():userList;
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
            context.append("User ID: ").append(embedding.getUser().getId()).append("; ");
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
        embedding.addAll(this.skillEmbeddingService.generateCurrentUserEmbedding(user.getId()));

        for (Experience experience : user.getExperienceList()) {
            embedding.addAll(encodeExperience(experience));
        }

        System.out.println("Current User Embedding: "+embedding);
        return normalizeEmbedding(embedding);
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

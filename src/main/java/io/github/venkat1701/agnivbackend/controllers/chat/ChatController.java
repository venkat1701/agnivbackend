package io.github.venkat1701.agnivbackend.controllers.chat;

import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.venkat1701.agnivbackend.service.chat.ChatService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@CrossOrigin(origins = "http://localhost:3000")
/**
 * Handles all the chat-related operations in the application. This class is
 * responsible for getting the chat response from the chat service based on the
 * provided query and user ID.
 *
 * <p>This class is annotated with {@link RestController} and contains a single
 * method {@link #getChatResponse(String, Long)} that is annotated with
 * {@link GetMapping}. This method takes two parameters, a query string and an
 * optional user ID. If the user ID is null, the method returns a string
 * indicating that the user ID is required. Otherwise, it calls the
 * {@link ChatService#getChatResponse(String, Long)} method with the provided
 * query and user ID and returns the result.</p>
 *
 * @author Venkat
 */
@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Gets the chat response for the given query and user ID.
     *
     * @param query  the query string
     * @param userId the user ID
     * @return the chat response
     */
    @GetMapping("/chat/query")
    public String getChatResponse(@RequestParam String query, @RequestParam(required = false) Long userId) throws OllamaBaseException, IOException, InterruptedException {
        if (userId == null) {
            return "User ID is required.";
        }
        return chatService.getChatResponse(query, userId);
    }
}
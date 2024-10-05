package io.github.venkat1701.agnivbackend.controllers.chat.stream;


import io.github.venkat1701.agnivbackend.service.chat.ChatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@CrossOrigin(origins="http://localhost:3000", allowCredentials = "true")
public class ChatStreamingController {


    private ChatService chatService;

    @Autowired
    public ChatStreamingController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping(value="/stream/query", produces= MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatResponse(@RequestParam String query, @RequestParam Long userId) {
        SseEmitter emitter = new SseEmitter(0L);

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            try{
                chatService.streamChatResponse(query, userId, emitter);
            }catch(Exception e) {
                emitter.completeWithError(e);
            } finally {
                service.shutdown();
            }
        });
        return emitter;
    }

}

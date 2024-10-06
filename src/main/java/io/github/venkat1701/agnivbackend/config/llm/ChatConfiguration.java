package io.github.venkat1701.agnivbackend.config.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class ChatConfiguration {

    @Value("${spring.ai.ollama.chat.url}")
    private String url;

    @Bean
    public ChatClient chatClient() throws URISyntaxException {
        OllamaApi ollamaApi = new OllamaApi(new URI(url).toString());
        var options = OllamaOptions.builder()
                .withModel("llama3.1")
                .withKeepAlive("10m")
                .build();
        return ChatClient.builder(
                new OllamaChatModel(
                        ollamaApi,
                        options
                )
        ).build();
    }
}

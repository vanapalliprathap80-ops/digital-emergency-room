package com.emergency.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeminiClient {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-flash}")
    private String defaultModel;

    private final RestTemplate restTemplate;

    public GeminiClient() {
        this.restTemplate = new RestTemplate();
    }

    public GeminiDto.GenerateContentResponse generateContent(GeminiDto.GenerateContentRequest request) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + defaultModel + ":generateContent?key=" + apiKey;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GeminiDto.GenerateContentRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.postForObject(url, entity, GeminiDto.GenerateContentResponse.class);
    }
}

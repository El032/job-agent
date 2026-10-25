package com.jobagent.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.agent.dto.JobAnalysisResultDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=";

    public GeminiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(45_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public JobAnalysisResultDto analyze(String myResume, String vacancyTitle, String vacancyRequirements) {
        String url = GEMINI_URL + apiKey;

        String prompt = """
            Ты — IT-рекрутер, нанимающий Java Backend разработчиков. Оцени соответствие кандидата вакансии.
            
            Профиль кандидата:
            %s
            Целевая должность: Junior / Middle Java Backend Developer (серверная разработка, REST API, микросервисы).
            
            Вакансия:
            Название: %s
            Требования: %s
            
            СТРОГИЕ ПРАВИЛА:
1. Вакансия ОБЯЗАНА быть связана с написанием серверного кода или API на Java.

            2. Если вакансия относится к QA/тестированию, Android, фронтенду, системному администрированию или поддержке — выставляй score 0%%.
            3. Если это чистый Java Backend (Spring, БД, REST, сервисы) — оценивай честно от 0 до 100 на основе стека кандидата.
            
            Ответь СТРОГО в формате JSON:
            {
              "score": (число от 0 до 100),
              "matchedSkills": ["совпавший навык 1", "совпавший навык 2"],
              "missingSkills": ["чего не хватает кандидату для бэкенда"],
              "coverLetter": "Лаконичное сопроводительное письмо на 3 предложения с фокусом на бэкенд разработку и создание API."
            }
            """.formatted(myResume, vacancyTitle, vacancyRequirements);

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                },
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("Отправка запроса в gemini-3.5-flash-lite...");
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String textJson = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            return objectMapper.readValue(textJson, JobAnalysisResultDto.class);

        } catch (HttpClientErrorException.TooManyRequests e) {
            System.out.println("⚠️ Сработал лимит 429 (Rate Limit). Ждем 30 секунд...");
            try {
                Thread.sleep(30_000);
            } catch (InterruptedException ignored) {}
            return null;
        } catch (HttpServerErrorException.ServiceUnavailable e) {
            System.out.println("⚠️ Сервер Gemini временно перегружен (503).");
            return null;
        } catch (Exception e) {
            System.err.println("Ошибка вызова Gemini: " + e.getMessage());
            return null;
        }
    }
}
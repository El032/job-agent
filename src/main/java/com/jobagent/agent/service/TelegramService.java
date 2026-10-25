package com.jobagent.agent.service;

import com.jobagent.agent.dto.JobAnalysisResultDto;
import com.jobagent.agent.dto.VacancyDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String chatId;

    private final RestTemplate restTemplate;

    public TelegramService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public void sendVacancyAlert(VacancyDto vacancy, JobAnalysisResultDto analysis) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        String matchedText = analysis.getMatchedSkills() != null && !analysis.getMatchedSkills().isEmpty()
                ? String.join(", ", analysis.getMatchedSkills())
                : "Не указаны";

        String missingText = analysis.getMissingSkills() != null && !analysis.getMissingSkills().isEmpty()
                ? String.join(", ", analysis.getMissingSkills())
                : "Всё совпало";

        String coverLetterText = analysis.getCoverLetter() != null
                ? analysis.getCoverLetter()
                : "Отсутствует";

        String messageText = """
                🎯 <b>Найдена подходящая вакансия!</b>
                
                📌 <b>Должность:</b> %s
                📊 <b>Совпадение:</b> %d%%
                
                ✅ <b>Совпавшие навыки:</b> %s
                ⚠️ <b>Чего не хватает:</b> %s
                
                ✉️ <b>Сопроводительное письмо:</b>
                <i>%s</i>
                """.formatted(
                escapeHtml(vacancy.getName()),
                analysis.getScore(),
                escapeHtml(matchedText),
                escapeHtml(missingText),
                escapeHtml(coverLetterText)
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", messageText);
        payload.put("parse_mode", "HTML");
        payload.put("disable_web_page_preview", false);

        String vacancyUrl = vacancy.getAlternateUrl();
        if (vacancyUrl != null && !vacancyUrl.isBlank()) {
            Map<String, Object> inlineButton = Map.of(
                    "text", "👉 Откликнуться / Открыть вакансию",
                    "url", vacancyUrl
            );
            Map<String, Object> replyMarkup = Map.of(
                    "inline_keyboard", List.of(
                            List.of(inlineButton)
                    )
            );
            payload.put("reply_markup", replyMarkup);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("📱 Уведомление с кнопкой отправлено в Telegram!");
        } catch (Exception e) {
            System.err.println("Ошибка отправки в Telegram: " + e.getMessage());
        }
    }

    public void sendSummary(int totalFetched, int nonBackendFiltered, int analyzedByAi, int matched) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        String text = """
                ☕ <b>Сводка по Java Backend вакансиям</b>
                
                📥 Получено с ленты: <b>%d</b>
                ⏩ Отсеяно (не Java/Backend/QA/Senior): <b>%d</b>
                🤖 Проанализировано нейросетью: <b>%d</b>
                🎯 Отобрано для отклика: <b>%d</b>
                
                %s
                """.formatted(
                totalFetched,
                nonBackendFiltered,
                analyzedByAi,
                matched,
                matched > 0
                        ? "🚀 Карточки подходящих вакансий отправлены выше!"
                        : "💤 Новых подходящих совпадений нет."
        );

        Map<String, Object> payload = Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("📊 Сводка отправлена в Telegram.");
        } catch (Exception e) {
            System.err.println("Ошибка отправки сводки в Telegram: " + e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
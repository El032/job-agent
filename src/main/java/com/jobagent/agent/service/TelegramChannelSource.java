package com.jobagent.agent.service;

import com.jobagent.agent.dto.VacancyDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramChannelSource implements VacancySource {

    // Список публичных каналов, где публикуют Java-вакансии
    private static final List<String> CHANNELS = List.of(
            "jvmjobs"
    );

    @Override
    public String getSourceName() {
        return "Telegram Каналы";
    }

    @Override
    public List<VacancyDto> fetchVacancies() {
        List<VacancyDto> vacancies = new ArrayList<>();

        for (String channel : CHANNELS) {
            String url = "https://t.me/s/" + channel;
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                        .timeout(10_000)
                        .get();

                // Ищем блоки сообщений по разным вариациям разметки Telegram Web
                Elements messageBlocks = doc.select(".tgme_widget_message_wrap, .tgme_widget_message");

                int channelCount = 0;
                for (Element block : messageBlocks) {
                    Element textElement = block.selectFirst(".tgme_widget_message_text, .js-message_text");
                    if (textElement == null) {
                        continue;
                    }

                    String fullText = textElement.text();
                    if (fullText.isBlank() || fullText.length() < 30) {
                        continue;
                    }

                    // Ищем уникальный ID поста
                    Element postElement = block.hasAttr("data-post") ? block : block.selectFirst("[data-post]");
                    String postKey = postElement != null ? postElement.attr("data-post") : "";

                    if (postKey.isBlank()) {
                        // Запасной вариант: берем ссылку из даты публикации
                        Element dateLink = block.selectFirst("a.tgme_widget_message_date");
                        if (dateLink != null) {
                            String href = dateLink.attr("href");
                            postKey = href.replace("https://t.me/", "");
                        }
                    }

                    if (postKey.isBlank()) {
                        continue;
                    }

                    String postLink = "https://t.me/" + postKey;

                    // Ищем первую непустую строку для названия вакансии
                    String title = "Java Вакансия";
                    String[] lines = fullText.split("\\R");
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (!trimmed.isBlank()) {
                            title = trimmed;
                            break;
                        }
                    }

                    if (title.length() > 80) {
                        title = title.substring(0, 80) + "...";
                    }

                    VacancyDto vacancy = new VacancyDto();
                    vacancy.setId("tg_" + postKey.replace("/", "_"));
                    vacancy.setName("[" + channel + "] " + title);
                    vacancy.setAlternateUrl(postLink);

                    VacancyDto.Snippet snippet = new VacancyDto.Snippet();
                    snippet.setRequirement(fullText);
                    vacancy.setSnippet(snippet);

                    vacancies.add(vacancy);
                    channelCount++;
                }

                System.out.printf("📥 [@%s] Найдено постов: %d%n", channel, channelCount);

            } catch (Exception e) {
                System.err.printf("Не удалось прочитать канал @%s: %s%n", channel, e.getMessage());
            }
        }

        System.out.printf("📥 [%s] Всего собрано постов: %d%n", getSourceName(), vacancies.size());
        return vacancies;
    }
}
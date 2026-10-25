package com.jobagent.agent.service;

import com.jobagent.agent.dto.VacancyDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HabrCareerService implements VacancySource {

    private static final String HABR_RSS_URL = "https://career.habr.com/vacancies/rss?q=Java";

    @Override
    public String getSourceName() {
        return "Хабр Карьера";
    }

    @Override
    public List<VacancyDto> fetchVacancies() {
        List<VacancyDto> vacancies = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(HABR_RSS_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15_000)
                    .parser(Parser.xmlParser())
                    .get();

            Elements items = doc.select("item");

            for (Element item : items) {
                String title = item.select("title").text();
                String link = item.select("link").text();
                String guid = item.select("guid").text();
                String descriptionHtml = item.select("description").text();

                if (title.isBlank() || link.isBlank()) {
                    continue;
                }

                String cleanDescription = Jsoup.parse(descriptionHtml).text();
                String vacancyId = "habr_" + (guid.isBlank() ? link : guid);

                VacancyDto vacancy = new VacancyDto();
                vacancy.setId(vacancyId);
                vacancy.setName(title);
                vacancy.setAlternateUrl(link);

                VacancyDto.Snippet snippet = new VacancyDto.Snippet();
                snippet.setRequirement(cleanDescription);
                vacancy.setSnippet(snippet);

                vacancies.add(vacancy);
            }

            System.out.printf("📥 [%s] Загружено вакансий: %d%n", getSourceName(), vacancies.size());

        } catch (Exception e) {
            System.err.println("Ошибка при получении вакансий с Хабр Карьеры: " + e.getMessage());
        }

        return vacancies;
    }
}
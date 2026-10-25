package com.jobagent.agent.service;

import com.jobagent.agent.dto.JobAnalysisResultDto;
import com.jobagent.agent.dto.VacancyDto;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AgentService {

    private final List<VacancySource> vacancySources;
    private final GeminiService geminiService;
    private final ReportService reportService;
    private final TelegramService telegramService;

    // Файл хранения просмотренных ID
    private static final String PROCESSED_IDS_FILE = "processed_ids.txt";
    private final Set<String> processedVacancyIds = new HashSet<>();

    private static final int MIN_MATCH_SCORE = 70;
    private static final String MY_PROFILE = """
            Java 21, Java Backend Developer, Spring Boot 3, Spring Security, Spring Data JPA, Hibernate,
            PostgreSQL, Flyway, Apache Kafka, Transactional Outbox Pattern, Idempotency,
            Redis, Docker, Testcontainers, JUnit 5, Mockito, CI/CD, Prometheus, Grafana,
            REST API, JWT, Информационная безопасность
            """;
    private static final List<String> FORBIDDEN_WORDS = List.of(
            "qa", "тестировщик", "тестированию", "aqa", "autotests", "автотестирован", "sdet", "quality", "test",
            "android", "ios", "mobile", "frontend", "фронтенд", "react", "angular", "vue", "javascript", "typescript",
            "devops", "sre", "data engineer", "дата", "аналитик", "analyst", "dba", "администратор", "sysadmin",
            "c++", "c#", ".net", "python", "golang", "php", "1c", "1с", "ruby", "rust", "scala",
            "senior", "lead", "architect", "руководитель", "тимлид", "архитектор", "главный", "ведущий", "tech lead"
    );

    public AgentService(List<VacancySource> vacancySources,
                        GeminiService geminiService,
                        ReportService reportService,
                        TelegramService telegramService) {
        this.vacancySources = vacancySources;
        this.geminiService = geminiService;
        this.reportService = reportService;
        this.telegramService = telegramService;
    }

    // При запуске приложения читаем уже обработанные ID с диска
    @PostConstruct
    public void initProcessedIds() {
        Path path = Paths.get(PROCESSED_IDS_FILE);
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                processedVacancyIds.addAll(lines);
                System.out.printf("💾 Восстановлено %d ранее обработанных вакансий из файла %s%n",
                        processedVacancyIds.size(), PROCESSED_IDS_FILE);
            } catch (IOException e) {
                System.err.println("Не удалось прочитать файл истории вакансий: " + e.getMessage());
            }
        }
    }

    // Запуск через 2 секунды после старта приложения.
    // Большой fixedDelay гарантирует, что повторного прогона во время сессии не будет
    @Scheduled(initialDelay = 2000, fixedDelay = Long.MAX_VALUE)
    public void runScheduledScan() {
        System.out.println("\n🚀 [РУЧНОЙ СТАРТ]: Сканирование площадок под профиль Java Backend...");
        processVacancies(MY_PROFILE);
    }

    public void processVacancies(String myResume) {
        try {
            List<VacancyDto> allVacancies = new ArrayList<>();
            for (VacancySource source : vacancySources) {
                allVacancies.addAll(source.fetchVacancies());
            }

            if (allVacancies.isEmpty()) {
                System.out.println("Источники не вернули вакансий.");
                telegramService.sendSummary(0, 0, 0, 0);
                return;
            }

            int totalFetched = allVacancies.size();
            int filteredOut = 0;
            int analyzedByAi = 0;
            int matched = 0;

            for (VacancyDto vacancy : allVacancies) {
                if (vacancy.getId() == null || processedVacancyIds.contains(vacancy.getId())) {
                    continue;
                }

                // Запоминаем ID в памяти и сохраняем на диск
                markAsProcessed(vacancy.getId());

                // Проверка на null в названии
                if (vacancy.getName() == null) {
                    filteredOut++;
                    continue;
                }

                String title = vacancy.getName().toLowerCase();

                // Проверка на Java Backend
                if (!isJavaBackendTarget(title)) {
                    filteredOut++;
                    continue;
                }

                String requirements = vacancy.getSnippet() != null && vacancy.getSnippet().getRequirement() != null
                        ? vacancy.getSnippet().getRequirement()
                        : "";

                if (containsForbidden(requirements.toLowerCase())) {
                    filteredOut++;
                    continue;
                }

                if (requirements.length() > 1200) {
                    requirements = requirements.substring(0, 1200);
                }

                System.out.println("--------------------------------------------------");
                System.out.println("🔎 Анализ Backend-вакансии: " + vacancy.getName());

                analyzedByAi++;
                JobAnalysisResultDto analysis = geminiService.analyze(
                        myResume,
                        vacancy.getName(),
                        requirements
                );

                if (analysis != null) {
                    System.out.println("Оценка: " + analysis.getScore() + "%");

                    if (analysis.getScore() >= MIN_MATCH_SCORE) {
                        matched++;
                        reportService.saveApplication(vacancy, analysis);
                        telegramService.sendVacancyAlert(vacancy, analysis);
                    } else {
                        System.out.println("❌ Пропуск: балл ниже " + MIN_MATCH_SCORE + "%");
                    }
                }

                try {
                    Thread.sleep(4500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Поток анализа был прерван: " + e.getMessage());
                    break;
                }
            }

            telegramService.sendSummary(totalFetched, filteredOut, analyzedByAi, matched);

            System.out.println("--------------------------------------------------");
            System.out.printf("Готово: Всего %d, Отсеяно %d, Проверено AI %d, Отобрано %d%n",
                    totalFetched, filteredOut, analyzedByAi, matched);

        } catch (Exception e) {
            System.err.println("Ошибка обработки: " + e.getMessage());
        }
    }

    private void markAsProcessed(String id) {
        processedVacancyIds.add(id);
        try {
            Files.writeString(Paths.get(PROCESSED_IDS_FILE), id + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Ошибка записи ID в файл: " + e.getMessage());
        }
    }

    private boolean isJavaBackendTarget(String title) {
        if (!title.contains("java")) {
            return false;
        }
        for (String forbidden : FORBIDDEN_WORDS) {
            if (title.contains(forbidden)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsForbidden(String text) {
        return text.contains("тестирован") || text.contains("автотест") ||
                text.contains("android sdk") || text.contains("frontend");
    }
}
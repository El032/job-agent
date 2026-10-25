package com.jobagent.agent.service;

import com.jobagent.agent.dto.JobAnalysisResultDto;
import com.jobagent.agent.dto.VacancyDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ReportService {

    private static final String REPORT_FILE = "applications.md";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void saveApplication(VacancyDto vacancy, JobAnalysisResultDto analysis) {
        Path path = Paths.get(REPORT_FILE);

        String matched = (analysis.getMatchedSkills() != null && !analysis.getMatchedSkills().isEmpty())
                ? String.join(", ", analysis.getMatchedSkills())
                : "Не указаны";

        String missing = (analysis.getMissingSkills() != null && !analysis.getMissingSkills().isEmpty())
                ? String.join(", ", analysis.getMissingSkills())
                : "Нет";

        String coverLetter = analysis.getCoverLetter() != null
                ? analysis.getCoverLetter().replace("\n", "\n> ")
                : "Отсутствует";

        String entry = """
                ## [%s](%s)
                - **Дата анализа:** %s
                - **Совпадение:** %d%%
                - **Совпавшие навыки:** %s
                - **Недостающие навыки:** %s
                
                ### Сопроводительное письмо:
                > %s
                
                ---
                
                """.formatted(
                vacancy.getName(),
                vacancy.getAlternateUrl(),
                LocalDateTime.now().format(DATE_FORMATTER),
                analysis.getScore(),
                matched,
                missing,
                coverLetter
        );

        try {
            // Если файла еще нет — создаем с заголовком
            if (!Files.exists(path)) {
                String header = "# Отобранные вакансии для отклика\n\n";
                Files.writeString(path, header, StandardOpenOption.CREATE);
            }
            // Дописываем вакансию в конец файла
            Files.writeString(path, entry, StandardOpenOption.APPEND);
            System.out.println("💾 Сохранено в " + REPORT_FILE);
        } catch (IOException e) {
            System.err.println("Ошибка записи в отчет: " + e.getMessage());
        }
    }
}
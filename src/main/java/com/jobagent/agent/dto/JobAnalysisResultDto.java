package com.jobagent.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobAnalysisResultDto {
    private int score;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String coverLetter;
}
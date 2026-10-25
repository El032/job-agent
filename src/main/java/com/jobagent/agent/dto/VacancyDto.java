package com.jobagent.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VacancyDto {
    private String id;
    private String name;

    @JsonProperty("alternate_url")
    private String alternateUrl;

    private Snippet snippet;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snippet {
        private String requirement;
        private String responsibility;
    }
}
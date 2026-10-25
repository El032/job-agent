package com.jobagent.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HhResponseDto {
    // Поле items совпадает с ключом items в JSON
    private List<VacancyDto> items;
}
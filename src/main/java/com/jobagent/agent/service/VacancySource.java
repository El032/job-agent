package com.jobagent.agent.service;

import com.jobagent.agent.dto.VacancyDto;
import java.util.List;

public interface VacancySource {
    List<VacancyDto> fetchVacancies();
    String getSourceName();
}
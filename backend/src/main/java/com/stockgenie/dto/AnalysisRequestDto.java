package com.stockgenie.dto;

import com.stockgenie.entity.AnalysisRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequestDto {
    private String symbol;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> indicators;
    private String llmResponse;
    private AnalysisRequest.Status status;
    private String errorMessage;
}

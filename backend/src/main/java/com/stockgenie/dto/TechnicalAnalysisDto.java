package com.stockgenie.dto;

import com.stockgenie.entity.TechnicalAnalysis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalAnalysisDto {
    private String symbol;
    private LocalDate date;
    private TechnicalAnalysis.IndicatorType indicatorType;
    private Integer period;
    private BigDecimal value;
    private BigDecimal signal;
    private BigDecimal histogram;
    private String metadata;
}

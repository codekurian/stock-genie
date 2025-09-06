package com.stockgenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponseDto {
    private String response;
    private String model;
    private LocalDateTime timestamp;
    private Long processingTimeMs;
    private String analysisType;
    private String symbol;
}

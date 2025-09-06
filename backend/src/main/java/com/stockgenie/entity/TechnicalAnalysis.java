package com.stockgenie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "technical_analysis",
       uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "date", "indicator_type", "period"}),
       indexes = {
           @Index(name = "idx_symbol_date_indicator", columnList = "symbol, date, indicator_type"),
           @Index(name = "idx_symbol_indicator", columnList = "symbol, indicator_type")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10)
    private String symbol;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false)
    private IndicatorType indicatorType;
    
    @Column(nullable = false)
    private Integer period;
    
    @Column(name = "indicator_value", precision = 10, scale = 6)
    private BigDecimal value;
    
    @Column(precision = 10, scale = 6)
    private BigDecimal signal;
    
    @Column(precision = 10, scale = 6)
    private BigDecimal histogram;
    
    @Column(length = 1000)
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum IndicatorType {
        SMA,    // Simple Moving Average
        EMA,    // Exponential Moving Average
        RSI,    // Relative Strength Index
        MACD,   // Moving Average Convergence Divergence
        BOLLINGER_BANDS,
        STOCHASTIC,
        WILLIAMS_R
    }
}

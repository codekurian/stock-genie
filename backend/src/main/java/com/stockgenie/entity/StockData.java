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
@Table(name = "stock_data", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "date"}),
       indexes = {
           @Index(name = "idx_symbol_date", columnList = "symbol, date"),
           @Index(name = "idx_symbol", columnList = "symbol"),
           @Index(name = "idx_date", columnList = "date")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10)
    private String symbol;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal open;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal high;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal low;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal close;
    
    @Column(nullable = false)
    private Long volume;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal adjustedClose;
    
    @Column(length = 50)
    private String dataSource;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

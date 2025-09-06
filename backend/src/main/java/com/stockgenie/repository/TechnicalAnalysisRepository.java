package com.stockgenie.repository;

import com.stockgenie.entity.TechnicalAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicalAnalysisRepository extends JpaRepository<TechnicalAnalysis, Long> {
    
    List<TechnicalAnalysis> findBySymbolAndIndicatorTypeOrderByDateAsc(String symbol, TechnicalAnalysis.IndicatorType indicatorType);
    
    List<TechnicalAnalysis> findBySymbolAndIndicatorTypeAndDateBetweenOrderByDateAsc(
            String symbol, TechnicalAnalysis.IndicatorType indicatorType, LocalDate startDate, LocalDate endDate);
    
    Optional<TechnicalAnalysis> findBySymbolAndDateAndIndicatorTypeAndPeriod(
            String symbol, LocalDate date, TechnicalAnalysis.IndicatorType indicatorType, Integer period);
    
    @Query("SELECT t FROM TechnicalAnalysis t WHERE t.symbol = :symbol AND t.indicatorType = :indicatorType AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date ASC")
    List<TechnicalAnalysis> findTechnicalAnalysisInRange(@Param("symbol") String symbol, 
                                                        @Param("indicatorType") TechnicalAnalysis.IndicatorType indicatorType,
                                                        @Param("startDate") LocalDate startDate, 
                                                        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(t) FROM TechnicalAnalysis t WHERE t.symbol = :symbol AND t.indicatorType = :indicatorType AND t.date >= :startDate AND t.date <= :endDate")
    long countBySymbolAndIndicatorTypeAndDateRange(@Param("symbol") String symbol, 
                                                  @Param("indicatorType") TechnicalAnalysis.IndicatorType indicatorType,
                                                  @Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);
    
    boolean existsBySymbolAndIndicatorTypeAndDateBetween(String symbol, TechnicalAnalysis.IndicatorType indicatorType, LocalDate startDate, LocalDate endDate);
}

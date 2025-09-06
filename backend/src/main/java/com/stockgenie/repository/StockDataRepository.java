package com.stockgenie.repository;

import com.stockgenie.entity.StockData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, Long> {
    
    List<StockData> findBySymbolOrderByDateAsc(String symbol);
    
    List<StockData> findBySymbolAndDateBetweenOrderByDateAsc(String symbol, LocalDate startDate, LocalDate endDate);
    
    Optional<StockData> findBySymbolAndDate(String symbol, LocalDate date);
    
    @Query("SELECT s FROM StockData s WHERE s.symbol = :symbol AND s.date >= :startDate AND s.date <= :endDate ORDER BY s.date ASC")
    List<StockData> findStockDataInRange(@Param("symbol") String symbol, 
                                        @Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(s) FROM StockData s WHERE s.symbol = :symbol AND s.date >= :startDate AND s.date <= :endDate")
    long countBySymbolAndDateRange(@Param("symbol") String symbol, 
                                  @Param("startDate") LocalDate startDate, 
                                  @Param("endDate") LocalDate endDate);
    
    @Query("SELECT MAX(s.date) FROM StockData s WHERE s.symbol = :symbol")
    Optional<LocalDate> findLatestDateBySymbol(@Param("symbol") String symbol);
    
    @Query("SELECT MIN(s.date) FROM StockData s WHERE s.symbol = :symbol")
    Optional<LocalDate> findEarliestDateBySymbol(@Param("symbol") String symbol);
    
    boolean existsBySymbolAndDateBetween(String symbol, LocalDate startDate, LocalDate endDate);
}

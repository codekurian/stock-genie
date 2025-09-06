package com.stockgenie.repository;

import com.stockgenie.entity.AnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, Long> {
    
    List<AnalysisRequest> findBySymbolOrderByCreatedAtDesc(String symbol);
    
    List<AnalysisRequest> findByStatusOrderByCreatedAtDesc(AnalysisRequest.Status status);
    
    Optional<AnalysisRequest> findByIdAndStatus(Long id, AnalysisRequest.Status status);
    
    @Query("SELECT a FROM AnalysisRequest a WHERE a.symbol = :symbol AND a.status = :status ORDER BY a.createdAt DESC")
    List<AnalysisRequest> findBySymbolAndStatusOrderByCreatedAtDesc(@Param("symbol") String symbol, 
                                                                   @Param("status") AnalysisRequest.Status status);
    
    @Query("SELECT COUNT(a) FROM AnalysisRequest a WHERE a.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);
}

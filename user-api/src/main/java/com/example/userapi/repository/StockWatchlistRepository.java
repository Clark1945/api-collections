package com.example.userapi.repository;

import com.example.userapi.entity.StockWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockWatchlistRepository extends JpaRepository<StockWatchlist, Long> {
    List<StockWatchlist> findByUserId(Long userId);
    Optional<StockWatchlist> findByUserIdAndStockSymbol(Long userId, String stockSymbol);
    boolean existsByUserIdAndStockSymbol(Long userId, String stockSymbol);
    void deleteByUserIdAndStockSymbol(Long userId, String stockSymbol);
}

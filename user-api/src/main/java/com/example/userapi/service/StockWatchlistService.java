package com.example.userapi.service;

import com.example.userapi.dto.response.StockQuoteResponse;
import com.example.userapi.entity.StockWatchlist;
import com.example.userapi.entity.User;
import com.example.userapi.exception.DuplicateResourceException;
import com.example.userapi.exception.ResourceNotFoundException;
import com.example.userapi.repository.StockWatchlistRepository;
import com.example.userapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockWatchlistService {

    private final StockWatchlistRepository stockWatchlistRepository;
    private final UserRepository userRepository;
    private final TwseApiService twseApiService;

    public StockWatchlistService(StockWatchlistRepository stockWatchlistRepository,
                                 UserRepository userRepository,
                                 TwseApiService twseApiService) {
        this.stockWatchlistRepository = stockWatchlistRepository;
        this.userRepository = userRepository;
        this.twseApiService = twseApiService;
    }

    @Transactional
    public StockWatchlist addStock(Long userId, String symbol) {
        if (stockWatchlistRepository.existsByUserIdAndStockSymbol(userId, symbol)) {
            throw new DuplicateResourceException("StockWatchlist", "stockSymbol", symbol);
        }

        // Verify symbol exists via TWSE API
        StockQuoteResponse quote = twseApiService.getStockQuote(symbol);
        if (quote == null || quote.getStockName() == null) {
            throw new ResourceNotFoundException("Stock", "symbol", symbol);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        StockWatchlist watchlist = new StockWatchlist();
        watchlist.setUser(user);
        watchlist.setStockSymbol(symbol);
        watchlist.setStockName(quote.getStockName());
        return stockWatchlistRepository.save(watchlist);
    }

    @Transactional
    public void removeStock(Long userId, String symbol) {
        if (!stockWatchlistRepository.existsByUserIdAndStockSymbol(userId, symbol)) {
            throw new ResourceNotFoundException("StockWatchlist", "stockSymbol", symbol);
        }
        stockWatchlistRepository.deleteByUserIdAndStockSymbol(userId, symbol);
    }

    @Transactional(readOnly = true)
    public List<StockWatchlist> getWatchlist(Long userId) {
        return stockWatchlistRepository.findByUserId(userId);
    }
}

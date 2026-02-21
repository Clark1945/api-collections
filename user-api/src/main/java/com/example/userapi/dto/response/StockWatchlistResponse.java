package com.example.userapi.dto.response;

import com.example.userapi.entity.StockWatchlist;
import java.time.Instant;

public class StockWatchlistResponse {

    private Long id;
    private String stockSymbol;
    private String stockName;
    private Instant createdAt;

    public static StockWatchlistResponse from(StockWatchlist watchlist) {
        StockWatchlistResponse response = new StockWatchlistResponse();
        response.setId(watchlist.getId());
        response.setStockSymbol(watchlist.getStockSymbol());
        response.setStockName(watchlist.getStockName());
        response.setCreatedAt(watchlist.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

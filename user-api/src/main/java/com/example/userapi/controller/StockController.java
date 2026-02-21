package com.example.userapi.controller;

import com.example.userapi.dto.request.AddStockRequest;
import com.example.userapi.dto.response.StockQuoteResponse;
import com.example.userapi.dto.response.StockWatchlistResponse;
import com.example.userapi.entity.StockWatchlist;
import com.example.userapi.entity.User;
import com.example.userapi.service.StockSseService;
import com.example.userapi.service.StockWatchlistService;
import com.example.userapi.service.TwseApiService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockWatchlistService stockWatchlistService;
    private final TwseApiService twseApiService;
    private final StockSseService stockSseService;

    public StockController(StockWatchlistService stockWatchlistService,
                           TwseApiService twseApiService,
                           StockSseService stockSseService) {
        this.stockWatchlistService = stockWatchlistService;
        this.twseApiService = twseApiService;
        this.stockSseService = stockSseService;
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<StockWatchlistResponse>> getWatchlist(HttpSession session) {
        User user = (User) session.getAttribute("user");
        List<StockWatchlistResponse> watchlist = stockWatchlistService.getWatchlist(user.getId())
                .stream()
                .map(StockWatchlistResponse::from)
                .toList();
        return ResponseEntity.ok(watchlist);
    }

    @PostMapping("/watchlist")
    public ResponseEntity<StockWatchlistResponse> addStock(@Valid @RequestBody AddStockRequest request,
                                                           HttpSession session) {
        User user = (User) session.getAttribute("user");
        StockWatchlist watchlist = stockWatchlistService.addStock(user.getId(), request.getStockSymbol());
        return ResponseEntity.ok(StockWatchlistResponse.from(watchlist));
    }

    @DeleteMapping("/watchlist/{symbol}")
    public ResponseEntity<Void> removeStock(@PathVariable String symbol, HttpSession session) {
        User user = (User) session.getAttribute("user");
        stockWatchlistService.removeStock(user.getId(), symbol);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/quote/{symbol}")
    public ResponseEntity<StockQuoteResponse> getQuote(@PathVariable String symbol) {
        StockQuoteResponse quote = twseApiService.getStockQuote(symbol);
        if (quote == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(quote);
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return stockSseService.subscribe(user.getId());
    }
}

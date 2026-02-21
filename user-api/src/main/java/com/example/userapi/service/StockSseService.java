package com.example.userapi.service;

import com.example.userapi.dto.response.StockQuoteResponse;
import com.example.userapi.entity.StockWatchlist;
import com.example.userapi.repository.StockWatchlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StockSseService {

    private static final Logger log = LoggerFactory.getLogger(StockSseService.class);

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final StockWatchlistRepository stockWatchlistRepository;
    private final TwseApiService twseApiService;

    @Value("${stock.sse.emitter-timeout-ms:300000}")
    private long emitterTimeoutMs;

    public StockSseService(StockWatchlistRepository stockWatchlistRepository, TwseApiService twseApiService) {
        this.stockWatchlistRepository = stockWatchlistRepository;
        this.twseApiService = twseApiService;
    }

    public SseEmitter subscribe(Long userId) {
        // Remove existing emitter if present
        SseEmitter existing = emitters.remove(userId);
        if (existing != null) {
            existing.complete();
        }

        SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));
        emitters.put(userId, emitter);
        return emitter;
    }

    public void unsubscribe(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    @Scheduled(fixedRateString = "${stock.sse.push-interval-ms:15000}")
    public void pushStockUpdates() {
        if (emitters.isEmpty()) {
            return;
        }

        // Collect all user IDs and their watchlists
        Map<Long, List<String>> userSymbolsMap = new HashMap<>();
        Set<String> allSymbols = new HashSet<>();

        for (Long userId : emitters.keySet()) {
            List<StockWatchlist> watchlist = stockWatchlistRepository.findByUserId(userId);
            List<String> symbols = watchlist.stream()
                    .map(StockWatchlist::getStockSymbol)
                    .collect(Collectors.toList());
            if (!symbols.isEmpty()) {
                userSymbolsMap.put(userId, symbols);
                allSymbols.addAll(symbols);
            }
        }

        if (allSymbols.isEmpty()) {
            return;
        }

        // Batch fetch all quotes
        List<StockQuoteResponse> allQuotes = twseApiService.getStockQuotes(new ArrayList<>(allSymbols));
        Map<String, StockQuoteResponse> quoteMap = allQuotes.stream()
                .collect(Collectors.toMap(StockQuoteResponse::getStockSymbol, q -> q, (a, b) -> a));

        // Push to each user
        List<Long> deadEmitters = new ArrayList<>();
        for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            List<String> userSymbols = userSymbolsMap.get(userId);
            if (userSymbols == null || userSymbols.isEmpty()) {
                continue;
            }

            List<StockQuoteResponse> userQuotes = userSymbols.stream()
                    .map(quoteMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            try {
                emitter.send(SseEmitter.event()
                        .name("stock-update")
                        .data(userQuotes));
            } catch (IOException e) {
                deadEmitters.add(userId);
            }
        }

        // Clean up dead emitters
        for (Long userId : deadEmitters) {
            SseEmitter emitter = emitters.remove(userId);
            if (emitter != null) {
                emitter.completeWithError(new RuntimeException("Send failed"));
            }
        }
    }
}

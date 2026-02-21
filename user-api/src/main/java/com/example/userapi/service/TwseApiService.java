package com.example.userapi.service;

import com.example.userapi.config.TwseApiProperties;
import com.example.userapi.dto.response.StockQuoteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TwseApiService {

    private static final Logger log = LoggerFactory.getLogger(TwseApiService.class);

    private final RestTemplate restTemplate;
    private final TwseApiProperties properties;
    private final ObjectMapper objectMapper;

    public TwseApiService(RestTemplate restTemplate, TwseApiProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<StockQuoteResponse> getStockQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        String exCh = symbols.stream()
                .map(s -> "tse_" + s + ".tw")
                .collect(Collectors.joining("|"));

        String url = properties.getBaseUrl() + "/getStockInfo.jsp?ex_ch=" + exCh;

        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            if (responseBody == null) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode msgArray = root.get("msgArray");
            if (msgArray == null || !msgArray.isArray()) {
                return Collections.emptyList();
            }

            List<StockQuoteResponse> quotes = new ArrayList<>();
            for (JsonNode node : msgArray) {
                StockQuoteResponse quote = parseQuote(node);
                if (quote != null) {
                    quotes.add(quote);
                }
            }
            return quotes;
        } catch (Exception e) {
            log.error("Failed to fetch stock quotes from TWSE API: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public StockQuoteResponse getStockQuote(String symbol) {
        List<StockQuoteResponse> quotes = getStockQuotes(List.of(symbol));
        return quotes.isEmpty() ? null : quotes.get(0);
    }

    private StockQuoteResponse parseQuote(JsonNode node) {
        try {
            String currentPrice = getTextValue(node, "z");
            String previousClose = getTextValue(node, "y");

            // z="-" means no trade yet
            if ("-".equals(currentPrice)) {
                currentPrice = previousClose;
            }

            StockQuoteResponse quote = new StockQuoteResponse();
            quote.setStockSymbol(getTextValue(node, "c"));
            quote.setStockName(getTextValue(node, "n"));
            quote.setCurrentPrice(currentPrice);
            quote.setOpenPrice(getTextValue(node, "o"));
            quote.setHighPrice(getTextValue(node, "h"));
            quote.setLowPrice(getTextValue(node, "l"));
            quote.setVolume(getTextValue(node, "v"));
            quote.setPreviousClose(previousClose);
            quote.setTimestamp(getTextValue(node, "t"));

            // Calculate change
            try {
                if (currentPrice != null && previousClose != null
                        && !"-".equals(currentPrice) && !"-".equals(previousClose)) {
                    double current = Double.parseDouble(currentPrice);
                    double prev = Double.parseDouble(previousClose);
                    double change = current - prev;
                    double changePercent = (prev != 0) ? (change / prev * 100) : 0;
                    quote.setChangePrice(String.format("%.2f", change));
                    quote.setChangePercent(String.format("%.2f", changePercent));
                } else {
                    quote.setChangePrice("0.00");
                    quote.setChangePercent("0.00");
                }
            } catch (NumberFormatException e) {
                quote.setChangePrice("0.00");
                quote.setChangePercent("0.00");
            }

            return quote;
        } catch (Exception e) {
            log.warn("Failed to parse stock quote: {}", e.getMessage());
            return null;
        }
    }

    private String getTextValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText() : null;
    }
}

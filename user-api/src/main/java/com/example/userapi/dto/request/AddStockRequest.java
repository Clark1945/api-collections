package com.example.userapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AddStockRequest {

    @NotBlank(message = "股票代號不可為空")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "股票代號格式不正確（需為 4-6 位數字）")
    private String stockSymbol;

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }
}

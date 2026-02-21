package com.example.userapi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class PaymentRequest {

    @NotNull(message = "金額不可為空")
    @DecimalMin(value = "0.01", message = "付款金額至少為 0.01")
    private BigDecimal amount;

    @Size(max = 255, message = "描述不可超過 255 字")
    private String description;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

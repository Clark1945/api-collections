package com.example.userapi.dto.response;

import com.example.userapi.entity.Wallet;
import java.math.BigDecimal;
import java.time.Instant;

public class WalletResponse {

    private Long id;
    private BigDecimal balance;
    private Instant updatedAt;

    public static WalletResponse from(Wallet wallet) {
        WalletResponse response = new WalletResponse();
        response.setId(wallet.getId());
        response.setBalance(wallet.getBalance());
        response.setUpdatedAt(wallet.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

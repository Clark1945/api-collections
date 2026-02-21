package com.example.userapi.controller;

import com.example.userapi.dto.request.PaymentRequest;
import com.example.userapi.dto.request.TopUpRequest;
import com.example.userapi.dto.response.WalletResponse;
import com.example.userapi.dto.response.WalletTransactionResponse;
import com.example.userapi.entity.User;
import com.example.userapi.entity.Wallet;
import com.example.userapi.service.WalletService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    @PostMapping("/top-up")
    public ResponseEntity<WalletResponse> topUp(@Valid @RequestBody TopUpRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        Wallet wallet = walletService.topUp(user.getId(), request.getAmount());
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    @PostMapping("/payment")
    public ResponseEntity<WalletResponse> payment(@Valid @RequestBody PaymentRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        Wallet wallet = walletService.payment(user.getId(), request.getAmount(), request.getDescription());
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionResponse> transactions = walletService
                .getTransactionHistory(user.getId(), pageable)
                .map(WalletTransactionResponse::from);
        return ResponseEntity.ok(transactions);
    }
}

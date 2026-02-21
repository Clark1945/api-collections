package com.example.userapi.service;

import com.example.userapi.entity.User;
import com.example.userapi.entity.Wallet;
import com.example.userapi.entity.WalletTransaction;
import com.example.userapi.enums.TransactionType;
import com.example.userapi.exception.InsufficientBalanceException;
import com.example.userapi.exception.ResourceNotFoundException;
import com.example.userapi.repository.WalletRepository;
import com.example.userapi.repository.WalletTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    public Wallet createWalletForUser(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId));
    }

    @Transactional
    public Wallet topUp(Long userId, BigDecimal amount) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet = walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.TOP_UP);
        tx.setAmount(amount);
        tx.setBalanceAfter(wallet.getBalance());
        tx.setDescription("儲值 " + amount + " 元");
        walletTransactionRepository.save(tx);

        return wallet;
    }

    @Transactional
    public Wallet payment(Long userId, BigDecimal amount, String description) {
        Wallet wallet = getWalletByUserId(userId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("餘額不足，目前餘額: " + wallet.getBalance() + "，需要: " + amount);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet = walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.PAYMENT);
        tx.setAmount(amount);
        tx.setBalanceAfter(wallet.getBalance());
        tx.setDescription(description != null ? description : "付款 " + amount + " 元");
        walletTransactionRepository.save(tx);

        return wallet;
    }

    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactionHistory(Long userId, Pageable pageable) {
        return walletTransactionRepository.findByWalletUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}

package com.example.userapi.repository;

import com.example.userapi.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Page<WalletTransaction> findByWalletUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

package com.example.userapi.repository;

import com.example.userapi.entity.User;
import com.example.userapi.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByStatusNot(UserStatus status, Pageable pageable);

    Optional<User> findByIdAndStatusNot(Long id, UserStatus status);
}

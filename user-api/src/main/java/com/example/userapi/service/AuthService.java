package com.example.userapi.service;

import com.example.userapi.entity.User;
import com.example.userapi.enums.AuditEventType;
import com.example.userapi.exception.AuthenticationException;
import com.example.userapi.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public User login(String username, String password, String ipAddress) {
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            throw new AuthenticationException("帳號或密碼錯誤");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            auditLogService.log(user, AuditEventType.LOGIN_FAILED, "IP: " + ipAddress);
            throw new AuthenticationException("帳號或密碼錯誤");
        }

        if (!user.getStatus().canLogin()) {
            auditLogService.log(user, AuditEventType.LOGIN_FAILED,
                    "帳號狀態不允許登入: " + user.getStatus().getDescription() + ", IP: " + ipAddress);
            throw new AuthenticationException("帳號狀態為「" + user.getStatus().getDescription() + "」，無法登入");
        }

        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        auditLogService.log(user, AuditEventType.LOGIN, "IP: " + ipAddress);

        return user;
    }
}

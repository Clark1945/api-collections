package com.example.userapi.service;

import com.example.userapi.entity.User;
import com.example.userapi.enums.AuditEventType;
import com.example.userapi.exception.AuthenticationException;
import com.example.userapi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository, AuditLogService auditLogService,
                       PasswordEncoder passwordEncoder, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public User login(String username, String password, String ipAddress) {
        // 1. 鎖定檢查：優先於 DB 查詢，鎖定時直接拋出例外
        loginAttemptService.checkLocked(username, ipAddress);

        // 2. 查找帳號
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            // 帳號不存在也計入失敗（防止 IP 暴力枚舉帳號）
            loginAttemptService.recordFailure(username, ipAddress);
            throw new AuthenticationException("帳號或密碼錯誤");
        }

        // 3. 驗證密碼
        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginAttemptService.recordFailure(username, ipAddress);
            auditLogService.log(user, AuditEventType.LOGIN_FAILED, "IP: " + ipAddress);
            throw new AuthenticationException("帳號或密碼錯誤");
        }

        // 4. 帳號狀態檢查（管理員停用，與暴力破解鎖定分開）
        if (!user.getStatus().canLogin()) {
            auditLogService.log(user, AuditEventType.LOGIN_FAILED,
                    "帳號狀態不允許登入: " + user.getStatus().getDescription() + ", IP: " + ipAddress);
            throw new AuthenticationException("帳號狀態為「" + user.getStatus().getDescription() + "」，無法登入");
        }

        // 5. 登入成功：清除失敗計數
        loginAttemptService.recordSuccess(username);

        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        auditLogService.log(user, AuditEventType.LOGIN, "IP: " + ipAddress);

        return user;
    }
}

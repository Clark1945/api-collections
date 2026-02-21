package com.example.userapi.service;

import com.example.userapi.exception.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_USER_ATTEMPTS = 5;
    private static final int MAX_IP_ATTEMPTS = 20;
    private static final Duration USER_LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration IP_LOCK_DURATION = Duration.ofHours(1);

    private static final String USER_KEY_PREFIX = "login:fail:user:";
    private static final String IP_KEY_PREFIX = "login:fail:ip:";

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 登入前檢查：IP 或帳號是否已被鎖定。
     * 鎖定時直接拋出 AuthenticationException。
     * Redis 不可用時 fail-open：跳過檢查，允許登入繼續。
     */
    public void checkLocked(String username, String ip) {
        try {
            if (isIpLocked(ip)) {
                throw new AuthenticationException("此 IP 已被暫時鎖定，請 1 小時後再試");
            }
            if (isUserLocked(username)) {
                long minutes = getUserLockTtlSeconds(username) / 60 + 1;
                throw new AuthenticationException("帳號已被暫時鎖定，請約 " + minutes + " 分鐘後再試");
            }
        } catch (AuthenticationException e) {
            // 正常鎖定邏輯，直接往上拋
            throw e;
        } catch (Exception e) {
            // Redis 不可用：fail-open，記錄警告但不阻擋登入
            log.warn("[LoginAttempt] Redis unavailable during checkLocked, bypassing rate limit. username={}", username, e);
        }
    }

    /**
     * 登入失敗：同時計入 username 與 IP 的失敗次數。
     * Redis 不可用時靜默跳過。
     */
    public void recordFailure(String username, String ip) {
        try {
            increment(USER_KEY_PREFIX + username, USER_LOCK_DURATION);
            increment(IP_KEY_PREFIX + ip, IP_LOCK_DURATION);
        } catch (Exception e) {
            log.warn("[LoginAttempt] Redis unavailable during recordFailure, skipping. username={}", username, e);
        }
    }

    /**
     * 登入成功：清除該帳號的失敗計數（IP 計數保留，自然過期）。
     * Redis 不可用時靜默跳過。
     */
    public void recordSuccess(String username) {
        try {
            redisTemplate.delete(USER_KEY_PREFIX + username);
        } catch (Exception e) {
            log.warn("[LoginAttempt] Redis unavailable during recordSuccess, skipping. username={}", username, e);
        }
    }

    // ── 內部方法 ───────────────────────────────────────────────

    private boolean isUserLocked(String username) {
        return getCount(USER_KEY_PREFIX + username) >= MAX_USER_ATTEMPTS;
    }

    private boolean isIpLocked(String ip) {
        return getCount(IP_KEY_PREFIX + ip) >= MAX_IP_ATTEMPTS;
    }

    private long getUserLockTtlSeconds(String username) {
        Long ttl = redisTemplate.getExpire(USER_KEY_PREFIX + username, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? ttl : 0;
    }

    private int getCount(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? 0 : Integer.parseInt(val);
    }

    /**
     * INCR key，若是第一次寫入則設定 TTL。
     * 注意：INCR 與 EXPIRE 非同一原子操作，極端情況下 key 可能不帶 TTL，
     * 但實務上發生機率極低且不影響安全性（下次成功登入會刪除）。
     */
    private void increment(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, ttl);
        }
    }
}

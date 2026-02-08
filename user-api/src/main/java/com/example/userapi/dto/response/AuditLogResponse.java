package com.example.userapi.dto.response;

import com.example.userapi.entity.AuditLog;
import com.example.userapi.enums.AuditEventType;

import java.time.Instant;

public class AuditLogResponse {

    private Long id;
    private Long userId;
    private AuditEventType eventType;
    private String ipAddress;
    private String userAgent;
    private String details;
    private Instant createdAt;

    public static AuditLogResponse from(AuditLog auditLog) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(auditLog.getId());
        response.setUserId(auditLog.getUser().getId());
        response.setEventType(auditLog.getEventType());
        response.setIpAddress(auditLog.getIpAddress());
        response.setUserAgent(auditLog.getUserAgent());
        response.setDetails(auditLog.getDetails());
        response.setCreatedAt(auditLog.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

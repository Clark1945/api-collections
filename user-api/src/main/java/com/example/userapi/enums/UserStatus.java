package com.example.userapi.enums;

public enum UserStatus {
    ENABLED("啟用", true),
    DISABLED("停用", false),
    LOCKED("鎖定", false),
    PENDING("待審核", false);

    private final String description;
    private final boolean canLogin;

    UserStatus(String description, boolean canLogin) {
        this.description = description;
        this.canLogin = canLogin;
    }

    public boolean canLogin() {
        return canLogin;
    }

    public String getDescription() {
        return description;
    }
}

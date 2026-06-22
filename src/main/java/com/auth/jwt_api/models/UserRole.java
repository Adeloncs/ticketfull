package com.auth.jwt_api.models;

public enum UserRole {
    ADMIN("admin"),
    USER("user"),
    ORGANIZER("organizer"),
    CUSTOMER("customer");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}

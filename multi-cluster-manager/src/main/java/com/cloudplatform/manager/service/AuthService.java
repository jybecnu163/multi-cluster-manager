package com.cloudplatform.manager.service;

public interface AuthService {
    String login(String email, String password);

    void logout(String token);

    String setupTotp(Long userId);

    boolean verifyTotp(Long userId, int code);
}

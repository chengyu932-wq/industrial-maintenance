package com.cq.maintenance.security.vo;

public record LoginVO(String accessToken, String refreshToken, long expiresIn, CurrentUserVO user) {}

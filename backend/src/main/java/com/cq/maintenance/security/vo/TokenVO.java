package com.cq.maintenance.security.vo;

public record TokenVO(String accessToken, String refreshToken, long expiresIn) {}

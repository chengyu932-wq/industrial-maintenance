package com.cq.maintenance.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    PARAMETER_ERROR(40000, "参数错误", HttpStatus.BAD_REQUEST),
    BUSINESS_CONFLICT(40001, "业务规则冲突", HttpStatus.BAD_REQUEST),
    CAPTCHA_INVALID(40010, "验证码错误", HttpStatus.BAD_REQUEST),
    CAPTCHA_EXPIRED(40011, "验证码已过期，请重新获取", HttpStatus.BAD_REQUEST),
    USERNAME_OR_PASSWORD_INVALID(40012, "用户名或密码错误", HttpStatus.BAD_REQUEST),
    ACCOUNT_LOCKED(42300, "账号已临时锁定，请稍后再试", HttpStatus.LOCKED),
    ACCOUNT_DISABLED(40310, "账号已禁用", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED(40100, "未认证", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_INVALID(40101, "Access Token 失效", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_EXPIRED(40103, "Access Token 已过期", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(40102, "Refresh Token 失效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "无接口权限", HttpStatus.FORBIDDEN),
    DATA_FORBIDDEN(40301, "数据权限不足", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "数据不存在", HttpStatus.NOT_FOUND),
    ILLEGAL_STATE_TRANSITION(40901, "状态流转非法", HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK(40902, "库存不足", HttpStatus.CONFLICT),
    DUPLICATE_OPERATION(40903, "重复操作", HttpStatus.CONFLICT),
    SYSTEM_ERROR(50000, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

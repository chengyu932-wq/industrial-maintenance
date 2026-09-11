package com.cq.maintenance.common.exception;

import com.cq.maintenance.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ApiResponse.failure(errorCode, exception.getMessage()));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HandlerMethodValidationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
        String message = extractValidationMessage(exception);
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(ErrorCode.PARAMETER_ERROR, message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException() {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
            .body(ApiResponse.failure(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException() {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus())
            .body(ApiResponse.failure(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled server exception", exception);
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.getHttpStatus())
            .body(ApiResponse.failure(ErrorCode.SYSTEM_ERROR));
    }

    private String extractValidationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodException) {
            return methodException.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? ErrorCode.PARAMETER_ERROR.getMessage() : error.getDefaultMessage())
                .orElse(ErrorCode.PARAMETER_ERROR.getMessage());
        }
        if (exception instanceof BindException bindException) {
            return bindException.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? ErrorCode.PARAMETER_ERROR.getMessage() : error.getDefaultMessage())
                .orElse(ErrorCode.PARAMETER_ERROR.getMessage());
        }
        return exception.getMessage() == null ? ErrorCode.PARAMETER_ERROR.getMessage() : exception.getMessage();
    }
}

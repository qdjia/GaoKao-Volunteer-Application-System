package com.gaokao.config;

import com.gaokao.security.AccountLockedException;
import com.gaokao.security.UnauthenticatedException;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleUploadTooLarge(Exception e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Result.error(413, "Excel文件不得超过2MB"));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Result<Void>> handleUnauthenticated(UnauthenticatedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(401, e.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Result<Void>> handleLocked(AccountLockedException e) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(Result.error(423, e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Result<Void>> handleSecurity(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(403, e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Result<Void>> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Result.error(400, e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> handleInvalidRequest(Exception e) {
        return ResponseEntity.badRequest().body(Result.error(400, "请求参数格式错误"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled request failure: type={}, requestId={}",
                e.getClass().getSimpleName(), request.getAttribute("requestId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误"));
    }
}

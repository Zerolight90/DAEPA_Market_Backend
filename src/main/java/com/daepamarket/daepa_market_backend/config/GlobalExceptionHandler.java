package com.daepamarket.daepa_market_backend.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 전역 예외 핸들러
 * - 모든 API 에러를 { code, message, fields?, timestamp } 형식으로 통일
 * - 프론트엔드 Axios interceptor가 code 값으로 에러를 분기함
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 1. ResponseStatusException (가장 흔한 비즈니스 예외) ──────────────
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        log.warn("[ResponseStatusException] status={}, reason={}", e.getStatusCode(), e.getReason());

        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String code = resolveCode(status);

        return ResponseEntity
                .status(e.getStatusCode())
                .body(ErrorResponse.of(code, e.getReason() != null ? e.getReason() : e.getMessage()));
    }

    // ── 2. @Valid 실패 → 400 + 필드별 메시지 ─────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = e.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> Optional.ofNullable(fe.getDefaultMessage()).orElse(""),
                        (a, b) -> a // 동일 필드 중복 시 첫 번째
                ));

        log.warn("[Validation] fields={}", fields);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.withFields("VALIDATION", "입력값을 확인해주세요.", fields));
    }

    // ── 3. 그 외 모든 예외 → 500 (상세 내용 노출 금지) ───────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception e) {
        log.error("[CRITICAL] Unhandled exception: {}", e.getMessage(), e);
        // TODO: Sentry.captureException(e); 연동 시 여기서 호출

        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of("INTERNAL", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }

    // ── 상태 코드 → 에러 코드 매핑 ───────────────────────────────────────
    private String resolveCode(HttpStatus status) {
        if (status == null) return "UNKNOWN";
        return switch (status) {
            case UNAUTHORIZED    -> "AUTH_001";
            case FORBIDDEN       -> "FORBIDDEN";
            case NOT_FOUND       -> "NOT_FOUND";
            case BAD_REQUEST     -> "BAD_REQUEST";
            case CONFLICT        -> "CONFLICT";
            default              -> "ERROR_" + status.value();
        };
    }

    // ── ErrorResponse DTO (내부 클래스) ──────────────────────────────────
    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private final String code;
        private final String message;
        private final Map<String, String> fields;   // Validation 에러용 (null 가능)
        private final LocalDateTime timestamp;

        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(code, message, null, LocalDateTime.now());
        }

        public static ErrorResponse withFields(String code, String message, Map<String, String> fields) {
            return new ErrorResponse(code, message, fields, LocalDateTime.now());
        }
    }
}

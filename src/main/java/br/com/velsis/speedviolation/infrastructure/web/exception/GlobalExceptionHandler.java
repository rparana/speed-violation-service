package br.com.velsis.speedviolation.infrastructure.web.exception;

import br.com.velsis.speedviolation.infrastructure.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        public static final String ATTR_LICENSE_PLATE = "request.licensePlate";
        public static final String ATTR_EQUIPMENT_ID = "request.equipmentId";
        public static final String ATTR_CAPTURE_TIMESTAMP = "request.captureTimestamp";

        private final Clock clock;

        public GlobalExceptionHandler(Clock clock) {
                this.clock = Objects.requireNonNull(clock, "clock must not be null");
        }

        @ExceptionHandler(SpeedValidationException.class)
        public ResponseEntity<ApiErrorResponse> handleSpeedValidationException(
                        SpeedValidationException ex,
                        HttpServletRequest request) {
                Instant now = Instant.now(clock);
                log.warn(
                                "Validation failed: errorCode={}, licensePlate={}, equipmentId={}, captureTimestamp={}, timestamp={}, message={}",
                                ex.getErrorCode(),
                                attributeOrDefault(request, ATTR_LICENSE_PLATE),
                                attributeOrDefault(request, ATTR_EQUIPMENT_ID),
                                attributeOrDefault(request, ATTR_CAPTURE_TIMESTAMP),
                                now,
                                ex.getMessage());
                ApiErrorResponse body = new ApiErrorResponse(ex.getErrorCode(), ex.getMessage(), now);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
        public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
                        Exception ex,
                        HttpServletRequest request) {
                Instant now = Instant.now(clock);
                log.warn(
                                "Malformed request: path={}, timestamp={}, exceptionType={}",
                                request.getRequestURI(),
                                now,
                                ex.getClass().getSimpleName());
                ApiErrorResponse body = new ApiErrorResponse(
                                "MALFORMED_REQUEST",
                                "Malformed request payload or invalid parameter type",
                                now);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {
                Instant now = Instant.now(clock);
                log.error(
                                "Unexpected error processing request: method={}, path={}, timestamp={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                now,
                                ex);
                ApiErrorResponse body = new ApiErrorResponse(
                                "INTERNAL_SERVER_ERROR",
                                "An unexpected error occurred",
                                now);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }

        private Object attributeOrDefault(HttpServletRequest request, String attributeName) {
                Object value = request.getAttribute(attributeName);
                return value != null ? value : "N/A";
        }
}

package br.com.velsis.speedviolation.infrastructure.web.dto;

import java.time.Instant;

public record ApiErrorResponse(
        String error,
        String message,
        Instant timestamp
) {
}

package br.com.velsis.speedviolation.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ViolationRecord(
        UUID id,
        String licensePlate,
        String equipmentId,
        CaptureOrigin origin,
        int measuredSpeed,
        int consideredSpeed,
        int speedLimit,
        double excessPercentage,
        ViolationSeverity severity,
        String ctbCode,
        Instant captureTimestamp,
        Instant processedAt
) {
}

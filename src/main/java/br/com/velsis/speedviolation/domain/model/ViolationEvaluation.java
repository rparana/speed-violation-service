package br.com.velsis.speedviolation.domain.model;

import java.time.Instant;

public record ViolationEvaluation(
        String licensePlate,
        String equipmentId,
        int measuredSpeed,
        int consideredSpeed,
        int speedLimit,
        double excessPercentage,
        boolean hasViolation,
        ViolationDetails violation,
        Instant processedAt
) {
}

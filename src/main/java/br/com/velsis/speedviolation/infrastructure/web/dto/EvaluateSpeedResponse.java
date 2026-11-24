package br.com.velsis.speedviolation.infrastructure.web.dto;

import br.com.velsis.speedviolation.domain.model.ViolationEvaluation;

import java.time.Instant;

public record EvaluateSpeedResponse(
        String licensePlate,
        String equipmentId,
        int measuredSpeed,
        int consideredSpeed,
        int speedLimit,
        double excessPercentage,
        boolean hasViolation,
        ViolationDetailsResponse violation,
        Instant processedAt
) {
    public static EvaluateSpeedResponse fromDomain(ViolationEvaluation evaluation) {
        return new EvaluateSpeedResponse(
                evaluation.licensePlate(),
                evaluation.equipmentId(),
                evaluation.measuredSpeed(),
                evaluation.consideredSpeed(),
                evaluation.speedLimit(),
                evaluation.excessPercentage(),
                evaluation.hasViolation(),
                ViolationDetailsResponse.fromDomain(evaluation.violation()),
                evaluation.processedAt()
        );
    }
}

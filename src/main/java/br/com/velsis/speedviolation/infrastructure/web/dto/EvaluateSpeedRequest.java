package br.com.velsis.speedviolation.infrastructure.web.dto;

public record EvaluateSpeedRequest(
        String licensePlate,
        Integer measuredSpeed,
        Integer speedLimit,
        String equipmentId,
        String captureTimestamp
) {
}

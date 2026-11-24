package br.com.velsis.speedviolation.domain.model;

import java.time.Instant;

public record SpeedReading(
        String licensePlate,
        int measuredSpeed,
        int speedLimit,
        String equipmentId,
        CaptureOrigin origin,
        Instant captureTimestamp
) {
}

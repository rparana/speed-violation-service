package br.com.velsis.speedviolation.infrastructure.web.dto;

import br.com.velsis.speedviolation.domain.model.ViolationRecord;

import java.time.Instant;
import java.util.UUID;

public record ViolationRecordResponse(
        UUID id,
        String licensePlate,
        String equipmentId,
        String origin,
        int measuredSpeed,
        int consideredSpeed,
        int speedLimit,
        double excessPercentage,
        String severity,
        String ctbCode,
        Instant captureTimestamp,
        Instant processedAt
) {
    public static ViolationRecordResponse fromDomain(ViolationRecord record) {
        return new ViolationRecordResponse(
                record.id(),
                record.licensePlate(),
                record.equipmentId(),
                record.origin().name(),
                record.measuredSpeed(),
                record.consideredSpeed(),
                record.speedLimit(),
                record.excessPercentage(),
                record.severity().name(),
                record.ctbCode(),
                record.captureTimestamp(),
                record.processedAt()
        );
    }
}

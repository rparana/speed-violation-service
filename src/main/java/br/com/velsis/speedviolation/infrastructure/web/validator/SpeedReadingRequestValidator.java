package br.com.velsis.speedviolation.infrastructure.web.validator;

import br.com.velsis.speedviolation.domain.model.CaptureOrigin;
import br.com.velsis.speedviolation.domain.model.SpeedReading;
import br.com.velsis.speedviolation.infrastructure.web.dto.EvaluateSpeedRequest;
import br.com.velsis.speedviolation.infrastructure.web.exception.SpeedValidationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class SpeedReadingRequestValidator {

    private static final Pattern OLD_PLATE_PATTERN = Pattern.compile("^[A-Z]{3}[0-9]{4}$");
    private static final Pattern MERCOSUL_PLATE_PATTERN = Pattern.compile("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");

    private final Clock clock;

    public SpeedReadingRequestValidator(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public SpeedReading validateAndBuild(String originHeader, EvaluateSpeedRequest request) {
        CaptureOrigin origin = validateOrigin(originHeader);
        if (request == null) {
            throw new SpeedValidationException("INVALID_REQUEST", "Request body must not be null");
        }

        String normalizedPlate = validateLicensePlate(request.licensePlate());
        int measuredSpeed = validatePositiveInt(
                request.measuredSpeed(),
                "INVALID_MEASURED_SPEED",
                "measuredSpeed must be greater than zero"
        );
        int speedLimit = validatePositiveInt(
                request.speedLimit(),
                "INVALID_SPEED_LIMIT",
                "speedLimit must be greater than zero"
        );
        String equipmentId = validateEquipmentId(request.equipmentId());
        Instant captureTimestamp = validateCaptureTimestamp(request.captureTimestamp());

        return new SpeedReading(
                normalizedPlate,
                measuredSpeed,
                speedLimit,
                equipmentId,
                origin,
                captureTimestamp
        );
    }

    public String validateLicensePlate(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new SpeedValidationException(
                    "INVALID_LICENSE_PLATE",
                    "licensePlate must not be null or blank"
            );
        }
        String normalized = licensePlate.trim().toUpperCase(Locale.ROOT);
        if (!OLD_PLATE_PATTERN.matcher(normalized).matches()
                && !MERCOSUL_PLATE_PATTERN.matcher(normalized).matches()) {
            throw new SpeedValidationException(
                    "INVALID_LICENSE_PLATE",
                    "licensePlate must match old (AAA0000) or Mercosul (AAA0A00) format"
            );
        }
        return normalized;
    }

    private CaptureOrigin validateOrigin(String originHeader) {
        if (originHeader == null || originHeader.isBlank()) {
            throw new SpeedValidationException(
                    "INVALID_ORIGIN",
                    "Header x-origin is required and must be one of: FIXED, MOBILE, HANDHELD"
            );
        }
        try {
            return CaptureOrigin.fromString(originHeader);
        } catch (IllegalArgumentException ex) {
            throw new SpeedValidationException(
                    "INVALID_ORIGIN",
                    "Header x-origin must be one of: FIXED, MOBILE, HANDHELD (case-sensitive)",
                    ex
            );
        }
    }

    private int validatePositiveInt(Integer value, String errorCode, String message) {
        if (value == null || value <= 0) {
            throw new SpeedValidationException(errorCode, message);
        }
        return value;
    }

    private String validateEquipmentId(String equipmentId) {
        if (equipmentId == null || equipmentId.isBlank()) {
            throw new SpeedValidationException(
                    "INVALID_EQUIPMENT_ID",
                    "equipmentId must not be null or blank"
            );
        }
        return equipmentId.trim();
    }

    private Instant validateCaptureTimestamp(String captureTimestamp) {
        if (captureTimestamp == null || captureTimestamp.isBlank()) {
            throw new SpeedValidationException(
                    "INVALID_CAPTURE_TIMESTAMP",
                    "captureTimestamp must be a valid ISO-8601 UTC timestamp"
            );
        }
        Instant parsed;
        try {
            parsed = Instant.parse(captureTimestamp.trim());
        } catch (DateTimeParseException ex) {
            throw new SpeedValidationException(
                    "INVALID_CAPTURE_TIMESTAMP",
                    "captureTimestamp must be a valid ISO-8601 timestamp (e.g. 2026-11-24T13:00:00Z)",
                    ex
            );
        }
        if (parsed.isAfter(Instant.now(clock))) {
            throw new SpeedValidationException(
                    "FUTURE_CAPTURE_TIMESTAMP",
                    "captureTimestamp must not be in the future"
            );
        }
        return parsed;
    }
}

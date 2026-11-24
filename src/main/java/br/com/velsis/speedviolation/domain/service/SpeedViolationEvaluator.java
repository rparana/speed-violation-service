package br.com.velsis.speedviolation.domain.service;

import br.com.velsis.speedviolation.domain.model.SpeedReading;
import br.com.velsis.speedviolation.domain.model.TolerancePolicy;
import br.com.velsis.speedviolation.domain.model.ViolationDetails;
import br.com.velsis.speedviolation.domain.model.ViolationEvaluation;
import br.com.velsis.speedviolation.domain.model.ViolationSeverity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class SpeedViolationEvaluator {

    private final TolerancePolicy policy;
    private final Clock clock;

    public SpeedViolationEvaluator(TolerancePolicy policy, Clock clock) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ViolationEvaluation evaluate(SpeedReading reading) {
        Objects.requireNonNull(reading, "reading must not be null");

        int consideredSpeed = calculateConsideredSpeed(reading);
        Instant processedAt = Instant.now(clock);

        if (consideredSpeed <= reading.speedLimit()) {
            return new ViolationEvaluation(
                    reading.licensePlate(),
                    reading.equipmentId(),
                    reading.measuredSpeed(),
                    consideredSpeed,
                    reading.speedLimit(),
                    0.0,
                    false,
                    null,
                    processedAt
            );
        }

        double rawExcess = ((double) (consideredSpeed - reading.speedLimit()) / reading.speedLimit()) * 100.0;
        ViolationSeverity severity = classifySeverity(rawExcess);
        double excessPercentage = BigDecimal.valueOf(rawExcess)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        return new ViolationEvaluation(
                reading.licensePlate(),
                reading.equipmentId(),
                reading.measuredSpeed(),
                consideredSpeed,
                reading.speedLimit(),
                excessPercentage,
                true,
                ViolationDetails.fromSeverity(severity),
                processedAt
        );
    }

    private int calculateConsideredSpeed(SpeedReading reading) {
        int rawConsidered;
        if (reading.speedLimit() <= policy.speedLimitThreshold()) {
            rawConsidered = reading.measuredSpeed() - policy.fixedMarginKmh();
        } else {
            rawConsidered = (int) (reading.measuredSpeed() * (1.0 - (policy.percentageMargin() / 100.0)));
        }
        return Math.max(0, rawConsidered);
    }

    private ViolationSeverity classifySeverity(double rawExcess) {
        if (rawExcess <= 20.0) {
            return ViolationSeverity.MEDIUM;
        }
        if (rawExcess <= 50.0) {
            return ViolationSeverity.SERIOUS;
        }
        return ViolationSeverity.VERY_SERIOUS;
    }
}

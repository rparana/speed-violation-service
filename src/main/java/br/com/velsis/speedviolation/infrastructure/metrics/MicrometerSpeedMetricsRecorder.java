package br.com.velsis.speedviolation.infrastructure.metrics;

import br.com.velsis.speedviolation.domain.metrics.SpeedMetricsRecorder;
import br.com.velsis.speedviolation.domain.model.CaptureOrigin;
import br.com.velsis.speedviolation.domain.model.ViolationSeverity;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MicrometerSpeedMetricsRecorder implements SpeedMetricsRecorder {

    private static final String METRIC_EVALUATIONS_TOTAL = "speed_evaluations_total";
    private static final String METRIC_VIOLATIONS_TOTAL = "speed_violations_total";

    private final MeterRegistry meterRegistry;

    public MicrometerSpeedMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    @Override
    public void recordEvaluation(CaptureOrigin origin, boolean hasViolation, ViolationSeverity severity) {
        Objects.requireNonNull(origin, "origin must not be null");
        meterRegistry.counter(METRIC_EVALUATIONS_TOTAL, "origin", origin.name()).increment();

        if (hasViolation && severity != null) {
            meterRegistry.counter(
                    METRIC_VIOLATIONS_TOTAL,
                    "origin", origin.name(),
                    "severity", severity.name()).increment();
        }
    }
}

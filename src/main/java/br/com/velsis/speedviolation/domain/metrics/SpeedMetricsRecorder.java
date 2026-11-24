package br.com.velsis.speedviolation.domain.metrics;

import br.com.velsis.speedviolation.domain.model.CaptureOrigin;
import br.com.velsis.speedviolation.domain.model.ViolationSeverity;

public interface SpeedMetricsRecorder {

    void recordEvaluation(CaptureOrigin origin, boolean hasViolation, ViolationSeverity severity);
}

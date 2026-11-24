package br.com.velsis.speedviolation.application.usecase;

import br.com.velsis.speedviolation.domain.metrics.SpeedMetricsRecorder;
import br.com.velsis.speedviolation.domain.model.SpeedReading;
import br.com.velsis.speedviolation.domain.model.ViolationEvaluation;
import br.com.velsis.speedviolation.domain.model.ViolationRecord;
import br.com.velsis.speedviolation.domain.model.ViolationSeverity;
import br.com.velsis.speedviolation.domain.repository.ViolationRepository;
import br.com.velsis.speedviolation.domain.service.SpeedViolationEvaluator;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class EvaluateSpeedUseCase {

    private final SpeedViolationEvaluator evaluator;
    private final ViolationRepository repository;
    private final SpeedMetricsRecorder metricsRecorder;

    public EvaluateSpeedUseCase(
            SpeedViolationEvaluator evaluator,
            ViolationRepository repository,
            SpeedMetricsRecorder metricsRecorder) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.metricsRecorder = Objects.requireNonNull(metricsRecorder, "metricsRecorder must not be null");
    }

    public ViolationEvaluation execute(SpeedReading reading) {
        ViolationEvaluation evaluation = evaluator.evaluate(reading);

        ViolationSeverity severity = null;
        if (evaluation.hasViolation()) {
            severity = evaluation.violation().severity();
            ViolationRecord record = new ViolationRecord(
                    UUID.randomUUID(),
                    evaluation.licensePlate(),
                    evaluation.equipmentId(),
                    reading.origin(),
                    evaluation.measuredSpeed(),
                    evaluation.consideredSpeed(),
                    evaluation.speedLimit(),
                    evaluation.excessPercentage(),
                    severity,
                    evaluation.violation().ctbCode(),
                    reading.captureTimestamp(),
                    evaluation.processedAt());
            repository.save(record);
        }

        metricsRecorder.recordEvaluation(reading.origin(), evaluation.hasViolation(), severity);
        return evaluation;
    }
}

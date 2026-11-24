package br.com.velsis.speedviolation.application.usecase;

import br.com.velsis.speedviolation.domain.model.CaptureOrigin;
import br.com.velsis.speedviolation.domain.model.SpeedReading;
import br.com.velsis.speedviolation.domain.model.TolerancePolicy;
import br.com.velsis.speedviolation.domain.model.ViolationEvaluation;
import br.com.velsis.speedviolation.domain.model.ViolationRecord;
import br.com.velsis.speedviolation.domain.model.ViolationSeverity;
import br.com.velsis.speedviolation.domain.repository.ViolationRepository;
import br.com.velsis.speedviolation.domain.service.SpeedViolationEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EvaluateSpeedUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-24T14:00:00Z");
    private static final Instant CAPTURE_TIME = Instant.parse("2026-11-24T13:59:30Z");

    @Mock
    private ViolationRepository repository;

    private EvaluateSpeedUseCase useCase;

    @BeforeEach
    void setUp() {
        SpeedViolationEvaluator evaluator = new SpeedViolationEvaluator(
                new TolerancePolicy(7, 7.0, 100),
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        useCase = new EvaluateSpeedUseCase(evaluator, repository);
    }

    @Test
    void shouldPersistViolationRecordWhenHasViolationIsTrue() {
        SpeedReading reading = new SpeedReading(
                "ABC1D23",
                127,
                100,
                "EQ-001",
                CaptureOrigin.FIXED,
                CAPTURE_TIME
        );

        ViolationEvaluation evaluation = useCase.execute(reading);

        assertThat(evaluation.hasViolation()).isTrue();
        assertThat(evaluation.consideredSpeed()).isEqualTo(120);
        assertThat(evaluation.excessPercentage()).isEqualTo(20.0);

        ArgumentCaptor<ViolationRecord> captor = ArgumentCaptor.forClass(ViolationRecord.class);
        verify(repository).save(captor.capture());

        ViolationRecord saved = captor.getValue();
        assertThat(saved.id()).isNotNull();
        assertThat(saved.licensePlate()).isEqualTo("ABC1D23");
        assertThat(saved.equipmentId()).isEqualTo("EQ-001");
        assertThat(saved.origin()).isEqualTo(CaptureOrigin.FIXED);
        assertThat(saved.measuredSpeed()).isEqualTo(127);
        assertThat(saved.consideredSpeed()).isEqualTo(120);
        assertThat(saved.speedLimit()).isEqualTo(100);
        assertThat(saved.excessPercentage()).isEqualTo(20.0);
        assertThat(saved.severity()).isEqualTo(ViolationSeverity.MEDIUM);
        assertThat(saved.ctbCode()).isEqualTo("218-I");
        assertThat(saved.captureTimestamp()).isEqualTo(CAPTURE_TIME);
        assertThat(saved.processedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void shouldNotPersistAnythingWhenHasViolationIsFalse() {
        SpeedReading reading = new SpeedReading(
                "ABC1D23",
                67,
                60,
                "EQ-001",
                CaptureOrigin.MOBILE,
                CAPTURE_TIME
        );

        ViolationEvaluation evaluation = useCase.execute(reading);

        assertThat(evaluation.hasViolation()).isFalse();
        assertThat(evaluation.violation()).isNull();
        verifyNoInteractions(repository);
    }
}

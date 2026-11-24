package br.com.velsis.speedviolation.domain.service;

import br.com.velsis.speedviolation.domain.model.CaptureOrigin;
import br.com.velsis.speedviolation.domain.model.SpeedReading;
import br.com.velsis.speedviolation.domain.model.TolerancePolicy;
import br.com.velsis.speedviolation.domain.model.ViolationEvaluation;
import br.com.velsis.speedviolation.domain.model.ViolationSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SpeedViolationEvaluatorTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-24T13:00:00Z");

    private SpeedViolationEvaluator evaluator;

    @BeforeEach
    void setUp() {
        TolerancePolicy policy = new TolerancePolicy(7, 7.0, 100);
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        evaluator = new SpeedViolationEvaluator(policy, clock);
    }

    @Test
    void shouldNotFlagViolationWhenConsideredSpeedEqualsLimitWithFixedMargin() {
        SpeedReading reading = reading(67, 60);

        ViolationEvaluation result = evaluator.evaluate(reading);

        assertThat(result.consideredSpeed()).isEqualTo(60);
        assertThat(result.excessPercentage()).isEqualTo(0.0);
        assertThat(result.hasViolation()).isFalse();
        assertThat(result.violation()).isNull();
        assertThat(result.processedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void shouldClampConsideredSpeedToZeroWhenMeasuredSpeedIsLowerThanFixedMargin() {
        SpeedReading reading = reading(5, 60);

        ViolationEvaluation result = evaluator.evaluate(reading);

        assertThat(result.consideredSpeed()).isZero();
        assertThat(result.hasViolation()).isFalse();
        assertThat(result.excessPercentage()).isEqualTo(0.0);
        assertThat(result.violation()).isNull();
    }

    @Test
    void shouldClassifyAsMediumAtExactTwentyPercentBoundary() {
        SpeedReading reading = reading(127, 100);

        ViolationEvaluation result = evaluator.evaluate(reading);

        assertThat(result.consideredSpeed()).isEqualTo(120);
        assertThat(result.excessPercentage()).isEqualTo(20.0);
        assertThat(result.hasViolation()).isTrue();
        assertThat(result.violation()).isNotNull();
        assertThat(result.violation().severity()).isEqualTo(ViolationSeverity.MEDIUM);
        assertThat(result.violation().ctbCode()).isEqualTo("218-I");
        assertThat(result.processedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void shouldClassifyAsSeriousAboveTwentyAndUpToFiftyPercent() {
        ViolationEvaluation lowerSerious = evaluator.evaluate(reading(128, 100));
        ViolationEvaluation upperSerious = evaluator.evaluate(reading(157, 100));

        assertThat(lowerSerious.consideredSpeed()).isEqualTo(121);
        assertThat(lowerSerious.excessPercentage()).isEqualTo(21.0);
        assertThat(lowerSerious.violation().severity()).isEqualTo(ViolationSeverity.SERIOUS);
        assertThat(lowerSerious.violation().ctbCode()).isEqualTo("218-II");

        assertThat(upperSerious.consideredSpeed()).isEqualTo(150);
        assertThat(upperSerious.excessPercentage()).isEqualTo(50.0);
        assertThat(upperSerious.violation().severity()).isEqualTo(ViolationSeverity.SERIOUS);
        assertThat(upperSerious.violation().ctbCode()).isEqualTo("218-II");
    }

    @Test
    void shouldClassifyAsVerySeriousAboveFiftyPercent() {
        SpeedReading reading = reading(158, 100);

        ViolationEvaluation result = evaluator.evaluate(reading);

        assertThat(result.consideredSpeed()).isEqualTo(151);
        assertThat(result.excessPercentage()).isEqualTo(51.0);
        assertThat(result.hasViolation()).isTrue();
        assertThat(result.violation().severity()).isEqualTo(ViolationSeverity.VERY_SERIOUS);
        assertThat(result.violation().ctbCode()).isEqualTo("218-III");
    }

    @Test
    void shouldApplyPercentageMarginAndTruncateToIntAboveThreshold() {
        ViolationEvaluation noViolation = evaluator.evaluate(reading(119, 110));
        assertThat(noViolation.consideredSpeed()).isEqualTo(110);
        assertThat(noViolation.hasViolation()).isFalse();
        assertThat(noViolation.excessPercentage()).isEqualTo(0.0);

        ViolationEvaluation violation = evaluator.evaluate(reading(130, 110));
        assertThat(violation.consideredSpeed()).isEqualTo(120);
        assertThat(violation.hasViolation()).isTrue();
        assertThat(violation.excessPercentage()).isEqualTo(9.09);
        assertThat(violation.violation().severity()).isEqualTo(ViolationSeverity.MEDIUM);
        assertThat(violation.violation().ctbCode()).isEqualTo("218-I");
    }

    @Test
    void shouldNotFlagViolationWhenMeasuredSpeedIsEqualToOrBelowLimit() {
        ViolationEvaluation atLimit = evaluator.evaluate(reading(60, 60));
        assertThat(atLimit.consideredSpeed()).isEqualTo(53);
        assertThat(atLimit.hasViolation()).isFalse();
        assertThat(atLimit.excessPercentage()).isEqualTo(0.0);
        assertThat(atLimit.violation()).isNull();

        ViolationEvaluation belowLimit = evaluator.evaluate(reading(55, 60));
        assertThat(belowLimit.consideredSpeed()).isEqualTo(48);
        assertThat(belowLimit.hasViolation()).isFalse();
        assertThat(belowLimit.excessPercentage()).isEqualTo(0.0);
        assertThat(belowLimit.violation()).isNull();
    }

    @Test
    void shouldUseFixedMarginAtExactSpeedLimitThresholdOf100() {
        ViolationEvaluation result = evaluator.evaluate(reading(108, 100));

        assertThat(result.consideredSpeed()).isEqualTo(101);
        assertThat(result.hasViolation()).isTrue();
        assertThat(result.excessPercentage()).isEqualTo(1.0);
        assertThat(result.violation().severity()).isEqualTo(ViolationSeverity.MEDIUM);
        assertThat(result.violation().ctbCode()).isEqualTo("218-I");
    }

    private SpeedReading reading(int measuredSpeed, int speedLimit) {
        return new SpeedReading(
                "ABC1D23",
                measuredSpeed,
                speedLimit,
                "EQ-001",
                CaptureOrigin.FIXED,
                Instant.parse("2026-11-24T12:59:00Z")
        );
    }
}

package br.com.velsis.speedviolation.infrastructure.web.validator;

import br.com.velsis.speedviolation.domain.model.CaptureOrigin;
import br.com.velsis.speedviolation.domain.model.SpeedReading;
import br.com.velsis.speedviolation.infrastructure.web.dto.EvaluateSpeedRequest;
import br.com.velsis.speedviolation.infrastructure.web.exception.SpeedValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeedReadingRequestValidatorTest {

        private static final Instant FIXED_NOW = Instant.parse("2026-11-24T14:00:00Z");

        private SpeedReadingRequestValidator validator;

        @BeforeEach
        void setUp() {
                validator = new SpeedReadingRequestValidator(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        }

        @Test
        void shouldValidateOldLicensePlateFormatAndNormalize() {
                EvaluateSpeedRequest request = new EvaluateSpeedRequest(
                                " abc1234 ",
                                90,
                                80,
                                " EQ-001 ",
                                "2026-11-24T13:59:00Z");

                SpeedReading reading = validator.validateAndBuild("FIXED", request);

                assertThat(reading.licensePlate()).isEqualTo("ABC1234");
                assertThat(reading.measuredSpeed()).isEqualTo(90);
                assertThat(reading.speedLimit()).isEqualTo(80);
                assertThat(reading.equipmentId()).isEqualTo("EQ-001");
                assertThat(reading.origin()).isEqualTo(CaptureOrigin.FIXED);
                assertThat(reading.captureTimestamp()).isEqualTo(Instant.parse("2026-11-24T13:59:00Z"));
        }

        @Test
        void shouldValidateMercosulLicensePlateFormat() {
                EvaluateSpeedRequest request = new EvaluateSpeedRequest(
                                "ABC1D23",
                                115,
                                100,
                                "EQ-002",
                                "2026-11-24T14:00:00Z");

                SpeedReading reading = validator.validateAndBuild("MOBILE", request);

                assertThat(reading.licensePlate()).isEqualTo("ABC1D23");
                assertThat(reading.origin()).isEqualTo(CaptureOrigin.MOBILE);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "AB1234", "ABCD123", "ABC12345", "1234567", "ABC12D3", "ABC-1234" })
        void shouldRejectInvalidLicensePlates(String plate) {
                EvaluateSpeedRequest request = new EvaluateSpeedRequest(
                                plate,
                                90,
                                80,
                                "EQ-001",
                                "2026-11-24T13:59:00Z");

                assertThatThrownBy(() -> validator.validateAndBuild("FIXED", request))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_LICENSE_PLATE");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "fixed", "Mobile", "handheld", "DRONE" })
        void shouldRejectInvalidOrWrongCaseOriginHeader(String origin) {
                EvaluateSpeedRequest request = validRequest();

                assertThatThrownBy(() -> validator.validateAndBuild(origin, request))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_ORIGIN");
        }

        @Test
        void shouldRejectNullOrNonPositiveMeasuredSpeed() {
                assertThatThrownBy(() -> validator.validateAndBuild("FIXED",
                                new EvaluateSpeedRequest("ABC1D23", null, 80, "EQ-001", "2026-11-24T13:59:00Z")))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_MEASURED_SPEED");

                assertThatThrownBy(() -> validator.validateAndBuild("FIXED",
                                new EvaluateSpeedRequest("ABC1D23", 0, 80, "EQ-001", "2026-11-24T13:59:00Z")))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_MEASURED_SPEED");

                assertThatThrownBy(() -> validator.validateAndBuild("FIXED",
                                new EvaluateSpeedRequest("ABC1D23", -10, 80, "EQ-001", "2026-11-24T13:59:00Z")))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_MEASURED_SPEED");
        }

        @Test
        void shouldRejectNullOrNonPositiveSpeedLimit() {
                assertThatThrownBy(() -> validator.validateAndBuild("FIXED",
                                new EvaluateSpeedRequest("ABC1D23", 90, null, "EQ-001", "2026-11-24T13:59:00Z")))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_SPEED_LIMIT");

                assertThatThrownBy(() -> validator.validateAndBuild("FIXED",
                                new EvaluateSpeedRequest("ABC1D23", 90, 0, "EQ-001", "2026-11-24T13:59:00Z")))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_SPEED_LIMIT");

                assertThatThrownBy(() -> validator.validateAndBuild("FIXED",
                                new EvaluateSpeedRequest("ABC1D23", 90, -60, "EQ-001", "2026-11-24T13:59:00Z")))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_SPEED_LIMIT");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        void shouldRejectNullOrBlankEquipmentId(String equipmentId) {
                EvaluateSpeedRequest request = new EvaluateSpeedRequest(
                                "ABC1D23",
                                90,
                                80,
                                equipmentId,
                                "2026-11-24T13:59:00Z");

                assertThatThrownBy(() -> validator.validateAndBuild("HANDHELD", request))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_EQUIPMENT_ID");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "2026-11-24 13:59:00", "not-a-timestamp" })
        void shouldRejectInvalidCaptureTimestamp(String timestamp) {
                EvaluateSpeedRequest request = new EvaluateSpeedRequest(
                                "ABC1D23",
                                90,
                                80,
                                "EQ-001",
                                timestamp);

                assertThatThrownBy(() -> validator.validateAndBuild("FIXED", request))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("INVALID_CAPTURE_TIMESTAMP");
        }

        @Test
        void shouldRejectFutureCaptureTimestamp() {
                EvaluateSpeedRequest request = new EvaluateSpeedRequest(
                                "ABC1D23",
                                90,
                                80,
                                "EQ-001",
                                "2026-11-24T14:00:01Z");

                assertThatThrownBy(() -> validator.validateAndBuild("FIXED", request))
                                .isInstanceOf(SpeedValidationException.class)
                                .extracting("errorCode")
                                .isEqualTo("FUTURE_CAPTURE_TIMESTAMP");
        }

        private EvaluateSpeedRequest validRequest() {
                return new EvaluateSpeedRequest(
                                "ABC1D23",
                                90,
                                80,
                                "EQ-001",
                                "2026-11-24T13:59:00Z");
        }
}

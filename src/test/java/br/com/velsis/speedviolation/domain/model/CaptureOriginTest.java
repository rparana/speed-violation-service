package br.com.velsis.speedviolation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CaptureOriginTest {

    @ParameterizedTest
    @CsvSource({
            "FIXED, FIXED",
            "MOBILE, MOBILE",
            "HANDHELD, HANDHELD"
    })
    void shouldParseValidOrigins(String input, CaptureOrigin expected) {
        assertThat(CaptureOrigin.fromString(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"fixed", "Mobile", "handheld", "   ", "RADAR"})
    void shouldThrowIllegalArgumentExceptionWhenOriginIsNullInvalidOrWrongCase(String input) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CaptureOrigin.fromString(input));
    }

    @Test
    void shouldContainExpectedEnumValues() {
        assertThat(CaptureOrigin.values())
                .containsExactly(CaptureOrigin.FIXED, CaptureOrigin.MOBILE, CaptureOrigin.HANDHELD);
    }
}

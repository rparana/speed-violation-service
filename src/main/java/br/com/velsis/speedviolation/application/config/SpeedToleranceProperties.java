package br.com.velsis.speedviolation.application.config;

import br.com.velsis.speedviolation.domain.model.TolerancePolicy;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "speed-violation.tolerance")
public record SpeedToleranceProperties(
        @Positive int fixedMarginKmh,
        @Positive double percentageMargin,
        @Positive int speedLimitThreshold) {
    public TolerancePolicy toDomainPolicy() {
        return new TolerancePolicy(fixedMarginKmh, percentageMargin, speedLimitThreshold);
    }
}

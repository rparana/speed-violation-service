package br.com.velsis.speedviolation.domain.model;

public record TolerancePolicy(
        int fixedMarginKmh,
        double percentageMargin,
        int speedLimitThreshold) {
    public TolerancePolicy {
        if (fixedMarginKmh <= 0) {
            throw new IllegalArgumentException("fixedMarginKmh must be positive");
        }
        if (percentageMargin <= 0) {
            throw new IllegalArgumentException("percentageMargin must be positive");
        }
        if (speedLimitThreshold <= 0) {
            throw new IllegalArgumentException("speedLimitThreshold must be positive");
        }
    }
}

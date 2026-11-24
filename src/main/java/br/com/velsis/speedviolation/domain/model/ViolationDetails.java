package br.com.velsis.speedviolation.domain.model;

public record ViolationDetails(
        ViolationSeverity severity,
        String ctbCode
) {
    public static ViolationDetails fromSeverity(ViolationSeverity severity) {
        if (severity == null) {
            throw new IllegalArgumentException("Violation severity must not be null");
        }
        return new ViolationDetails(severity, severity.getCtbCode());
    }
}

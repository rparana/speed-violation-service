package br.com.velsis.speedviolation.domain.model;

public enum CaptureOrigin {
    FIXED,
    MOBILE,
    HANDHELD;

    public static CaptureOrigin fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Capture origin must not be null");
        }
        try {
            return CaptureOrigin.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid capture origin: " + value, ex);
        }
    }
}

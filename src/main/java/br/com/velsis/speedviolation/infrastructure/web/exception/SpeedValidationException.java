package br.com.velsis.speedviolation.infrastructure.web.exception;

public class SpeedValidationException extends RuntimeException {

    private final String errorCode;

    public SpeedValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SpeedValidationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

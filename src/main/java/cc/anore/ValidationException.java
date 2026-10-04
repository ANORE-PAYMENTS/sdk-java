package cc.anore;

public class ValidationException extends ApiException {
    public ValidationException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

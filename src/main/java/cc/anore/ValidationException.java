package cc.anore;

/** 400 — invalid request (amount/description/shopId/JSON). */
public class ValidationException extends ApiException {
    public ValidationException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

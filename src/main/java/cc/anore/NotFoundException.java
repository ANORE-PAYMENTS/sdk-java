package cc.anore;

/** 404 — shop or payment not found. */
public class NotFoundException extends ApiException {
    public NotFoundException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

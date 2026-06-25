package cc.anore;

/** 403 — shop blocked, or access denied. */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

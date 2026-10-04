package cc.anore;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

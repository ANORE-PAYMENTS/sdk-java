package cc.anore;

public class NotFoundException extends ApiException {
    public NotFoundException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

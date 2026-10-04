package cc.anore;

public class AuthenticationException extends ApiException {
    public AuthenticationException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

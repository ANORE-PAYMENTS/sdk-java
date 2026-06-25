package cc.anore;

/** 401 — missing or invalid API key / signature. */
public class AuthenticationException extends ApiException {
    public AuthenticationException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

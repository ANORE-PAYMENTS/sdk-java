package cc.anore;

import java.util.Map;

public class AuthenticationException extends ApiException {
    public AuthenticationException(String message, int status, String requestId) {
        super(message, status, requestId);
    }

    public AuthenticationException(String message, int status, String requestId, Map<String, Object> details) {
        super(message, status, requestId, details);
    }
}

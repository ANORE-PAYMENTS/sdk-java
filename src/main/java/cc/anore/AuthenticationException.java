package cc.anore;

import java.util.Map;

public class AuthenticationException extends ApiException {
    private static final long serialVersionUID = 3554125210410410796L;

    public AuthenticationException(String message, int status, String requestId) {
        super(message, status, requestId);
    }

    public AuthenticationException(String message, int status, String requestId, Map<String, Object> details) {
        super(message, status, requestId, details);
    }
}

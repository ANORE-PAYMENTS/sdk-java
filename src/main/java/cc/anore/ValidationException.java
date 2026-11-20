package cc.anore;

import java.util.Map;

public class ValidationException extends ApiException {
    private static final long serialVersionUID = -7266735501397237155L;

    public ValidationException(String message, int status, String requestId) {
        super(message, status, requestId);
    }

    public ValidationException(String message, int status, String requestId, Map<String, Object> details) {
        super(message, status, requestId, details);
    }
}

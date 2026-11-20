package cc.anore;

import java.util.Map;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String message, int status, String requestId) {
        super(message, status, requestId);
    }

    public ForbiddenException(String message, int status, String requestId, Map<String, Object> details) {
        super(message, status, requestId, details);
    }
}

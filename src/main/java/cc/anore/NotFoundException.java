package cc.anore;

import java.util.Map;

public class NotFoundException extends ApiException {
    private static final long serialVersionUID = 774601712241732611L;

    public NotFoundException(String message, int status, String requestId) {
        super(message, status, requestId);
    }

    public NotFoundException(String message, int status, String requestId, Map<String, Object> details) {
        super(message, status, requestId, details);
    }
}

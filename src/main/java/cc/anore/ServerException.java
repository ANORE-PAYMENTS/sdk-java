package cc.anore;

import java.util.Map;

public class ServerException extends ApiException {
    private static final long serialVersionUID = 6093978326650854947L;

    public ServerException(String message, int status, String requestId) {
        super(message, status, requestId);
    }

    public ServerException(String message, int status, String requestId, Map<String, Object> details) {
        super(message, status, requestId, details);
    }
}

package cc.anore;

import java.util.Map;

public class ServerException extends ApiException {
    public ServerException(String message, int status, String requestId) {
        super(message, status, requestId);
    }

    public ServerException(String message, int status, String requestId, Map<String, Object> details) {
        super(message, status, requestId, details);
    }
}

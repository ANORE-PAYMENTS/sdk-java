package cc.anore;

public class ServerException extends ApiException {
    public ServerException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

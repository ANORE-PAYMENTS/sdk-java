package cc.anore;

/** 5xx — something went wrong on anore's side. */
public class ServerException extends ApiException {
    public ServerException(String message, int status, String requestId) {
        super(message, status, requestId);
    }
}

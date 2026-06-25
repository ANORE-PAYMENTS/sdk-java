package cc.anore;

/** The API responded with a non-2xx status. */
public class ApiException extends AnoreException {
    private final int status;
    private final String requestId;

    public ApiException(String message, int status, String requestId) {
        super(message);
        this.status = status;
        this.requestId = requestId;
    }

    /** HTTP status code returned by the API. */
    public int getStatus() {
        return status;
    }

    /** Value of the X-Request-Id response header, if any. */
    public String getRequestId() {
        return requestId;
    }

    /** Map an HTTP status to the most specific ApiException subclass. */
    public static ApiException forStatus(int status, String message, String requestId) {
        switch (status) {
            case 400:
                return new ValidationException(message, status, requestId);
            case 401:
                return new AuthenticationException(message, status, requestId);
            case 403:
                return new ForbiddenException(message, status, requestId);
            case 404:
                return new NotFoundException(message, status, requestId);
            default:
                if (status >= 500) return new ServerException(message, status, requestId);
                return new ApiException(message, status, requestId);
        }
    }
}

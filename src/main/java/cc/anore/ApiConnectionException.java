package cc.anore;

/** Could not reach the API (network error / timeout), even after retries. */
public class ApiConnectionException extends AnoreException {
    public ApiConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

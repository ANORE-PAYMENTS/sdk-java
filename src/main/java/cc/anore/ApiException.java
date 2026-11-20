package cc.anore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiException extends AnoreException {
    private final int status;
    private final String requestId;
    private final Map<String, Object> details;

    public ApiException(String message, int status, String requestId) {
        this(message, status, requestId, Collections.emptyMap());
    }

    public ApiException(String message, int status, String requestId, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.requestId = requestId;
        this.details = details == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public int getStatus() {
        return status;
    }

    public String getRequestId() {
        return requestId;
    }

    /** The parsed API error object, preserved for machine-readable fields such as subscriptionId. */
    public Map<String, Object> getDetails() {
        return details;
    }

    /** Alias matching the API's structured error-data terminology. */
    public Map<String, Object> getData() {
        return details;
    }

    public Object getDetail(String name) {
        return details.get(name);
    }

    public Long getSubscriptionId() {
        Object value = details.get("subscriptionId");
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    public static ApiException forStatus(int status, String message, String requestId) {
        return forStatus(status, message, requestId, Collections.emptyMap());
    }

    public static ApiException forStatus(int status, String message, String requestId, Map<String, Object> details) {
        switch (status) {
            case 400:
                return new ValidationException(message, status, requestId, details);
            case 401:
                return new AuthenticationException(message, status, requestId, details);
            case 403:
                return new ForbiddenException(message, status, requestId, details);
            case 404:
                return new NotFoundException(message, status, requestId, details);
            default:
                if (status >= 500) return new ServerException(message, status, requestId, details);
                return new ApiException(message, status, requestId, details);
        }
    }
}

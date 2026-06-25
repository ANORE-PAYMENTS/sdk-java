package cc.anore;

import java.util.Map;

/** A parsed (and verified) webhook payload. */
public final class WebhookEvent extends Model {
    WebhookEvent(Map<String, Object> raw) {
        super(raw);
    }

    /** e.g. "payment.succeeded". */
    public String event() {
        return str("event");
    }

    public String id() {
        return str("id");
    }

    public String orderId() {
        return str("orderId");
    }

    public Double amount() {
        return doubleVal("amount");
    }

    public String currency() {
        return str("currency");
    }

    public String status() {
        return str("status");
    }

    public String description() {
        return str("description");
    }

    public String shop() {
        return str("shop");
    }

    public String createdAt() {
        return str("createdAt");
    }

    public boolean isSucceeded() {
        return "payment.succeeded".equals(event());
    }

    @Override
    public String toString() {
        return "WebhookEvent{event=" + event() + ", id=" + id() + ", status=" + status() + "}";
    }
}

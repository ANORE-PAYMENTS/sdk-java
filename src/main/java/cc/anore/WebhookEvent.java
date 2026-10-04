package cc.anore;

import java.util.Map;

public final class WebhookEvent extends Model {
    WebhookEvent(Map<String, Object> raw) {
        super(raw);
    }

    public String event() {
        return str("event");
    }

    public String eventId() { return str("eventId"); }
    public boolean test() { return bool("test"); }
    public boolean manualCorrection() { return bool("manualCorrection"); }
    public Long statusRevision() { return longVal("statusRevision"); }

    public String id() {
        return str("id");
    }

    public String orderId() {
        return str("orderId");
    }

    public Double amount() {
        return doubleVal("amount");
    }

    public Double rubAmount() { return doubleVal("rubAmount"); }

    public String currency() {
        return str("currency");
    }

    public Double currencyRate() { return doubleVal("currencyRate"); }

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
        return "payment.succeeded".equals(event()) || "payout.succeeded".equals(event());
    }

    public boolean isPayout() { return event() != null && event().startsWith("payout."); }

    public String externalId() { return str("externalId"); }

    public Long shopId() { return longVal("shopId"); }

    public String method() { return str("method"); }

    public Double fee() { return doubleVal("fee"); }

    public Double netRub() { return doubleVal("netRub"); }

    public Double amountUsdt() { return doubleVal("amountUsdt"); }
    public String address() { return str("address"); }
    public String bank() { return str("bank"); }
    public Double rapiraRate() { return doubleVal("rapiraRate"); }
    public Double cbrRate() { return doubleVal("cbrRate"); }

    public Double settlementRub() { return doubleVal("settlementRub"); }

    public String processedAt() { return str("processedAt"); }

    public String txHash() { return str("txHash"); }

    @Override
    public String toString() {
        return "WebhookEvent{event=" + event() + ", id=" + id() + ", status=" + status() + "}";
    }
}

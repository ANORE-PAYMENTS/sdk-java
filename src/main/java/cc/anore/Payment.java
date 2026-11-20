package cc.anore;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Payment extends Model {
    Payment(Map<String, Object> raw) {
        super(withoutProtectedInput(raw));
    }

    private static Map<String, Object> withoutProtectedInput(Map<String, Object> raw) {
        if (raw == null) return null;
        Map<String, Object> safe = new LinkedHashMap<>(raw);
        safe.remove("user_identity");
        safe.remove("franchise");
        return safe;
    }

    public boolean success() {
        return bool("success");
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

    public Double baseAmount() { return doubleVal("baseAmount"); }

    public String currency() {
        return str("currency");
    }

    public Double currencyRate() { return doubleVal("currencyRate"); }

    public Double rubAmount() { return doubleVal("rubAmount"); }

    public String description() { return str("description"); }

    public String status() {
        return str("status");
    }

    public boolean paid() {
        return bool("paid");
    }

    public boolean test() { return bool("test"); }

    public String paymentUrl() {
        return str("paymentUrl");
    }

    public String sbpUrl() { return str("sbpUrl"); }

    public String method() { return str("method"); }

    public String createdAt() { return str("createdAt"); }

    public String paidAt() { return str("paidAt"); }

    public Long expiresIn() {
        return longVal("expiresIn");
    }

    @Override
    public String toString() {
        return "Payment{id=" + id() + ", status=" + status() + ", amount=" + amount() + "}";
    }
}

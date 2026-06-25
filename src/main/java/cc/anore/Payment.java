package cc.anore;

import java.util.Map;

/** A payment / invoice — response of {@code createPayment} and {@code getPayment}. */
public final class Payment extends Model {
    Payment(Map<String, Object> raw) {
        super(raw);
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

    /** "new" | "paid" | "expired". */
    public String status() {
        return str("status");
    }

    public boolean paid() {
        return bool("paid");
    }

    public String paymentUrl() {
        return str("paymentUrl");
    }

    public Long expiresIn() {
        return longVal("expiresIn");
    }

    @Override
    public String toString() {
        return "Payment{id=" + id() + ", status=" + status() + ", amount=" + amount() + "}";
    }
}

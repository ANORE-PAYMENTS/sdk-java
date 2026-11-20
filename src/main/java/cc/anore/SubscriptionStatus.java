package cc.anore;

import java.util.Map;

public final class SubscriptionStatus extends Model {
    SubscriptionStatus(Map<String, Object> raw) { super(raw); }

    public boolean success() { return bool("success"); }
    public Long subscriptionId() { return longVal("subscriptionId"); }
    public Long shopId() { return longVal("shopId"); }
    public String planId() { return str("planId"); }
    public String planTitle() { return str("planTitle"); }
    public String status() { return str("status"); }
    public String customerId() { return str("customerId"); }
    public String customerEmail() { return str("customerEmail"); }
    public Double amount() { return doubleVal("amount"); }
    public String paymentMethod() { return str("paymentMethod"); }
    public String environment() { return str("environment"); }
    public String nextChargeAt() { return str("nextChargeAt"); }
}

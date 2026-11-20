package cc.anore;

import java.util.Map;

public final class SubscriptionCharge extends Model {
    SubscriptionCharge(Map<String, Object> raw) { super(raw); }

    public boolean success() { return bool("success"); }
    public Long subscriptionId() { return longVal("subscriptionId"); }
    public String chargeId() { return str("chargeId"); }
    public Long paymentId() { return longVal("paymentId"); }
    public String status() { return str("status"); }
}

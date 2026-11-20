package cc.anore;

import java.util.Map;

public final class SubscriptionBinding extends Model {
    SubscriptionBinding(Map<String, Object> raw) { super(raw); }

    public boolean success() { return bool("success"); }
    public Long subscriptionId() { return longVal("subscriptionId"); }
    public Long shopId() { return longVal("shopId"); }
    public String status() { return str("status"); }
    public String qrUrl() { return str("qrUrl"); }
    public String environment() { return str("environment"); }
}

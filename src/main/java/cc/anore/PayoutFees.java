package cc.anore;

import java.util.Collections;
import java.util.Map;

public final class PayoutFees extends Model {
    PayoutFees(Map<String, Object> raw) { super(raw); }

    public Long shopId() { return longVal("shopId"); }
    public String currency() { return str("currency"); }
    public Double thresholdRub() { return doubleVal("thresholdRub"); }
    public Double minAmountRub() { return doubleVal("minAmountRub"); }
    public Double maxAmountRub() { return doubleVal("maxAmountRub"); }
    public Map<String, Object> minAmountRubByMethod() { return map("minAmountRubByMethod"); }
    public Map<String, Object> maxAmountRubByMethod() { return map("maxAmountRubByMethod"); }
    public java.util.List<PayoutFee> methodFees() { return PayoutFee.parse(raw.get("methods")); }

    public Map<String, Object> methods() {
        return PayoutFee.byMethod(raw.get("methods"));
    }
}

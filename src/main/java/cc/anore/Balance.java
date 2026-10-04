package cc.anore;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Balance extends Model {
    Balance(Map<String, Object> raw) { super(raw); }

    public String currency() { return str("currency"); }
    public Double available() { return doubleVal("available"); }
    public Double shopLocalBalance() { return doubleVal("shopLocalBalance"); }
    public Double shopLocalAvailable() { return doubleVal("shopLocalAvailable"); }
    public Double accountAvailable() { return doubleVal("accountAvailable"); }
    public Double hold() { return doubleVal("hold"); }
    public Double frozen() { return doubleVal("frozen"); }
    public Double matured() { return doubleVal("matured"); }
    public Double paidAmount() { return doubleVal("paidAmount"); }
    public Double withdrawn() { return doubleVal("withdrawn"); }
    public Double reserved() { return doubleVal("reserved"); }
    public boolean legacyPayoutsUnassigned() { return bool("legacyPayoutsUnassigned"); }
    public Double minAmount() { return doubleVal("minAmount"); }
    public Double maxAmount() { return doubleVal("maxAmount"); }
    public Double usdtRateRub() { return doubleVal("usdtRateRub"); }
    public Double rapiraMarketUsdtRub() { return doubleVal("rapiraMarketUsdtRub"); }
    public Double rapiraMarkupPercent() { return doubleVal("rapiraMarkupPercent"); }
    public Double cbrRateRub() { return doubleVal("cbrRateRub"); }
    public String rateBasis() { return str("rateBasis"); }
    public String rapiraFixing() { return str("rapiraFixing"); }
    public Map<String, Object> minAmountByMethod() { return map("minAmountByMethod"); }
    public Map<String, Object> maxAmountByMethod() { return map("maxAmountByMethod"); }
    public List<PayoutFee> methodFees() { return PayoutFee.parse(fee().get("methods")); }

    public Map<String, Object> fee() {
        Object value = raw.get("fee");
        if (!(value instanceof Map)) return Collections.emptyMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> fee = (Map<String, Object>) value;
        return fee;
    }

    public List<Map<String, Object>> sbpBanks() {
        Object value = raw.get("sbpBanks");
        if (!(value instanceof List)) return Collections.emptyList();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> banks = (List<Map<String, Object>>) value;
        return banks;
    }
}

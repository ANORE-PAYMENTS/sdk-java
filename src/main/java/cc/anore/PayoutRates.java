package cc.anore;

import java.util.Map;

public final class PayoutRates extends Model {
    PayoutRates(Map<String, Object> raw) { super(raw); }

    public Long shopId() { return longVal("shopId"); }
    public String ratePolicy() { return str("ratePolicy"); }
    public String rateBasis() { return str("rateBasis"); }
    public String sbpRatePolicy() { return str("sbpRatePolicy"); }
    public Double rapiraUsdtRub() { return doubleVal("rapiraUsdtRub"); }
    public Double rapiraMarketUsdtRub() { return doubleVal("rapiraMarketUsdtRub"); }
    public Double rapiraMarkupPercent() { return doubleVal("rapiraMarkupPercent"); }
    public Double cbrUsdRub() { return doubleVal("cbrUsdRub"); }
    public Double rubToUsdt() { return doubleVal("rubToUsdt"); }
    public Double usdtToRub() { return doubleVal("usdtToRub"); }
    public Double rubToRubSettlement() { return doubleVal("rubToRubSettlement"); }
    public String rapiraFixing() { return str("rapiraFixing"); }
    public java.util.List<PayoutFee> methodFees() { return PayoutFee.parse(raw.get("fees")); }

    public Map<String, Object> fees() {
        return PayoutFee.byMethod(raw.get("fees"));
    }
}

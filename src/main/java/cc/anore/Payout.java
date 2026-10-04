package cc.anore;

import java.util.Map;

public final class Payout extends Model {
    Payout(Map<String, Object> raw) { super(raw); }

    public String id() { return str("id"); }
    public Long shopId() { return longVal("shopId"); }
    public boolean legacy() { return bool("legacy"); }
    public String status() { return str("status"); }
    public Double amount() { return doubleVal("amount"); }
    public String method() { return str("methodCode") != null ? str("methodCode") : str("method"); }
    public String address() { return str("address"); }
    public String bank() { return str("bank"); }
    public Double fee() { return doubleVal("fee"); }
    public Double netRub() { return doubleVal("netRub"); }
    public Double amountUsdt() { return doubleVal("amountUsdt"); }
    public Double rapiraRate() { return doubleVal("rapiraRate"); }
    public Double cbrRate() { return doubleVal("cbrRate"); }
    public Double settlementRub() { return doubleVal("settlementRub"); }
    public String quoteType() { return str("quoteType"); }
    public String ratePolicy() { return str("ratePolicy"); }
    public String rateBasis() { return str("rateBasis"); }
    public String quoteAppliedAt() { return str("quoteAppliedAt"); }
    public boolean manualCorrection() { return bool("manualCorrection"); }
    public Long statusRevision() { return longVal("statusRevision"); }
    public String externalId() { return str("externalId"); }
    public String createdAt() { return str("createdAt"); }
    public String processedAt() { return str("processedAt"); }
    public String txHash() { return str("txHash"); }
}

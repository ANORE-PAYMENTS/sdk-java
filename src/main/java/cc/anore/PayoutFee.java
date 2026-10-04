package cc.anore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PayoutFee extends Model {
    PayoutFee(Map<String, Object> raw) { super(raw); }

    public String method() { return str("method"); }
    public String label() { return str("label"); }
    public Double flatRub() { return doubleVal("flatRub"); }
    public Double percentAbove() { return doubleVal("percentAbove"); }
    public Double surchargeRub() { return doubleVal("surchargeRub"); }

    static List<PayoutFee> parse(Object value) {
        if (!(value instanceof List)) return Collections.emptyList();
        List<PayoutFee> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> row = (Map<String, Object>) item;
                result.add(new PayoutFee(row));
            }
        }
        return result;
    }

    static Map<String, Object> byMethod(Object value) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) value;
            return result;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (PayoutFee fee : parse(value)) {
            if (fee.method() != null) result.put(fee.method(), fee.raw());
        }
        return result;
    }
}

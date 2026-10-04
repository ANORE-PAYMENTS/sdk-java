package cc.anore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class PaymentList extends Model implements Iterable<Payment> {
    PaymentList(Map<String, Object> raw) { super(raw); }

    public boolean success() { return bool("success"); }
    public Long shopId() { return longVal("shopId"); }

    public List<Payment> payments() {
        Object value = raw.get("payments");
        if (!(value instanceof List)) return Collections.emptyList();
        List<Payment> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payment = (Map<String, Object>) item;
                result.add(new Payment(payment));
            }
        }
        return result;
    }

    public Long total() { return longVal("total"); }
    public Long limit() { return longVal("limit"); }
    public Long offset() { return longVal("offset"); }
    public java.util.Iterator<Payment> iterator() { return payments().iterator(); }
}

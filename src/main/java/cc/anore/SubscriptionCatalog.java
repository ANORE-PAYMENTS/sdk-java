package cc.anore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SubscriptionCatalog extends Model {
    SubscriptionCatalog(Map<String, Object> raw) { super(raw); }

    public boolean success() { return bool("success"); }
    public boolean shopIdRequired() { return bool("shopIdRequired"); }
    public String environment() { return str("environment"); }
    public List<Map<String, Object>> shops() {
        return Collections.unmodifiableList(new ArrayList<>(maps("shops")));
    }
}

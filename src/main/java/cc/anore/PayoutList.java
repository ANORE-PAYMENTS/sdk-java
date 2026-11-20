package cc.anore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class PayoutList extends Model implements Iterable<Payout> {
    PayoutList(Map<String, Object> raw) { super(raw); }

    public Long shopId() { return longVal("shopId"); }
    public Long limit() { return longVal("limit"); }
    public boolean hasMore() { return bool("hasMore"); }
    public String nextCursor() { return str("nextCursor"); }
    public List<Payout> items() {
        List<Payout> result = new ArrayList<>();
        for (Map<String, Object> row : maps("items")) result.add(new Payout(row));
        return result;
    }
    @Override public Iterator<Payout> iterator() { return items().iterator(); }
}

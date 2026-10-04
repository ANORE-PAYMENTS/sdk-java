package cc.anore;

import java.util.Collections;
import java.util.Map;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

abstract class Model {
    protected final Map<String, Object> raw;

    Model(Map<String, Object> raw) {
        this.raw = raw == null ? Collections.emptyMap() : raw;
    }

    public Map<String, Object> raw() {
        return raw;
    }

    public BigDecimal decimal(String key) {
        Object value = raw.get(key);
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number || value instanceof String) {
            try { return new BigDecimal(value.toString()); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    protected Map<String, Object> map(String key) {
        Object value = raw.get(key);
        if (!(value instanceof Map)) return Collections.emptyMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) value;
        return result;
    }

    protected List<Map<String, Object>> maps(String key) {
        Object value = raw.get(key);
        if (!(value instanceof List)) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> row = (Map<String, Object>) item;
                result.add(row);
            }
        }
        return result;
    }

    protected String str(String key) {
        Object v = raw.get(key);
        return v == null ? null : v.toString();
    }

    protected boolean bool(String key) {
        Object v = raw.get(key);
        return Boolean.TRUE.equals(v);
    }

    protected Long longVal(String key) {
        Object v = raw.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return null;
    }

    protected Double doubleVal(String key) {
        BigDecimal value = decimal(key);
        return value == null ? null : value.doubleValue();
    }
}

package cc.anore;

import java.util.Collections;
import java.util.Map;

/** Base for typed response wrappers — keeps the raw map so forward-compatible
 *  fields are never lost. Package-private. */
abstract class Model {
    protected final Map<String, Object> raw;

    Model(Map<String, Object> raw) {
        this.raw = raw == null ? Collections.emptyMap() : raw;
    }

    /** The underlying parsed JSON object (read-only view of what the API returned). */
    public Map<String, Object> raw() {
        return raw;
    }

    protected String str(String key) {
        Object v = raw.get(key);
        return v == null ? null : v.toString();
    }

    protected boolean bool(String key) {
        Object v = raw.get(key);
        return Boolean.TRUE.equals(v);
    }

    /** Numbers may arrive as Long or Double from the JSON parser. */
    protected Long longVal(String key) {
        Object v = raw.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return null;
    }

    protected Double doubleVal(String key) {
        Object v = raw.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return null;
    }
}

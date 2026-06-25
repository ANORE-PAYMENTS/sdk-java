package cc.anore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal zero-dependency JSON parser/serializer — just enough for the API:
 * objects, arrays, strings (with escapes), numbers, booleans, null. Numbers
 * become Long when integral and within range, otherwise Double. Not a general
 * library; intentionally small. Package-private.
 */
final class Json {

    private Json() {}

    // ---- parse ----

    static Object parse(String s) {
        if (s == null) return null;
        Parser p = new Parser(s);
        p.skipWs();
        Object v = p.value();
        p.skipWs();
        if (!p.atEnd()) throw new AnoreException("invalid JSON: trailing data at " + p.pos);
        return v;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseObject(String s) {
        Object v = parse(s);
        if (v instanceof Map) return (Map<String, Object>) v;
        return new LinkedHashMap<>();
    }

    private static final class Parser {
        final String src;
        int pos;

        Parser(String src) { this.src = src; }

        boolean atEnd() { return pos >= src.length(); }

        void skipWs() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        Object value() {
            skipWs();
            if (atEnd()) throw new AnoreException("invalid JSON: unexpected end");
            char c = src.charAt(pos);
            switch (c) {
                case '{': return object();
                case '[': return array();
                case '"': return string();
                case 't': case 'f': return bool();
                case 'n': literal("null"); return null;
                default: return number();
            }
        }

        Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            pos++; // {
            skipWs();
            if (peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                if (peek() != '"') throw new AnoreException("invalid JSON: expected key at " + pos);
                String key = string();
                skipWs();
                if (peek() != ':') throw new AnoreException("invalid JSON: expected ':' at " + pos);
                pos++;
                m.put(key, value());
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; break; }
                throw new AnoreException("invalid JSON: expected ',' or '}' at " + pos);
            }
            return m;
        }

        List<Object> array() {
            List<Object> a = new ArrayList<>();
            pos++; // [
            skipWs();
            if (peek() == ']') { pos++; return a; }
            while (true) {
                a.add(value());
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; break; }
                throw new AnoreException("invalid JSON: expected ',' or ']' at " + pos);
            }
            return a;
        }

        String string() {
            StringBuilder sb = new StringBuilder();
            pos++; // opening "
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = src.charAt(pos++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default: throw new AnoreException("invalid JSON: bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new AnoreException("invalid JSON: unterminated string");
        }

        Object number() {
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') pos++;
                else break;
            }
            String num = src.substring(start, pos);
            if (num.isEmpty()) throw new AnoreException("invalid JSON: number expected at " + start);
            if (num.indexOf('.') < 0 && num.indexOf('e') < 0 && num.indexOf('E') < 0) {
                try {
                    return Long.parseLong(num);
                } catch (NumberFormatException ignore) {
                    // falls through to double for out-of-range integers
                }
            }
            return Double.parseDouble(num);
        }

        boolean bool() {
            if (src.charAt(pos) == 't') { literal("true"); return true; }
            literal("false");
            return false;
        }

        void literal(String lit) {
            if (!src.regionMatches(pos, lit, 0, lit.length())) {
                throw new AnoreException("invalid JSON: expected '" + lit + "' at " + pos);
            }
            pos += lit.length();
        }

        char peek() {
            if (atEnd()) throw new AnoreException("invalid JSON: unexpected end");
            return src.charAt(pos);
        }
    }

    // ---- serialize ----

    static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            writeString(sb, (String) v);
        } else if (v instanceof Boolean || v instanceof Number) {
            sb.append(v.toString());
        } else if (v instanceof Map) {
            writeObject(sb, (Map<String, Object>) v);
        } else if (v instanceof Iterable) {
            writeArray(sb, (Iterable<Object>) v);
        } else {
            writeString(sb, v.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<String, Object> m) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, e.getKey());
            sb.append(':');
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Iterable<Object> it) {
        sb.append('[');
        boolean first = true;
        for (Object v : it) {
            if (!first) sb.append(',');
            first = false;
            writeValue(sb, v);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }
}

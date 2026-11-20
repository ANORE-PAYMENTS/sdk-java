package cc.anore;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class Validation {
    private static final String MAX_INT64 = "9223372036854775807";
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
            Pattern.CASE_INSENSITIVE);

    private Validation() {}

    static void telegramId(String value, String label) {
        if (value == null || value.isEmpty() || value.length() > 19 || value.charAt(0) == '0') {
            throw new IllegalArgumentException(label + " must be a positive signed-64 canonical decimal string");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException(label + " must be a positive signed-64 canonical decimal string");
            }
        }
        if (value.length() == 19 && value.compareTo(MAX_INT64) > 0) {
            throw new IllegalArgumentException(label + " must be a positive signed-64 canonical decimal string");
        }
    }

    static String telegramUsername(String value, String label) {
        if (value == null) throw new IllegalArgumentException(label + " must be a Telegram username");
        String username = value.trim();
        if (username.startsWith("@")) username = username.substring(1);
        username = username.toLowerCase(Locale.ROOT);
        if (!username.matches("[A-Za-z0-9_]{5,32}")) {
            throw new IllegalArgumentException(label + " must be a Telegram username");
        }
        return username;
    }

    static String email(String value, String label, int maxLength) {
        if (value == null) throw invalidIdentity(label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > maxLength || containsControl(normalized)
                || !EMAIL.matcher(normalized).matches()) {
            throw invalidIdentity(label);
        }
        return normalized;
    }

    static String identityValue(String type, String value, String label) {
        if (UserIdentity.EMAIL.equals(type)) return email(value, label, 190);
        if (value == null) throw invalidIdentity(label);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        if (UserIdentity.USERNAME.equals(type)) normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 190 || containsControl(normalized)) {
            throw invalidIdentity(label);
        }
        return normalized;
    }

    private static boolean containsControl(String value) {
        return value.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                || Character.getType(codePoint) == Character.FORMAT);
    }

    private static IllegalArgumentException invalidIdentity(String label) {
        return new IllegalArgumentException(label + " must contain 1 to 190 non-control characters");
    }
}

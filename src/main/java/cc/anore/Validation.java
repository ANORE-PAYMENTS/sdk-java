package cc.anore;

final class Validation {
    private static final String MAX_INT64 = "9223372036854775807";

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

    static void telegramUsername(String value, String label) {
        if (value == null || !value.matches("@?[A-Za-z0-9_]{5,32}")) {
            throw new IllegalArgumentException(label + " must be a Telegram username");
        }
    }
}

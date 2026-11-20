package cc.anore;

import java.util.LinkedHashMap;
import java.util.Map;

/** Validated identity metadata accepted only by payment creation. */
public final class UserIdentity {
    public static final String TELEGRAM_ID = "telegram_id";
    public static final String EMAIL = "email";
    public static final String USERNAME = "username";
    public static final String EXTERNAL_ID = "external_id";

    private final String type;
    private final String value;
    private String telegramUsername;

    private UserIdentity(String type, String value) {
        if (!TELEGRAM_ID.equals(type) && !EMAIL.equals(type)
                && !USERNAME.equals(type) && !EXTERNAL_ID.equals(type)) {
            throw new IllegalArgumentException("userIdentity: unsupported type");
        }
        if (TELEGRAM_ID.equals(type)) {
            Validation.telegramId(value, "userIdentity.value");
            this.value = value;
        } else {
            this.value = Validation.identityValue(type, value, "userIdentity.value");
        }
        this.type = type;
    }

    public static UserIdentity of(String type, String value) { return new UserIdentity(type, value); }
    public static UserIdentity telegramId(String value) { return of(TELEGRAM_ID, value); }
    public static UserIdentity email(String value) { return of(EMAIL, value); }
    public static UserIdentity username(String value) { return of(USERNAME, value); }
    public static UserIdentity externalId(String value) { return of(EXTERNAL_ID, value); }

    public UserIdentity telegramUsername(String value) {
        if (!TELEGRAM_ID.equals(type)) {
            throw new IllegalArgumentException("userIdentity.telegramUsername is allowed only with telegram_id");
        }
        this.telegramUsername = Validation.telegramUsername(value, "userIdentity.telegramUsername");
        return this;
    }

    Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("value", value);
        if (telegramUsername != null) body.put("telegram_username", telegramUsername);
        return body;
    }
}

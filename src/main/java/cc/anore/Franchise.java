package cc.anore;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Validated franchise routing metadata accepted only by payment creation. */
public final class Franchise {
    public static final String TELEGRAM_BOT = "telegram_bot";
    public static final String WEBSITE = "website";

    private final String type;
    private final String address;
    private final String ownerTelegramId;
    private String ownerTelegramUsername;

    private Franchise(String type, String address, String ownerTelegramId) {
        if (!TELEGRAM_BOT.equals(type) && !WEBSITE.equals(type)) {
            throw new IllegalArgumentException("franchise: unsupported type");
        }
        Validation.telegramId(ownerTelegramId, "franchise.ownerTelegramId");
        if (TELEGRAM_BOT.equals(type)) {
            Validation.telegramUsername(address, "franchise.address");
        } else {
            validateWebsite(address);
        }
        this.type = type;
        this.address = address;
        this.ownerTelegramId = ownerTelegramId;
    }

    public static Franchise of(String type, String address, String ownerTelegramId) {
        return new Franchise(type, address, ownerTelegramId);
    }
    public static Franchise telegramBot(String address, String ownerTelegramId) {
        return of(TELEGRAM_BOT, address, ownerTelegramId);
    }
    public static Franchise website(String address, String ownerTelegramId) {
        return of(WEBSITE, address, ownerTelegramId);
    }

    public Franchise ownerTelegramUsername(String value) {
        Validation.telegramUsername(value, "franchise.ownerTelegramUsername");
        this.ownerTelegramUsername = value;
        return this;
    }

    Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("address", address);
        body.put("owner_telegram_id", ownerTelegramId);
        if (ownerTelegramUsername != null) body.put("owner_telegram_username", ownerTelegramUsername);
        return body;
    }

    private static void validateWebsite(String value) {
        try {
            URI uri = URI.create(value);
            String path = uri.getRawPath();
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getPort() != -1
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !(path == null || path.isEmpty() || "/".equals(path))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("franchise.address must be a canonical HTTPS origin");
        }
    }
}

package cc.anore;

import java.net.IDN;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
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
            this.address = Validation.telegramUsername(address, "franchise.address");
        } else {
            this.address = canonicalWebsite(address);
        }
        this.type = type;
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
        this.ownerTelegramUsername = Validation.telegramUsername(value, "franchise.ownerTelegramUsername");
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

    private static String canonicalWebsite(String value) {
        try {
            URI uri = URI.create(value);
            String path = uri.getRawPath();
            String authority = uri.getRawAuthority();
            String uriHost = uri.getHost();
            boolean ipv6 = authority != null && authority.startsWith("[") && authority.endsWith("]")
                    && uriHost != null && uriHost.indexOf(':') >= 0;
            if (!"https".equalsIgnoreCase(uri.getScheme()) || authority == null
                    || uri.getUserInfo() != null || uri.getPort() != -1
                    || authority.indexOf('@') >= 0 || (!ipv6 && authority.indexOf(':') >= 0)
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !(path == null || path.isEmpty() || "/".equals(path))) {
                throw new IllegalArgumentException();
            }
            if (ipv6) return "https://" + uriHost.toLowerCase(Locale.ROOT);
            String host = IDN.toASCII(authority).toLowerCase(Locale.ROOT);
            if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
            if (host.isEmpty()) throw new IllegalArgumentException();
            return "https://" + host;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("franchise.address must be a canonical HTTPS origin");
        }
    }
}

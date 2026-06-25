package cc.anore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Webhook signature verification and payload parsing. */
public final class Webhooks {

    private Webhooks() {}

    /**
     * Verify an incoming webhook signature.
     *
     * @param rawBody   the RAW request body bytes (never the re-serialized JSON)
     * @param signature the Anore-Signature header value
     * @param secret    signing secret from the dashboard (Webhooks tab)
     * @return true if the signature matches
     */
    public static boolean verify(byte[] rawBody, String signature, String secret) {
        if (rawBody == null || rawBody.length == 0 || signature == null || secret == null || secret.isEmpty()) {
            return false;
        }
        String expected = hmacHex(secret, rawBody);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b); // constant-time
    }

    /** String overload — body is treated as UTF-8. */
    public static boolean verify(String rawBody, String signature, String secret) {
        if (rawBody == null) return false;
        return verify(rawBody.getBytes(StandardCharsets.UTF_8), signature, secret);
    }

    /**
     * Verify the signature and return the parsed event.
     *
     * @throws SignatureException if the signature does not match
     */
    public static WebhookEvent parse(byte[] rawBody, String signature, String secret) {
        if (!verify(rawBody, signature, secret)) {
            throw new SignatureException("webhook signature verification failed");
        }
        String json = new String(rawBody, StandardCharsets.UTF_8);
        return new WebhookEvent(Json.parseObject(json));
    }

    /** String overload. */
    public static WebhookEvent parse(String rawBody, String signature, String secret) {
        return parse(rawBody == null ? new byte[0] : rawBody.getBytes(StandardCharsets.UTF_8), signature, secret);
    }

    private static String hmacHex(String secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] d = mac.doFinal(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new AnoreException("HMAC failure", e);
        }
    }
}

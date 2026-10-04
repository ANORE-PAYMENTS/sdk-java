package cc.anore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Webhooks {

    private Webhooks() {}

    public static boolean verify(byte[] rawBody, String signature, String secret) {
        if (rawBody == null || rawBody.length == 0 || signature == null || secret == null || secret.isEmpty()) {
            return false;
        }
        String normalized = signature.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-f0-9]{64}")) return false;
        String expected = hmacHex(secret, rawBody);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = normalized.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    public static boolean verify(String rawBody, String signature, String secret) {
        if (rawBody == null) return false;
        return verify(rawBody.getBytes(StandardCharsets.UTF_8), signature, secret);
    }

    public static WebhookEvent parse(byte[] rawBody, String signature, String secret) {
        if (!verify(rawBody, signature, secret)) {
            throw new SignatureException("webhook signature verification failed");
        }
        String json = new String(rawBody, StandardCharsets.UTF_8);
        return new WebhookEvent(Json.parseObject(json));
    }

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

package cc.anore;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Client for the anore payments API. Zero dependencies — JDK 11+ {@link HttpClient}.
 *
 * <pre>{@code
 * AnoreClient anore = AnoreClient.builder()
 *         .apiKey("an_live_xxxxxxxxxxxxxxxx")
 *         .build();
 *
 * Payment p = anore.createPayment(
 *         CreatePaymentParams.of(1500, "Подписка Pro").orderId("order_42").shopId(1));
 * System.out.println(p.paymentUrl());
 *
 * Payment s = anore.getPayment(p.id());
 * System.out.println(s.status() + " " + s.paid());
 * }</pre>
 */
public final class AnoreClient {

    public static final String DEFAULT_BASE_URL = "https://api.anore.cc/v1";
    private static final String USER_AGENT = "anore-java/1.0.0";

    private final String baseUrl;
    private final String apiKey;
    private final String secret;
    private final int maxRetries;
    private final Duration timeout;
    private final HttpClient http;

    private AnoreClient(Builder b) {
        if (b.apiKey == null || b.apiKey.isEmpty()) {
            throw new IllegalArgumentException("AnoreClient: apiKey is required");
        }
        this.baseUrl = b.baseUrl.replaceAll("/+$", "");
        this.apiKey = b.apiKey;
        this.secret = b.secret;
        this.maxRetries = b.maxRetries;
        this.timeout = b.timeout;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Create a payment / invoice (POST /payments). Use {@link Payment#paymentUrl()} to redirect the customer. */
    public Payment createPayment(CreatePaymentParams params) {
        if (params == null) throw new IllegalArgumentException("createPayment: params required");
        Map<String, Object> body = params.toBody();
        return new Payment(request("POST", "/payments", body));
    }

    /** Fetch payment status (GET /payments/{id}). {@link Payment#status()} is "new" | "paid" | "expired". */
    public Payment getPayment(String id) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("getPayment: id is required");
        String enc = URLEncoder.encode(id, StandardCharsets.UTF_8);
        return new Payment(request("GET", "/payments/" + enc, null));
    }

    private Map<String, Object> request(String method, String path, Map<String, Object> body) {
        String payload = body == null ? null : Json.write(body);

        long backoff = 500;
        ApiConnectionException lastConn = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json");

            if (payload != null) {
                rb.header("Content-Type", "application/json");
                if (secret != null && !secret.isEmpty()) {
                    rb.header("Anore-Signature", hmacHex(secret, payload));
                }
                rb.method(method, HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
            } else {
                rb.method(method, HttpRequest.BodyPublishers.noBody());
            }

            try {
                HttpResponse<String> res = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
                int code = res.statusCode();
                Map<String, Object> data = Json.parseObject(res.body());
                if (code >= 200 && code < 300) {
                    return data;
                }
                // retry transient 5xx; surface 4xx immediately
                if (code >= 500 && attempt < maxRetries) {
                    sleep(backoff);
                    backoff = Math.min(backoff * 2, 4000);
                    continue;
                }
                String requestId = res.headers().firstValue("x-request-id").orElse(null);
                String message = message(data, "request failed");
                throw ApiException.forStatus(code, message, requestId);
            } catch (java.io.IOException | InterruptedException e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                lastConn = new ApiConnectionException("could not reach anore API: " + e.getMessage(), e);
                if (attempt < maxRetries) {
                    sleep(backoff);
                    backoff = Math.min(backoff * 2, 4000);
                    continue;
                }
                throw lastConn;
            }
        }
        throw lastConn != null ? lastConn : new ApiConnectionException("could not reach anore API", null);
    }

    private static String message(Map<String, Object> data, String fallback) {
        Object m = data.get("message");
        if (m == null) m = data.get("error");
        return m == null ? fallback : m.toString();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static String hmacHex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] d = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new AnoreException("HMAC failure", e);
        }
    }

    /** Fluent builder for {@link AnoreClient}. */
    public static final class Builder {
        private String baseUrl = DEFAULT_BASE_URL;
        private String apiKey;
        private String secret;
        private int maxRetries = 2;
        private Duration timeout = Duration.ofSeconds(30);

        /** Key from the dashboard (an_live_… / an_test_…). Required. */
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }

        /** Optional signing secret — signs outgoing requests with Anore-Signature. */
        public Builder secret(String secret) { this.secret = secret; return this; }

        /** API base, defaults to https://api.anore.cc/v1. */
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }

        /** Retries on network errors / 5xx (default 2). */
        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }

        /** Per-request timeout (default 30s). */
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }

        public AnoreClient build() { return new AnoreClient(this); }
    }

    /** Parameters for {@link #createPayment}. */
    public static final class CreatePaymentParams {
        private final double amount;
        private final String description;
        private String orderId;
        private Long shopId;

        private CreatePaymentParams(double amount, String description) {
            this.amount = amount;
            this.description = description;
        }

        /** amount in rubles (> 0), description shown to the customer. */
        public static CreatePaymentParams of(double amount, String description) {
            if (!(amount > 0)) throw new IllegalArgumentException("createPayment: amount must be > 0");
            if (description == null || description.isEmpty()) {
                throw new IllegalArgumentException("createPayment: description is required");
            }
            return new CreatePaymentParams(amount, description);
        }

        /** Your own order reference. */
        public CreatePaymentParams orderId(String orderId) { this.orderId = orderId; return this; }

        /** Shop id — required for account-level keys (an_*). */
        public CreatePaymentParams shopId(long shopId) { this.shopId = shopId; return this; }

        Map<String, Object> toBody() {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("amount", amount);
            b.put("description", description);
            if (orderId != null) b.put("orderId", orderId);
            if (shopId != null) b.put("shopId", shopId);
            return b;
        }
    }
}

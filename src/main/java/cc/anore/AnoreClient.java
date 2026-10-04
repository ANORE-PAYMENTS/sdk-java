package cc.anore;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AnoreClient {

    public static final String DEFAULT_BASE_URL = "https://api.anore.cc/api/v1";
    private static final String USER_AGENT = "anore-java/1.2.0";

    private final String baseUrl;
    private final String apiKey;
    private final String secret;
    private final int maxRetries;
    private final Duration timeout;
    private final HttpClient http;

    private AnoreClient(Builder b) {
        if (b.apiKey == null || b.apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("AnoreClient: apiKey is required");
        }
        if (b.maxRetries < 0) throw new IllegalArgumentException("maxRetries must be >= 0");
        if (b.timeout == null || b.timeout.isZero() || b.timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.baseUrl = normalizeBaseUrl(b.baseUrl);
        this.apiKey = b.apiKey.trim();
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

    public Payment createPayment(CreatePaymentParams params) {
        if (params == null) throw new IllegalArgumentException("createPayment: params required");
        Map<String, Object> body = params.toBody();
        return new Payment(request("POST", "/payments", body));
    }

    public Payment getPayment(String id) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("getPayment: id is required");
        String enc = encode(id);
        return new Payment(request("GET", "/payments/" + enc, null));
    }

    public PaymentList listPayments(ListPaymentsParams params) {
        ListPaymentsParams p = params == null ? new ListPaymentsParams() : params;
        return new PaymentList(request("GET", "/payments?" + p.toQuery(), null));
    }

    public Balance getBalance(Long shopId) {
        return new Balance(request("GET", shopPath("/balance", shopId), null));
    }

    public Balance getBalance() { return getBalance(null); }

    public PayoutFees getPayoutFees(Long shopId) {
        return new PayoutFees(request("GET", shopPath("/payouts/fees", shopId), null));
    }

    public PayoutFees getPayoutFees() { return getPayoutFees(null); }

    public PayoutRates getPayoutRates(Long shopId) {
        return new PayoutRates(request("GET", shopPath("/payouts/rates", shopId), null));
    }

    public PayoutRates getPayoutRates() { return getPayoutRates(null); }

    public Payout createPayout(CreatePayoutParams params) {
        if (params == null) throw new IllegalArgumentException("createPayout: params required");
        return new Payout(request("POST", "/payouts", params.toBody()));
    }

    public Payout getPayout(String id) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("getPayout: id is required");
        return new Payout(request("GET", "/payouts/" + encode(id), null));
    }

    private static String shopPath(String path, Long shopId) {
        if (shopId != null && shopId <= 0) throw new IllegalArgumentException("shopId must be > 0");
        return shopId == null ? path : path + "?shopId=" + encode(shopId.toString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("baseUrl is required");
        String value = baseUrl.trim().replaceAll("/+$", "");
        URI uri = URI.create(value);
        if ((!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("baseUrl must be an HTTP(S) API URL without query, fragment or credentials");
        }
        if (value.endsWith("/api/v1") || value.endsWith("/v1")) return value;
        return value + "/api/v1";
    }

    private Map<String, Object> request(String method, String path, Map<String, Object> body) {
        String payload = body == null ? null : Json.write(body);
        int retries = "GET".equals(method) ? maxRetries : 0;

        long backoff = 500;
        ApiConnectionException lastConn = null;
        for (int attempt = 0; attempt <= retries; attempt++) {
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json");

            if (payload != null) {
                rb.header("Content-Type", "application/json");
                if (secret != null && !secret.isEmpty()) {
                    String signature = hmacHex(secret, payload);
                    rb.header("X-ZPay-Signature", signature);
                    rb.header("Anore-Signature", signature);
                }
                rb.method(method, HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
            } else {
                rb.method(method, HttpRequest.BodyPublishers.noBody());
            }

            try {
                HttpResponse<String> res = http.send(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int code = res.statusCode();
                if (code >= 200 && code < 300) {
                    try {
                        return Json.parseObject(res.body());
                    } catch (AnoreException error) {
                        throw new ApiException("API returned invalid JSON", code,
                                res.headers().firstValue("x-request-id").orElse(null));
                    }
                }

                if ((code >= 500 || code == 429) && attempt < retries) {
                    sleep(backoff);
                    backoff = Math.min(backoff * 2, 4000);
                    continue;
                }
                String requestId = res.headers().firstValue("x-request-id").orElse(null);
                Map<String, Object> data;
                try {
                    data = Json.parseObject(res.body());
                } catch (AnoreException ignored) {
                    data = java.util.Collections.emptyMap();
                }
                String message = message(data, "API returned HTTP " + code);
                throw ApiException.forStatus(code, message, requestId);
            } catch (java.io.IOException | InterruptedException e) {
                lastConn = new ApiConnectionException("could not reach anore API: " + e.getMessage(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw lastConn;
                }
                if (attempt < retries) {
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
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiConnectionException("API request interrupted", e);
        }
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

    public static final class Builder {
        private String baseUrl = DEFAULT_BASE_URL;
        private String apiKey;
        private String secret;
        private int maxRetries = 2;
        private Duration timeout = Duration.ofSeconds(30);

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }

        public Builder secret(String secret) { this.secret = secret; return this; }

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }

        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }

        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }

        public AnoreClient build() { return new AnoreClient(this); }
    }

    public static final class CreatePaymentParams {
        private final BigDecimal amount;
        private final String description;
        private String orderId;
        private Long shopId;
        private String currency;
        private List<String> methods;
        private String getbackUrl;
        private String successUrl;
        private String failUrl;
        private String email;
        private String callbackUrl;

        private CreatePaymentParams(BigDecimal amount, String description) {
            this.amount = amount;
            this.description = description;
        }

        public static CreatePaymentParams of(double amount, String description) {
            if (!Double.isFinite(amount)) throw new IllegalArgumentException("createPayment: amount must be finite");
            return of(BigDecimal.valueOf(amount), description);
        }

        public static CreatePaymentParams of(BigDecimal amount, String description) {
            if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("createPayment: amount must be > 0");
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("createPayment: description is required");
            }
            return new CreatePaymentParams(amount, description);
        }

        public CreatePaymentParams orderId(String orderId) { this.orderId = orderId; return this; }

        public CreatePaymentParams shopId(long shopId) {
            if (shopId <= 0) throw new IllegalArgumentException("shopId must be > 0");
            this.shopId = shopId; return this;
        }

        public CreatePaymentParams currency(String currency) { this.currency = currency; return this; }

        public CreatePaymentParams methods(List<String> methods) { this.methods = methods; return this; }

        public CreatePaymentParams getbackUrl(String getbackUrl) { this.getbackUrl = getbackUrl; return this; }

        public CreatePaymentParams successUrl(String successUrl) { this.successUrl = successUrl; return this; }

        public CreatePaymentParams failUrl(String failUrl) { this.failUrl = failUrl; return this; }

        public CreatePaymentParams email(String email) { this.email = email; return this; }

        public CreatePaymentParams callbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; return this; }

        Map<String, Object> toBody() {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("amount", amount);
            b.put("description", description);
            if (orderId != null) b.put("orderId", orderId);
            if (shopId != null) b.put("shopId", shopId);
            if (currency != null) b.put("currency", currency);
            if (methods != null) b.put("methods", methods);
            if (getbackUrl != null) b.put("getbackurl", getbackUrl);
            if (successUrl != null) b.put("successurl", successUrl);
            if (failUrl != null) b.put("failurl", failUrl);
            if (email != null) b.put("email", email);
            if (callbackUrl != null) b.put("callbackUrl", callbackUrl);
            return b;
        }
    }

    public static final class ListPaymentsParams {
        private Long shopId;
        private String status;
        private String from;
        private String to;
        private int limit = 50;
        private int offset;

        public ListPaymentsParams shopId(long shopId) {
            if (shopId <= 0) throw new IllegalArgumentException("shopId must be > 0");
            this.shopId = shopId; return this;
        }
        public ListPaymentsParams status(String status) { this.status = status; return this; }
        public ListPaymentsParams from(String from) { this.from = from; return this; }
        public ListPaymentsParams to(String to) { this.to = to; return this; }
        public ListPaymentsParams limit(int limit) {
            if (limit < 1 || limit > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
            this.limit = limit; return this;
        }
        public ListPaymentsParams offset(int offset) {
            if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");
            this.offset = offset; return this;
        }

        String toQuery() {
            Map<String, Object> query = new LinkedHashMap<>();
            if (shopId != null) query.put("shopId", shopId);
            if (status != null) query.put("status", status);
            if (from != null) query.put("from", from);
            if (to != null) query.put("to", to);
            query.put("limit", limit);
            query.put("offset", offset);
            StringBuilder out = new StringBuilder();
            for (Map.Entry<String, Object> entry : query.entrySet()) {
                if (out.length() > 0) out.append('&');
                out.append(encode(entry.getKey())).append('=').append(encode(entry.getValue().toString()));
            }
            return out.toString();
        }
    }

    public static final class CreatePayoutParams {
        private final BigDecimal amount;
        private final String method;
        private final String address;
        private Long shopId;
        private String bank;
        private String externalId;

        private CreatePayoutParams(BigDecimal amount, String method, String address) {
            this.amount = amount;
            this.method = method;
            this.address = address;
        }

        public static CreatePayoutParams of(double amount, String method, String address) {
            if (!Double.isFinite(amount)) throw new IllegalArgumentException("createPayout: amount must be finite");
            return of(BigDecimal.valueOf(amount), method, address);
        }

        public static CreatePayoutParams of(BigDecimal amount, String method, String address) {
            if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("createPayout: amount must be a positive whole RUB amount");
            }
            if (method == null || method.trim().isEmpty()) throw new IllegalArgumentException("createPayout: method is required");
            if (address == null || address.trim().isEmpty()) throw new IllegalArgumentException("createPayout: address is required");
            return new CreatePayoutParams(amount, method, address);
        }

        public CreatePayoutParams shopId(long shopId) {
            if (shopId <= 0) throw new IllegalArgumentException("shopId must be > 0");
            this.shopId = shopId; return this;
        }
        public CreatePayoutParams bank(String bank) { this.bank = bank; return this; }
        public CreatePayoutParams externalId(String externalId) { this.externalId = externalId; return this; }

        Map<String, Object> toBody() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", amount);
            body.put("method", method);
            body.put("address", address);
            if (shopId != null) body.put("shopId", shopId);
            if (bank != null) body.put("bank", bank);
            if (externalId != null) body.put("externalId", externalId);
            return body;
        }
    }
}

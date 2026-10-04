package cc.anore;

import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class ContractTest {
    private static int passed;
    private static HttpServer server;
    private static String origin;
    private static volatile String body;
    private static volatile String query;
    private static volatile String path;
    private static volatile String authorization;
    private static volatile String signature;
    private static volatile String userAgent;
    private static volatile String overrideBody;
    private static volatile int overrideStatus;
    private static volatile int failuresLeft;
    private static final AtomicInteger requests = new AtomicInteger();
    private static final String PAYMENT = "{\"success\":true,\"id\":\"b12726e5-d84a-4bd7-b3c7-0a147d223928\",\"amount\":13.25,\"rubAmount\":1141.4875,\"baseAmount\":1141.4875,\"currency\":\"usd\",\"currencyRate\":86.15,\"status\":\"new\",\"paymentUrl\":\"https://pay.anore.cc/test\",\"expiresIn\":14400}";
    private static final String FEES = "[{\"method\":\"usdt_erc20\",\"label\":\"USDT ERC20\",\"flatRub\":240,\"percentAbove\":1,\"surchargeRub\":0}]";

    public static void main(String[] args) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            query = exchange.getRequestURI().getRawQuery();
            path = exchange.getRequestURI().getRawPath();
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            signature = exchange.getRequestHeaders().getFirst("X-ZPay-Signature");
            userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            int status = overrideStatus == 0 ? 200 : overrideStatus;
            String response = overrideBody == null ? fixture(path) : overrideBody;
            if (failuresLeft > 0) {
                failuresLeft--;
            } else if (overrideStatus != 0 && overrideBody == null) {
                status = 200;
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().add("X-Request-Id", "fixture-request");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        origin = "http://127.0.0.1:" + server.getAddress().getPort();
        try {
            run("canonical and legacy API bases", () -> {
                equal(AnoreClient.DEFAULT_BASE_URL, "https://api.anore.cc/api/v1");
                for (String base : Arrays.asList(origin, origin + "/", origin + "/api/v1", origin + "/api/v1/")) {
                    client(base).getPayment("payment");
                    equal(path, "/api/v1/payments/payment");
                }
                client(origin + "/v1/").getPayment("payment");
                equal(path, "/v1/payments/payment");
            });
            run("signed UTF-8 payment creation with customer email and callback", () -> {
                Payment value = client(origin).createPayment(AnoreClient.CreatePaymentParams.of(new BigDecimal("13.25"), "Заказ 🛒")
                        .currency("usd").email("customer@example.test").callbackUrl("https://billing.example.test/events")
                        .orderId("order-42").shopId(7).methods(Arrays.asList("sbp", "card", "crypto-old"))
                        .getbackUrl("https://example.test/back").successUrl("https://example.test/ok").failUrl("https://example.test/fail"));
                Map<String, Object> payload = Json.parseObject(body);
                equal(payload.get("email"), "customer@example.test");
                equal(payload.get("callbackUrl"), "https://billing.example.test/events");
                equal(payload.get("amount"), new BigDecimal("13.25"));
                equal(payload.get("description"), "Заказ 🛒");
                equal(payload.get("shopId"), 7L);
                equal(payload.get("methods"), Arrays.asList("sbp", "card", "crypto-old"));
                equal(payload.get("getbackurl"), "https://example.test/back");
                equal(payload.get("successurl"), "https://example.test/ok");
                equal(payload.get("failurl"), "https://example.test/fail");
                equal(signature, AnoreClient.hmacHex("api-secret", body));
                equal(authorization, "Bearer fixture-key");
                equal(userAgent, "anore-java/1.2.0");
                equal(value.decimal("amount"), new BigDecimal("13.25"));
                equal(value.decimal("rubAmount"), new BigDecimal("1141.4875"));
                equal(value.currency(), "usd");
            });
            run("payment pagination and written-off status filter", () -> {
                PaymentList page = client(origin).listPayments(new AnoreClient.ListPaymentsParams()
                        .shopId(7).status("written_off").from("2026-10-01").to("2026-10-03").limit(20).offset(40));
                equal(query, "shopId=7&status=written_off&from=2026-10-01&to=2026-10-03&limit=20&offset=40");
                equal(page.total(), 45L);
                equal(page.limit(), 20L);
                equal(page.offset(), 40L);
                equal(page.payments().get(0).status(), "written_off");
                int count = 0;
                for (Payment ignored : page) count++;
                equal(count, 1);
            });
            run("shop-scoped balance and method-specific bounds", () -> {
                Balance balance = client(origin).getBalance();
                equal(query, null);
                equal(balance.available(), 9000.25);
                equal(balance.shopLocalBalance(), 9100.25);
                equal(balance.rateBasis(), "fixed_0900_msk");
                equal(balance.minAmountByMethod().get("card_rub"), 3000L);
                equal(balance.maxAmountByMethod().get("usdt_erc20"), 1500000L);
                equal(balance.methodFees().get(0).flatRub(), 240.0);
                client(origin).getBalance(7L);
                equal(query, "shopId=7");
            });
            run("fees array and rates decode without losing fee methods", () -> {
                PayoutFees fees = client(origin).getPayoutFees(7L);
                equal(fees.methodFees().get(0).method(), "usdt_erc20");
                equal(fees.methodFees().get(0).percentAbove(), 1.0);
                check(fees.methods().containsKey("usdt_erc20"));
                equal(fees.maxAmountRubByMethod().get("usdt_erc20"), 1500000L);
                PayoutRates rates = client(origin).getPayoutRates();
                equal(rates.ratePolicy(), "approval");
                equal(rates.rateBasis(), "fixed_0900_msk");
                equal(rates.sbpRatePolicy(), "execution");
                equal(rates.rapiraFixing(), "09:00 Europe/Moscow");
                equal(rates.methodFees().get(0).flatRub(), 240.0);
                check(rates.fees().containsKey("usdt_erc20"));
            });
            run("all current payout methods including ERC20", () -> {
                for (String method : Arrays.asList("card", "sbp", "usdt_ton", "usdt_bep20", "usdt_trc20", "usdt_erc20")) {
                    Payout payout = client(origin).createPayout(AnoreClient.CreatePayoutParams.of(5000, method, "fixture-destination")
                            .shopId(7).bank("fixture-bank").externalId("external-42"));
                    Map<String, Object> payload = Json.parseObject(body);
                    equal(((Number) payload.get("amount")).intValue(), 5000);
                    equal(payload.get("method"), method);
                    equal(payload.get("address"), "fixture-destination");
                    equal(payload.get("bank"), "fixture-bank");
                    equal(payload.get("externalId"), "external-42");
                    equal(signature, AnoreClient.hmacHex("api-secret", body));
                    equal(payout.id(), "WD-509");
                    equal(payout.quoteType(), "estimate");
                    equal(payout.ratePolicy(), "approval");
                    equal(payout.rateBasis(), "fixed_0900_msk");
                }
            });
            run("stored payout quote and manual corrections", () -> {
                Payout payout = client(origin).getPayout("WD-509");
                equal(path, "/api/v1/payouts/WD-509");
                equal(payout.method(), "usdt_erc20");
                equal(payout.status(), "rejected");
                check(payout.manualCorrection());
                equal(payout.statusRevision(), 2L);
                equal(payout.quoteAppliedAt(), "2026-10-03T09:00:00Z");
                equal(payout.amountUsdt(), 54.9);
            });
            run("POST server failures are never automatically retried", () -> {
                overrideStatus = 502;
                overrideBody = "<html>upstream unavailable</html>";
                ServerException error = expect(ServerException.class, () -> client(origin).createPayment(AnoreClient.CreatePaymentParams.of(100, "test")));
                equal(requests.get(), 1);
                equal(error.getStatus(), 502);
                equal(error.getRequestId(), "fixture-request");
            });
            run("POST rate limiting is never automatically retried", () -> {
                overrideStatus = 429;
                overrideBody = "{\"message\":\"slow down\"}";
                ApiException error = expect(ApiException.class, () -> client(origin).createPayout(AnoreClient.CreatePayoutParams.of(1000, "usdt_ton", "fixture")));
                equal(requests.get(), 1);
                equal(error.getStatus(), 429);
            });
            run("GET server errors can be retried", () -> {
                overrideStatus = 503;
                failuresLeft = 1;
                client(origin).getPayment("payment");
                equal(requests.get(), 2);
            });
            run("GET rate limiting can be retried", () -> {
                overrideStatus = 429;
                failuresLeft = 1;
                client(origin).getPayment("payment");
                equal(requests.get(), 2);
            });
            run("non-JSON HTTP errors keep status and request ID", () -> {
                overrideStatus = 404;
                overrideBody = "missing";
                NotFoundException error = expect(NotFoundException.class, () -> client(origin).getPayment("missing"));
                equal(error.getStatus(), 404);
                equal(error.getRequestId(), "fixture-request");
                equal(requests.get(), 1);
            });
            run("non-JSON successful response is rejected", () -> {
                overrideBody = "<html>login</html>";
                ApiException error = expect(ApiException.class, () -> client(origin).getPayment("payment"));
                equal(error.getStatus(), 200);
                equal(requests.get(), 1);
            });
            run("HTTP error classes", () -> {
                int[] statuses = {400, 401, 403, 404, 409, 500};
                Class<?>[] classes = {ValidationException.class, AuthenticationException.class, ForbiddenException.class,
                        NotFoundException.class, ApiException.class, ServerException.class};
                for (int i = 0; i < statuses.length; i++) {
                    overrideStatus = statuses[i];
                    overrideBody = "{\"message\":\"fixture error\"}";
                    ApiException error = expect(ApiException.class, () -> client(origin, 0).getPayment("payment"));
                    check(classes[i].isInstance(error));
                    equal(error.getMessage(), "fixture error");
                }
            });
            run("raw-byte webhook signature before parsing", () -> {
                byte[] raw = "{ \"event\":\"payment.succeeded\",\"amount\":13.25,\"rubAmount\":1141.4875,\"description\":\"Заказ 🛒\",\"test\":true }".getBytes(StandardCharsets.UTF_8);
                String sig = AnoreClient.hmacHex("webhook-secret", new String(raw, StandardCharsets.UTF_8));
                check(Webhooks.verify(raw, sig.toUpperCase(java.util.Locale.ROOT), "webhook-secret"));
                WebhookEvent event = Webhooks.parse(raw, sig, "webhook-secret");
                check(event.isSucceeded());
                check(event.test());
                equal(event.decimal("rubAmount"), new BigDecimal("1141.4875"));
                expect(SignatureException.class, () -> Webhooks.parse(Arrays.copyOf(raw, raw.length - 1), sig, "webhook-secret"));
                check(!Webhooks.verify(raw, sig, "wrong"));
                check(!Webhooks.verify(raw, "zz".repeat(32), "webhook-secret"));
                check(!Webhooks.verify(raw, null, "webhook-secret"));
            });
            run("payout webhook fields", () -> {
                String raw = "{\"event\":\"payout.succeeded\",\"eventId\":\"fixture-event\",\"id\":\"WD-509\",\"shopId\":7,\"externalId\":\"external-42\",\"method\":\"usdt_erc20\",\"fee\":240,\"amountUsdt\":54.9,\"manualCorrection\":true,\"statusRevision\":2,\"txHash\":\"hash\"}";
                WebhookEvent event = Webhooks.parse(raw, AnoreClient.hmacHex("webhook", raw), "webhook");
                check(event.isPayout());
                check(event.isSucceeded());
                check(event.manualCorrection());
                equal(event.eventId(), "fixture-event");
                equal(event.statusRevision(), 2L);
                equal(event.amountUsdt(), 54.9);
                equal(event.method(), "usdt_erc20");
                equal(event.externalId(), "external-42");
            });
            run("decimal JSON remains exact", () -> {
                Map<String, Object> value = Json.parseObject("{\"amount\":999999999999.99,\"large\":9223372036854775808,\"rate\":8.615e1}");
                equal(value.get("amount"), new BigDecimal("999999999999.99"));
                equal(value.get("large"), new BigInteger("9223372036854775808"));
                equal(value.get("rate"), new BigDecimal("8.615e1"));
                equal(Json.parseObject(Json.write(value)), value);
                equal(new Payment(Json.parseObject("{\"amount\":\"13.25\"}")).amount(), 13.25);
            });
            run("invalid JSON never becomes a silent empty model", () -> {
                List<String> invalid = Arrays.asList("", "[]", "null", "true", "{} trailing", "{\"a\":01}", "{\"a\":+1}",
                        "{\"a\":.1}", "{\"a\":1.}", "{\"a\":1e}", "{\"a\":NaN}", "{\"a\":\"\\u00zz\"}",
                        "{\"a\":\"\\u12\"}", "{\"a\":\"\\", "{\"a\":\"line\nfeed\"}");
                for (String json : invalid) expect(AnoreException.class, () -> Json.parseObject(json));
                expect(IllegalArgumentException.class, () -> Json.write(Collections.singletonMap("amount", Double.NaN)));
                expect(IllegalArgumentException.class, () -> Json.write(Collections.singletonMap("amount", Double.POSITIVE_INFINITY)));
            });
            run("finite amounts and payout whole rubles", () -> {
                expect(IllegalArgumentException.class, () -> AnoreClient.CreatePaymentParams.of(Double.POSITIVE_INFINITY, "test"));
                expect(IllegalArgumentException.class, () -> AnoreClient.CreatePaymentParams.of(Double.NaN, "test"));
                expect(IllegalArgumentException.class, () -> AnoreClient.CreatePayoutParams.of(1000.5, "usdt_ton", "fixture"));
                expect(IllegalArgumentException.class, () -> AnoreClient.CreatePayoutParams.of(Double.POSITIVE_INFINITY, "card", "fixture"));
                expect(IllegalArgumentException.class, () -> AnoreClient.CreatePayoutParams.of(BigDecimal.ZERO, "card", "fixture"));
            });
            run("configuration and pagination validation", () -> {
                expect(IllegalArgumentException.class, () -> AnoreClient.builder().apiKey(" ").build());
                expect(IllegalArgumentException.class, () -> AnoreClient.builder().apiKey("key").baseUrl("https://api.anore.cc?token=secret").build());
                expect(IllegalArgumentException.class, () -> AnoreClient.builder().apiKey("key").maxRetries(-1).build());
                expect(IllegalArgumentException.class, () -> AnoreClient.builder().apiKey("key").timeout(Duration.ZERO).build());
                expect(IllegalArgumentException.class, () -> new AnoreClient.ListPaymentsParams().limit(201));
                expect(IllegalArgumentException.class, () -> new AnoreClient.ListPaymentsParams().offset(-1));
                expect(IllegalArgumentException.class, () -> client(origin).getBalance(0L));
            });
            run("thread interruption is preserved without retry", () -> {
                Thread.currentThread().interrupt();
                expect(ApiConnectionException.class, () -> client(origin).getPayment("payment"));
                check(Thread.currentThread().isInterrupted());
                Thread.interrupted();
                equal(requests.get(), 0);
            });
            System.out.println(passed + " Java SDK contract checks passed");
        } finally {
            Thread.interrupted();
            server.stop(0);
        }
    }

    private static AnoreClient client(String base) { return client(base, 2); }
    private static AnoreClient client(String base, int retries) {
        return AnoreClient.builder().apiKey("fixture-key").secret("api-secret")
                .baseUrl(base).maxRetries(retries).timeout(Duration.ofSeconds(5)).build();
    }

    private static String fixture(String requestPath) {
        if (requestPath.endsWith("/payouts/fees")) return "{\"shopId\":7,\"currency\":\"RUB\",\"maxAmountRubByMethod\":{\"usdt_erc20\":1500000},\"methods\":" + FEES + "}";
        if (requestPath.endsWith("/payouts/rates")) return "{\"ratePolicy\":\"approval\",\"rateBasis\":\"fixed_0900_msk\",\"sbpRatePolicy\":\"execution\",\"rapiraFixing\":\"09:00 Europe/Moscow\",\"fees\":" + FEES + "}";
        if (requestPath.endsWith("/balance")) return "{\"available\":9000.25,\"shopLocalBalance\":9100.25,\"rateBasis\":\"fixed_0900_msk\",\"minAmountByMethod\":{\"card_rub\":3000},\"maxAmountByMethod\":{\"usdt_erc20\":1500000},\"fee\":{\"methods\":" + FEES + "}}";
        if (requestPath.endsWith("/payouts/WD-509")) return "{\"id\":\"WD-509\",\"status\":\"rejected\",\"method\":\"USDT ERC20\",\"methodCode\":\"usdt_erc20\",\"manualCorrection\":true,\"statusRevision\":2,\"amountUsdt\":54.9,\"quoteAppliedAt\":\"2026-10-03T09:00:00Z\"}";
        if (requestPath.endsWith("/payouts")) return "{\"id\":\"WD-509\",\"status\":\"pending\",\"quoteType\":\"estimate\",\"ratePolicy\":\"approval\",\"rateBasis\":\"fixed_0900_msk\"}";
        if (requestPath.endsWith("/payments") && body.isEmpty()) return "{\"success\":true,\"total\":45,\"limit\":20,\"offset\":40,\"payments\":[{\"id\":\"fixture\",\"status\":\"written_off\"}]}";
        return PAYMENT;
    }

    private static void run(String name, Checked action) throws Exception {
        requests.set(0);
        overrideBody = null;
        overrideStatus = 0;
        failuresLeft = 0;
        action.run();
        passed++;
        System.out.println("PASS " + name);
    }

    private static void equal(Object actual, Object expected) {
        if (!java.util.Objects.equals(actual, expected)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError("Expected true"); }
    private static <T extends Throwable> T expect(Class<T> type, Checked action) throws Exception {
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return type.cast(error);
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + error, error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private interface Checked { void run() throws Exception; }
}

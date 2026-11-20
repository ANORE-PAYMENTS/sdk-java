# anore — Java SDK

Официальный SDK для приёма платежей через [anore](https://anore.cc). Без зависимостей, JDK 11+ (встроенный `HttpClient`, `javax.crypto`).

## Структура

```
java/
├── pom.xml
└── src/main/java/cc/anore/
    ├── AnoreClient.java   клиент платежей, баланса и выплат
    ├── Webhooks.java      verify / parse
    ├── Payment.java       модель платежа
    ├── WebhookEvent.java  модель события вебхука
    ├── Json.java          встроенный JSON (package-private)
    ├── Model.java         база моделей (package-private)
    └── *Exception.java    иерархия ошибок
```

## Установка

### Maven (через [JitPack](https://jitpack.io) — без публикации в Maven Central)

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.ANORE-PAYMENTS</groupId>
  <artifactId>sdk-java</artifactId>
  <version>d69cdfd67a0105b9d719624d845f0e6a778721f2</version>
</dependency>
```

Сейчас документация фиксирует проверенный commit-SHA, чтобы зависимость разрешалась без плавающего snapshot. Стабильная координата `1.3.0` станет доступна только после отдельно одобренного создания тега `1.3.0`; этот PR тег не создаёт и ничего не публикует.

Либо просто скопируйте папку `cc/anore/` в проект — зависимостей нет.

## Быстрый старт

```java
import cc.anore.*;
import cc.anore.AnoreClient.CreatePaymentParams;

AnoreClient anore = AnoreClient.builder()
        .apiKey("an_live_xxxxxxxxxxxxxxxx")
        .build();

// 1. создать счёт
Payment payment = anore.createPayment(
        CreatePaymentParams.of(1500, "Подписка Pro")
                .orderId("order_42")
                .shopId(1)
                .userIdentity(UserIdentity.telegramId("123456789")
                        .telegramUsername("@buyer_name"))
                .franchise(Franchise.website("https://branch.example", "987654321")));
System.out.println(payment.paymentUrl()); // отправьте клиента сюда

// 2. проверить статус
Payment status = anore.getPayment(payment.id());
System.out.println(status.status() + " " + status.paid()); // paid true

PaymentList page = anore.listPayments(new AnoreClient.ListPaymentsParams()
        .shopId(1).status("paid").limit(20));
Balance balance = anore.getBalance(1L);

Payout payout = anore.createPayout(
        AnoreClient.CreatePayoutParams.of(5000, "usdt_ton", "UQ...")
                .shopId(1)
                .externalId("payout_42"));
System.out.println(payout.id());

PayoutList payouts = anore.listPayouts(new AnoreClient.ListPayoutsParams()
        .shopId(1).limit(50));
```

`user_identity` поддерживает `telegram_id`, `email`, `username`, `external_id`. Для Telegram ID требуется каноническая положительная int64-строка; `telegram_username` разрешён только для `telegram_id`. `franchise` поддерживает `telegram_bot` и канонический HTTPS origin `website`. SDK проверяет вложенные поля до сети и отправляет их только при создании платежа; модели ответов эти защищённые входные данные не раскрывают.

## Подписки

```java
SubscriptionCatalog catalog = anore.getSubscriptionCatalog();
SubscriptionBinding binding = anore.createSubscription(
        AnoreClient.CreateSubscriptionParams.of(
                "monthly-pro", "customer-42", "buyer@example.com", "bind-order-42")
                .shopId(1));

SubscriptionStatus subscription = anore.getSubscription(binding.subscriptionId());
SubscriptionCharge charge = anore.chargeSubscription(binding.subscriptionId(), "charge-2026-11");
```

`Idempotency-Key` для обоих subscription POST обязателен и всегда задаётся вызывающим кодом (1–128 непробельных символов). SDK никогда автоматически не повторяет POST. При `503 binding_reconciling` сохраните точные ключ и тело и не создавайте новый ключ, повторный или параллельный POST. Если ответ содержит корректный `subscriptionId`, получите его через `error.getSubscriptionId()` и проверяйте `getSubscription(id)`; если ID отсутствует или некорректен, не пытайтесь угадать его — дождитесь reconciliation либо следуйте указаниям оператора/API.

## Проверка вебхука

При оплате anore шлёт `POST` на ваш URL с заголовком `Anore-Signature`.
Проверяйте подпись по **сырому** телу запроса (не распарсенному JSON):

Тот же обработчик принимает `payout.created`, `payout.processing`, `payout.succeeded`, `payout.failed` и `payout.updated` (ручная корректировка статуса, см. `statusRevision`); используйте `event.isPayout()`.

```java
// пример для сервлета
byte[] raw = request.getInputStream().readAllBytes();
String sig = request.getHeader("Anore-Signature");

try {
    WebhookEvent event = Webhooks.parse(raw, sig, System.getenv("ANORE_WEBHOOK_SECRET"));
    if (event.isSucceeded()) {
        // отгрузить заказ event.id() / event.orderId()
    }
    response.setStatus(200);
} catch (SignatureException e) {
    response.setStatus(403); // подпись не сошлась
}
```

## Обработка ошибок

```java
try {
    anore.createPayment(CreatePaymentParams.of(1500, "Заказ").shopId(1));
} catch (ValidationException e) {       // 400 — кривой запрос
} catch (AuthenticationException e) {   // 401 — неверный ключ
} catch (ApiConnectionException e) {    // сеть недоступна
}
```

## Справка

| API | Описание |
|-----|----------|
| `AnoreClient.builder().apiKey(...).secret(...).build()` | клиент; `secret` подписывает исходящие запросы |
| `createPayment(CreatePaymentParams)` | создать счёт → `Payment` |
| `getPayment(id)` | статус → `Payment` (`.status()`, `.paid()`) |
| `listPayments(ListPaymentsParams)` | страница платежей → `PaymentList` |
| `getSubscriptionCatalog()` | каталог планов и касс → `SubscriptionCatalog` |
| `createSubscription(CreateSubscriptionParams)` | создать привязку → `SubscriptionBinding`; требует caller-supplied `Idempotency-Key` |
| `getSubscription(id)` | статус привязки → `SubscriptionStatus` |
| `chargeSubscription(id, idempotencyKey)` | создать списание → `SubscriptionCharge` |
| `getBalance(shopId)` | баланс → `Balance` |
| `getPayoutFees(shopId)` | комиссии → `PayoutFees` |
| `getPayoutRates(shopId)` | курсы → `PayoutRates` |
| `listPayouts(ListPayoutsParams)` | cursor-страница выплат → `PayoutList` |
| `createPayout(CreatePayoutParams)` | заявка → `Payout` |
| `getPayout(id)` | статус выплаты → `Payout` |
| `Webhooks.verify(rawBody, signature, secret)` | проверка подписи → `boolean` |
| `Webhooks.parse(rawBody, signature, secret)` | проверка + разбор → `WebhookEvent` (бросает `SignatureException`) |

Ошибки API сохраняют статус, request ID и полный разобранный объект через `ApiException.getDetails()`; `getSubscriptionId()` возвращает положительный int64 ID, только если он действительно присутствует и корректен, иначе `null`. Классы: `ValidationException` (400), `AuthenticationException` (401), `ForbiddenException` (403), `NotFoundException` (404), `ServerException` (5xx), `ApiConnectionException` (сеть), `SignatureException` (подпись). База — `AnoreException`.

Полная документация: https://anore.cc/docs

# anore — Java SDK

Официальный SDK для приёма платежей через [anore](https://anore.cc). Без зависимостей, JDK 11+ (встроенный `HttpClient`, `javax.crypto`).

## Структура

```
java/
├── pom.xml
└── src/main/java/cc/anore/
    ├── AnoreClient.java   клиент (билдер, ретраи) + CreatePaymentParams
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
  <groupId>com.github.alexbrevis.zpay</groupId>
  <artifactId>anore</artifactId>
  <version>main-SNAPSHOT</version>
</dependency>
```

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
                .shopId(1)); // обязателен для аккаунтовых ключей (an_*)
System.out.println(payment.paymentUrl()); // отправьте клиента сюда

// 2. проверить статус
Payment status = anore.getPayment(payment.id());
System.out.println(status.status() + " " + status.paid()); // paid true
```

## Проверка вебхука

При оплате anore шлёт `POST` на ваш URL с заголовком `Anore-Signature`.
Проверяйте подпись по **сырому** телу запроса (не распарсенному JSON):

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
| `Webhooks.verify(rawBody, signature, secret)` | проверка подписи → `boolean` |
| `Webhooks.parse(rawBody, signature, secret)` | проверка + разбор → `WebhookEvent` (бросает `SignatureException`) |

Ошибки: `ValidationException` (400), `AuthenticationException` (401), `ForbiddenException` (403), `NotFoundException` (404), `ServerException` (5xx), `ApiConnectionException` (сеть), `SignatureException` (подпись). База — `AnoreException`.

Полная документация: https://anore.cc/docs

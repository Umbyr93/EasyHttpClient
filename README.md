# EasyHttpClient
An easy-to-use HTTP client built on top of **Java 11 HttpClient**.

---

## 🎯 Goal
The goal of **EasyHttpClient** is to simplify the use of Java's `HttpClient`, especially when handling ***path parameters*** and ***query parameters***.

---

## 🚀 Create the Request

Here's how to create an **EasyHttpRequest** that contains all the necessary details:
```java
var request = EasyHttpRequest.builder("https://blabla.org/countries/{country}/users/{user}")
    .GET()
    .pathParam("country", "italy")
    .pathParam("user", "001")
    .queryParam("findDeleted", "true")
    .header("Authorization", "token")
    .build();
```
You can see that `.pathParam()` was used multiple times. The same applies to `.queryParam()` and `.header()` — you can add as many as you need.

---

Another way to set parameters is to pass a map containing them, using the respective `.map()` methods:
```java
var request = EasyHttpRequest.builder("https://blabla.org/countries/{country}/users/{user}")
    .GET()
    .pathMap(Map.of("country", "italy", "user", "001"))
    .queryMap(Map.of("findDeleted", "true"))
    .headerMap(Map.of("Authorization", "token"))
    .build();
```
---

If you need to send a body for like a `POST` call, use the `.body()` method that accepts a `String`:
```java
var request = EasyHttpRequest.builder("https://blabla.org/countries/{country}/users/")
    .POST()
    .pathMap(Map.of("country", "italy"))
    .body("{\"userName\":\"test\"}")
    .build();
```

---

## ⚡ Executing the HTTP Call
### Synchronous Call: `send`
This method sends the request and returns an HttpResponse<String> containing the response body.
```java
HttpResponse<String> response = new EasyHttpClient().send(request);
```

---

### Asynchronous Call: `sendAsync`
This method sends the request asynchronously. It doesn't return the response directly, but when the response arrives, `EasyHttpClient` logs the URL, status code, and response body automatically.
```java
new EasyHttpClient().sendAsync(request);
```

---

### 🔄 Supported HTTP Methods
Currently supported HTTP methods:
- GET
- POST
- PUT
- PATCH
- DELETE
- HEAD

---

## 🛑 Exceptions
Instead of propagating the checked exceptions thrown by `HttpClient.send()` (such as `IOException` and `InterruptedException`), `EasyHttpClient` catches them and wraps them into a new unchecked exception: `EasyHttpException`.

---

## 📌 Notes
- Make sure your URLs correctly use curly braces `{}` around path parameters for correct replacement.
- Make sure the path parameter names in the URL and in the request are **IDENTICAL** (case sensitive).
- Asynchronous calls are fire-and-forget but log useful info upon completion.

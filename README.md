# EasyHttpClient
An easy-to-use HTTP client built on top of **Java 11 HttpClient**. Providing cleanier APIs to use it and rich features to extend it.

---

## 📝 Creating the Request
### Params one by one
Params can be set one by one, calling the specific methods multiple times:
```java
EasyHttpRequest request = EasyHttpRequest.builder("https://blabla.org/countries/{country}/users/{user}")
    .GET()
    .pathParam("country", "italy")
    .pathParam("user", "001")
    .queryParam("findDeleted", "true")
    .header("Authorization", "token")
    .fragment("test")
    .build();
```
**NOTE:** it is possible to call `fragment()` multiple times, but the value gets overwrited every time and the final value will be the last one set.

---

### Params map
Another way to set parameters is by passing a map containing them, using the respective `.map()` methods:
```java
EasyHttpRequest request = EasyHttpRequest.builder("https://blabla.org/countries/{country}/users/{user}")
    .GET()
    .pathMap(Map.of("country", "italy", "user", "001"))
    .queryMap(Map.of("findDeleted", "true"))
    .headerMap(Map.of("Authorization", "token"))
    .build();
```

### Built-in Headers
The request builder provides methods that allow to set the most common headers by just passing the value:
```java
EasyHttpRequest request = EasyHttpRequest.builder("https://blabla.org/countries")
    .GET()
    .authorization("token")
    .contentType("application/json")
    .build();
```

#### Available built-in Headers
- Authorization
- Content type
- Accept
- Accept encoding
- Accept language
- User agent
- Cookie
- Referer
- Origin

---

### Request Body
To send a body for like a `POST` call, there's the `.body(Data, Class)` method that accepts the data and the class type:
```java
public record SampleRequest(String username){}
```
```java
EasyHttpRequest request = EasyHttpRequest.builder("https://blabla.org/countries/{country}/users/")
    .POST()
    .pathMap(Map.of("country", "italy"))
    .body("{\"username\":\"test\"}", SampleRequest.class)
    .build();
```

---

## 🔄 Serialization
To handle the `Json<->Object` parsing for both request and response, it's possible to implement the  interface `EasySerializer`.

### Default Jackson Serializer
The library uses this `EasyJacksonSerializer` implementation by default:
```java
public class EasyJacksonSerializer implements EasySerializer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public <T> String serialize(T object) throws IOException {
        return MAPPER.writeValueAsString(object);
    }

    @Override
    public <T> T deserialize(String data, Class<T> clazz) throws IOException {
        return MAPPER.readValue(data, clazz);
    }
}
```

### Collections Parsing
Parsing to and from `Collection` is not supported yet. The main reasons are the quite rigid system that Java HttpClient uses for parsing and type erasure.
<br><br>
Currently, the only way is to pass and get an `Array of Objects`. Here's some examples to help with this conversion until this feature gets added:

`Array -> List (Unmodifiable)`
```java
String[] array = {"a", "b", "c"};
List<String> list = List.of(array);
```
`Array -> List (Modifiable)`
```java
String[] array = {"a", "b", "c"};
List<String> list = new ArrayList<>(List.of(array));
```
`List -> Array`
```java
List<String> list = List.of("a", "b", "c");
String[] array = list.toArray(new String[0]);
```

---

## 📦 Creating the EasyHttpClient
There are 2 ways to create an `EasyHttpClient` client.

### Default Client
```java
EasyHttpClient client = EasyHttpClient.defaultClient();
```

### Client Builder
```java
EasyHttpClient client = EasyHttpClient.builder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
```

#### Builder Options
- .serializer(`EasySerializer`)
- .connectTimeout(`Duration`)
- .followRedirects(`HttpClient.Redirect`)
- .proxy(`ProxySelector`)
- .sslContext(`SSLContext`)
- .sslParameters(`SSLParameters`)
- .authenticator(`Authenticator`)
- .version(`HttpClient.Version`)
- .execution(`Executor`)
- .cookieHandler(`CookieHandler`)

---

## ⚡ Executing the HTTP Call
### Synchronous
#### `send`(EasyHttpRequest)
Returns a `HttpResponse<String>` with the response body as a String.
```java
HttpResponse<String> response = new EasyHttpClient().send(request);
```
#### `send`(EasyHttpRequest, Class)
It's possible to specify the class type of the expected response. Here's a few examples:
```java
HttpResponse<SampleResponse> response = new EasyHttpClient().send(request, SampleResponse.class);
HttpResponse<SampleResponse[]> response = new EasyHttpClient().send(request, SampleResponse[].class);
HttpResponse<byte[]> response = new EasyHttpClient().send(request, byte[].class);
```

---

### Asynchronous
#### `sendAsync`(EasyHttpRequest)
Returns a `CompletableFuture<HttpResponse<String>>` with the body being a String.
```java
CompletableFuture<HttpResponse<String>> completableFuture = new EasyHttpClient().sendAsync(request);
```

#### `sendAsync`(EasyHttpRequest, Class)
It's possible to specify the class type of the expected response. Here's a few examples:
```java
CompletableFuture<HttpResponse<SampleResponse>> response = new EasyHttpClient().sendAsync(request, SampleResponse.class);
CompletableFuture<HttpResponse<SampleResponse[]>> response = new EasyHttpClient().sendAsync(request, SampleResponse[].class);
CompletableFuture<HttpResponse<byte[]>> response = new EasyHttpClient().sendAsync(request, byte[].class);
```

---

### Supported HTTP Methods
- GET
- POST
- PUT
- PATCH
- DELETE
- HEAD

---

## 🛑 Exceptions
- `HttpCallException` — instead of propagating the checked exceptions thrown by `HttpClient.send()` (such as `IOException` and `InterruptedException`), `EasyHttpClient` catches them and wraps them into a new unchecked exception.
- `MalformedUriException` — the URI creation throws an `IllegalArgumentException`. That is now wrapped into a more descriptive exception for more clarity and better debugging.
- `FileNotFoundRuntimeException` — if the request input is read from a file and the file can't be found, this exception is thrown.
- `SerializationException` — exception thrown when there's an error in the serialization process.
- `DeserializationException` — exception thrown when there's an error in the deserialization process.

---

## 📌 Notes
- Make sure your URLs correctly use curly braces `{}` around path parameters for correct replacement.
- Make sure the path parameter names in the URL and in the request are **IDENTICAL** (case sensitive).
- Asynchronous calls are fire-and-forget but log useful info upon completion.

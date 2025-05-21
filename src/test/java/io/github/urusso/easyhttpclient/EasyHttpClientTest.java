package io.github.urusso.easyhttpclient;

import io.github.urusso.easyhttpclient.constant.Headers;
import io.github.urusso.easyhttpclient.constant.HttpMethod;
import io.github.urusso.easyhttpclient.dto.SampleRequest;
import io.github.urusso.easyhttpclient.dto.SampleResponse;
import io.github.urusso.easyhttpclient.exception.HttpCallException;
import io.github.urusso.easyhttpclient.exception.MalformedUriException;
import io.github.urusso.easyhttpclient.serializer.EasyJacksonSerializer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
public class EasyHttpClientTest {
    private EasyHttpClient httpClient = EasyHttpClient.defaultClient();
    private final MockWebServer mockServer = new MockWebServer();

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, names = {"GET", "POST", "PUT", "DELETE", "HEAD"})
    public void sendTest_SingleManualParams(HttpMethod httpMethod) throws IOException, InterruptedException {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[\"1\", \"2\"]"));
        mockServer.start();

        String baseUrl = mockServer.url("/test").toString();
        baseUrl += "/users/{user}";

        EasyHttpRequest.Builder requestBuilder = EasyHttpRequest.builder(baseUrl)
                .pathParam("user", "1")
                .queryParam("findDeleted","true")
                .header("Authorization", "token");

        EasyHttpRequest request = switch (httpMethod) {
            case POST -> requestBuilder.POST().build();
            case PUT -> requestBuilder.PUT().build();
            case DELETE -> requestBuilder.DELETE().build();
            case HEAD -> requestBuilder.HEAD().build();
            default -> requestBuilder.GET().build();
        };

        HttpResponse<String[]> response = httpClient.send(request, String[].class);
        RecordedRequest recorded = mockServer.takeRequest();

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        if(!HttpMethod.HEAD.equals(httpMethod))
            assertTrue(response.body() != null && response.body()[0].equals("1")
                    && response.body()[1].equals("2"));
        assertNotNull(recorded.getPath());
        assertTrue(recorded.getPath().contains("users/1"));
        assertTrue(recorded.getPath().contains("findDeleted=true"));
        assertNotNull(recorded.getHeader("Authorization"));
        assertEquals("token", recorded.getHeader("Authorization"));

        mockServer.shutdown();
    }

    @Test
    public void sendTest_ParamMap_PATCH() throws IOException, InterruptedException {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"result\": \"OK\"}"));
        mockServer.start();

        String baseUrl = mockServer.url("/test").toString();
        baseUrl += "/countries/{country}/users/{user}//"; // Slashes added to test the correct cleaning of the URL

        var request = EasyHttpRequest.builder(baseUrl)
                .PATCH()
                .pathMap(Map.of("country", "italy", "user", "1"))
                .queryMap(Map.of("logData", "false", "overwrite","true"))
                .headerMap(Map.of("Authorization", "token", "Accept", "application/json"))
                .body("{\"username\": \"test\"}", SampleRequest.class)
                .build();

        httpClient = EasyHttpClient.defaultClient();

        HttpResponse<SampleResponse> response = httpClient.send(request, SampleResponse.class);
        RecordedRequest recorded = mockServer.takeRequest();

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body().result());
        assertNotNull(recorded.getPath());
        assertTrue(recorded.getPath().contains("countries/italy"));
        assertTrue(recorded.getPath().contains("users/1"));
        assertTrue(recorded.getPath().contains("overwrite=true"));
        assertTrue(recorded.getPath().contains("logData=false"));
        assertNotNull(recorded.getHeader("Authorization"));
        assertEquals("token", recorded.getHeader("Authorization"));
        assertNotNull(recorded.getHeader("Accept"));
        assertEquals("application/json", recorded.getHeader("Accept"));
        assertNotNull(recorded.getBody());
        String recordedBody = recorded.getBody().readUtf8();
        assertTrue(recordedBody.contains("username") && recordedBody.contains("test"));

        mockServer.shutdown();
    }

    @Test
    public void sendTest_ClientBuilder_BuiltInHeaders() throws IOException, InterruptedException, NoSuchAlgorithmException {
        httpClient = EasyHttpClient.builder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.getDefault())
                .sslContext(SSLContext.getDefault())
                .sslParameters(new SSLParameters())
                .authenticator(Authenticator.getDefault())
                .version(HttpClient.Version.HTTP_1_1)
                .cookieHandler(CookieHandler.getDefault())
                .serializer(new EasyJacksonSerializer())
                .build();

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"result\": \"OK\"}"));
        mockServer.start();

        String baseUrl = mockServer.url("/test").toString();
        baseUrl += "/countries";

        var request = EasyHttpRequest.builder(baseUrl)
                .POST()
                .authorization("test")
                .contentType("test")
                .accept("test")
                .acceptEncoding("test")
                .acceptLanguage("test")
                .userAgent("test")
                .cookie("test")
                .referer("test")
                .origin("test")
                .fragment("test 2")
                .body("{\"username\": \"test\"}", SampleRequest.class)
                .build();

        httpClient = EasyHttpClient.defaultClient();

        HttpResponse<String> response = httpClient.send(request);
        RecordedRequest recorded = mockServer.takeRequest();

        assertNotNull(response);
        assertEquals("test", recorded.getHeader(Headers.AUTHORIZATION.code));
        assertEquals("test", recorded.getHeader(Headers.CONTENT_TYPE.code));
        assertEquals("test", recorded.getHeader(Headers.ACCEPT.code));
        assertEquals("test", recorded.getHeader(Headers.ACCEPT_ENCODING.code));
        assertEquals("test", recorded.getHeader(Headers.ACCEPT_LANGUAGE.code));
        assertEquals("test", recorded.getHeader(Headers.USER_AGENT.code));
        assertEquals("test", recorded.getHeader(Headers.COOKIE.code));
        assertEquals("test", recorded.getHeader(Headers.REFERER.code));
        assertEquals("test", recorded.getHeader(Headers.ORIGIN.code));

        mockServer.shutdown();
    }

    @Test
    public void sendTest_MalformedUriException() {
        var request = EasyHttpRequest.builder("/test")
                .PATCH()
                .pathMap(Map.of("country", "italy", "user", "1"))
                .queryMap(Map.of("logData", "false", "overwrite","true"))
                .headerMap(Map.of("Authorization", "token", "Accept", "application/json"))
                .body("{\"username\":\"test\"}", String.class)
                .build();

        assertThrows(MalformedUriException.class, () -> httpClient.send(request));
    }

    @Test
    public void sendTest_HttpCallException() {
        var request = EasyHttpRequest.builder("https://localhost:8080/test")
                .GET()
                .build();

        assertThrows(HttpCallException.class, () -> httpClient.send(request));
    }

    @Test
    public void sendAsyncTest() throws IOException, InterruptedException, ExecutionException {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("hello"));
        mockServer.start();

        String baseUrl = mockServer.url("/test").toString();

        var request = EasyHttpRequest.builder(baseUrl)
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> completableFuture = httpClient.sendAsync(request);
        HttpResponse<String> response = completableFuture.get();

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());

        mockServer.shutdown();
    }

    @Test
    public void request_BlankUrlException() {
        assertThrows(IllegalArgumentException.class, () -> EasyHttpRequest.builder(null)
                .GET()
                .build());
        assertThrows(IllegalArgumentException.class, () -> EasyHttpRequest.builder("")
                .GET()
                .build());
    }

    @Test
    public void request_NoHttpMethodException() {
        assertThrows(IllegalArgumentException.class, () -> EasyHttpRequest.builder("test")
                .build());
    }
}

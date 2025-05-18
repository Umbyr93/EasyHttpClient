package io.github.urusso.easyhttpclient;

import io.github.urusso.easyhttpclient.enums.HttpMethod;
import io.github.urusso.easyhttpclient.exception.HttpCallException;
import io.github.urusso.easyhttpclient.exception.MalformedUriException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
public class EasyHttpClientTest {
    private final EasyHttpClient httpClient = new EasyHttpClient();
    private final MockWebServer mockServer = new MockWebServer();

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, names = {"GET", "POST", "PUT", "DELETE", "HEAD"})
    public void sendTest_SingleParam(HttpMethod httpMethod) throws IOException, InterruptedException {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("hello"));
        mockServer.start();

        String baseUrl = mockServer.url("/test").toString();
        baseUrl += "/users/{user}";

        EasyHttpRequest.EasyHttpRequestBuilder requestBuilder = EasyHttpRequest.builder(baseUrl)
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

        HttpResponse<String> response = httpClient.send(request);
        RecordedRequest recorded = mockServer.takeRequest();

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        if(!HttpMethod.HEAD.equals(httpMethod))assertTrue(response.body().contains("hello"));
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
                .setBody("hello"));
        mockServer.start();

        String baseUrl = mockServer.url("/test").toString();
        baseUrl += "/countries/{country}/users/{user}//"; // Slashes added to test the correct cleaning of the URL

        var request = EasyHttpRequest.builder(baseUrl)
                .PATCH()
                .pathMap(Map.of("country", "italy", "user", "1"))
                .queryMap(Map.of("logData", "false", "overwrite","true"))
                .headerMap(Map.of("Authorization", "token", "Accept", "application/json"))
                .jsonBody("{\"username\":\"test\"}")
                .build();

        HttpResponse<String> response = httpClient.send(request);
        RecordedRequest recorded = mockServer.takeRequest();

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("hello"));
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
        assertTrue(recordedBody.contains("\"username\":\"test\""));

        mockServer.shutdown();
    }

    @Test
    public void sendTest_MalformedUriException() {
        var request = EasyHttpRequest.builder("/test")
                .PATCH()
                .pathMap(Map.of("country", "italy", "user", "1"))
                .queryMap(Map.of("logData", "false", "overwrite","true"))
                .headerMap(Map.of("Authorization", "token", "Accept", "application/json"))
                .jsonBody("{\"username\":\"test\"}")
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
        assertTrue(response.body().contains("hello"));

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

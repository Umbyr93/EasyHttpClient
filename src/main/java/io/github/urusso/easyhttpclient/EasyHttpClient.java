package io.github.urusso.easyhttpclient;

import io.github.urusso.easyhttpclient.exception.HttpCallException;
import io.github.urusso.easyhttpclient.exception.MalformedUriException;
import io.github.urusso.easyhttpclient.interfaces.EasySerializer;
import io.github.urusso.easyhttpclient.serializer.EasyJacksonSerializer;
import io.github.urusso.easyhttpclient.utils.EasyBodyHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EasyHttpClient {
    private final HttpClient httpClient;
    private final EasyBodyHandler easyBodyHandler;

    private EasyHttpClient(EasySerializer serializer) {
        this.httpClient = HttpClient.newHttpClient();
        this.easyBodyHandler = new EasyBodyHandler(serializer);
    }

    private EasyHttpClient(Duration connectTimeout, HttpClient.Redirect followRedirects, ProxySelector proxy,
                           SSLContext sslContext, SSLParameters sslParameters, Authenticator authenticator,
                           HttpClient.Version version, Executor executor, CookieHandler cookieHandler,
                           EasySerializer serializer) {

        var clientBuilder = HttpClient.newBuilder();

        if(connectTimeout != null)
            clientBuilder.connectTimeout(connectTimeout);
        if(followRedirects != null)
            clientBuilder.followRedirects(followRedirects);
        if(proxy != null)
            clientBuilder.proxy(proxy);
        if(sslContext != null)
            clientBuilder.sslContext(sslContext);
        if(sslParameters != null)
            clientBuilder.sslParameters(sslParameters);
        if(authenticator != null)
            clientBuilder.authenticator(authenticator);
        if(version != null)
            clientBuilder.version(version);
        if(executor != null)
            clientBuilder.executor(executor);
        if(cookieHandler != null)
            clientBuilder.cookieHandler(cookieHandler);

        this.httpClient = clientBuilder.build();
        this.easyBodyHandler = new EasyBodyHandler(serializer);
    }

    public static EasyHttpClient defaultClient() {
        return new EasyHttpClient(new EasyJacksonSerializer());
    }

    /**
     * Default method to execute a synchronous HTTP call that has String for the response body
     *
     * @param easyReq {@link EasyHttpRequest} containing all the info to make the call
     * @return {@link HttpResponse} containing a String with the body received
     */
    public HttpResponse<String> send(EasyHttpRequest easyReq) {
        return send(easyReq, String.class);
    }

    /**
     * The method that executes a synchronous HTTP call
     *
     * @param easyReq {@link EasyHttpRequest} containing all the info to make the call
     * @return {@link HttpResponse} containing a String with the body received
     */
    public <T> HttpResponse<T> send(EasyHttpRequest easyReq, Class<T> responseClass) {
        HttpRequest request = convertRequest(easyReq);

        try {
            var bodyHandler = easyBodyHandler.getResponseBodyHandler(responseClass);
            return httpClient.send(request, bodyHandler);
        } catch (IOException | InterruptedException e) {
            throw new HttpCallException(e);
        }
    }

    /**
     * Default method to execute a asynchronous HTTP call that has String for the response body
     *
     * @param easyReq {@link EasyHttpRequest} containing all the info to make the call
     * @return {@link CompletableFuture} of the {@link HttpResponse}
     */
    public CompletableFuture<HttpResponse<String>> sendAsync(EasyHttpRequest easyReq) {
        return sendAsync(easyReq, String.class);
    }

    /**
     * The method that executes an asynchronous HTTP call
     *
     * @param easyReq {@link EasyHttpRequest} containing all the info to make the call
     * @return {@link CompletableFuture} of the {@link HttpResponse}
     */
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(EasyHttpRequest easyReq, Class<T> responseClass) {
        HttpRequest request = convertRequest(easyReq);

        var bodyHandler = easyBodyHandler.getResponseBodyHandler(responseClass);
        return httpClient.sendAsync(request, bodyHandler);
    }

    /**
     * Converts {@link EasyHttpRequest} to {@link HttpRequest}
     *
     * @param easyReq {@link EasyHttpRequest
     * @return {@link HttpRequest}
     */
    private HttpRequest convertRequest(EasyHttpRequest easyReq) {
        var requestBuilder = HttpRequest.newBuilder();

        try {
            requestBuilder.uri(getUri(easyReq));
        } catch (IllegalArgumentException e) {
            throw new MalformedUriException(e);
        }

        setHttpMethod(requestBuilder, easyReq);
        setHeaders(requestBuilder, easyReq.getHeaders());

        return requestBuilder.build();
    }

    /**
     * Manipulates the URL string for params to replace/add
     *
     * @param request {@link EasyHttpRequest}
     * @return {@link URI} created with the manipulated String
     */
    private static URI getUri(EasyHttpRequest request) {
        String url = removeEndSlashes(request.getUrl());
        url = replacePathParams(url, request.getPathParams());
        url = addQueryParams(url, request.getQueryParams());
        url = addFragment(url, request.getFragment());

        return URI.create(url);
    }

    /**
     * Add fragment to the URL, like
     *
     * @param url String to manipulate
     * @param fragment String fragment to add
     * @return fixed String URL
     */
    private static String addFragment(String url, String fragment) {
        if(fragment != null && !fragment.isBlank()) {
            String encoded = encode(fragment);
            return url.concat("#").concat(encoded);
        }
        return url;
    }

    /**
     * Removes unnecessary end slashes. An URL like "blabla.com/api/user///" becomes "blabla.com/api/user"
     *
     * @param url String to manipulate
     * @return fixed String URL
     */
    private static String removeEndSlashes(String url) {
        int substringEnd = url.length();
        for (int i = url.length() - 1; i >= 0; i--) {
            if (url.charAt(i) == '/')
                substringEnd -= 1;
            else
                break;
        }

        return url.substring(0, substringEnd);
    }

    /**
     * Replaces path params added in the URL string with the pathParams map
     *
     * @param url String to manipulate
     * @param pathParams {@link Map] containing the key to replace and the value to replace it with
     * @return String URL with path params replaced
     */
    private static String replacePathParams(String url, Map<String, String> pathParams) {
        for (Map.Entry<String, String> e : pathParams.entrySet()) {
            String paramToReplace = "{" + e.getKey() + "}";
            url = url.replace(paramToReplace, encode(e.getValue()));
        }

        return url;
    }

    /**
     * Replaces path params added in the URL string with the pathParams map
     *
     * @param url String to manipulate
     * @param queryParams {@link Map} containing the key-value params to add
     * @return String URL with path params replaced
     */
    private static String addQueryParams(String url, Map<String, String> queryParams) {
        var paramsToAdd = new StringBuilder();

        boolean justStarted = true;
        for (Map.Entry<String, String> e : queryParams.entrySet()) {
            if (justStarted)
                paramsToAdd.append("?");
            else
                paramsToAdd.append("&");

            paramsToAdd.append(e.getKey()).append("=").append(encode(e.getValue()));
            justStarted = false;
        }

        return url.concat(paramsToAdd.toString());
    }

    /**
     * Sets the HTTP Method from the EasyHttpRequest
     *
     * @param builder {@link HttpRequest.Builder}
     * @param easyReq {@link EasyHttpRequest}
     */
    private void setHttpMethod(HttpRequest.Builder builder, EasyHttpRequest easyReq) {
        var bodyPublisher = easyBodyHandler.getRequestBodyPublisher(easyReq.getBody());

        switch (easyReq.getHttpMethod()) {
            case GET:
                builder.GET();
                break;
            case POST:
                builder.POST(bodyPublisher);
                break;
            case PUT:
                builder.PUT(bodyPublisher);
                break;
            case PATCH:
                builder.method("PATCH", bodyPublisher);
                break;
            case DELETE:
                builder.DELETE();
                break;
            case HEAD:
                builder.HEAD();
                break;
        }
    }

    /**
     * Adds the headers from the EasyHttpRequest
     *
     * @param builder {@link HttpRequest.Builder}
     * @param headers {@link Map} containing the headers to add
     */
    private void setHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            builder.header(e.getKey(), e.getValue());
        }
    }

    /**
     * Encodes URI parameters. <br>
     * Since {@link URLEncoder} is actually made for HTML form encoding, everything is correctrly converted except for spaces.
     * Spaces are converted to a '+' character, which is why this function replace them with '%20'
     *
     * @param param String parameter to encode
     * @return String parameter encoded
     */
    private static String encode(String param) {
        return URLEncoder.encode(param, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static Builder builder() {
        return new Builder();
    }

    //*******************************************
    //***************** BUILDER *****************
    //*******************************************
    public static class Builder {
        private EasySerializer serializer;
        private Duration connectTimeout;
        private HttpClient.Redirect followRedirects;
        private ProxySelector proxy;
        private SSLContext sslContext;
        private SSLParameters sslParameters;
        private Authenticator authenticator;
        private HttpClient.Version version;
        private Executor executor;
        private CookieHandler cookieHandler;

        private Builder() {}

        public Builder serializer(EasySerializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder followRedirects(HttpClient.Redirect followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder proxy(ProxySelector proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder sslParameters(SSLParameters sslParameters) {
            this.sslParameters = sslParameters;
            return this;
        }

        public Builder authenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        public Builder version(HttpClient.Version version) {
            this.version = version;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder cookieHandler(CookieHandler cookieHandler) {
            this.cookieHandler = cookieHandler;
            return this;
        }

        public EasyHttpClient build() {
            serializer = Objects.requireNonNullElseGet(serializer, EasyJacksonSerializer::new);

            return new EasyHttpClient(connectTimeout, followRedirects, proxy, sslContext, sslParameters, authenticator,
                    version, executor, cookieHandler, serializer);
        }
    }
}

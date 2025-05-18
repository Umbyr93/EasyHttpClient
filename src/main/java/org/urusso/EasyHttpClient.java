package org.urusso;

import org.urusso.exception.EasyHttpException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

public class EasyHttpClient {
    private static final Logger LOGGER = Logger.getLogger(EasyHttpClient.class.getName());
    private final HttpClient httpClient;

    public EasyHttpClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * The method that executes the HTTP call synchronously
     *
     * @param easyReq {@link EasyHttpRequest} containing all the info to make the call
     * @return {@link HttpResponse} containing a String with the body received
     */
    public HttpResponse<String> send(EasyHttpRequest easyReq) {
        HttpRequest request = convertRequest(easyReq);

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new EasyHttpException(e);
        }
    }

    /**
     * The method that executes the HTTP call asynchronously
     *
     * @param easyReq {@link EasyHttpRequest} containing all the info to make the call
     */
    public void sendAsync(EasyHttpRequest easyReq) {
        HttpRequest request = convertRequest(easyReq);

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp ->
                        LOGGER.info(String.format("Endpoint called: %s(%s)\nStatus code: %s\nResponse body: %s",
                                request.uri().getHost() + request.uri().getPath(), easyReq.getHttpMethod().toString(),
                                resp.statusCode(), resp.body())
                        ));
    }

    /**
     * Converts {@link EasyHttpRequest} to {@link HttpRequest}
     *
     * @param easyReq {@link EasyHttpRequest
     * @return {@link HttpRequest}
     */
    private HttpRequest convertRequest(EasyHttpRequest easyReq) {
        var requestBuilder = HttpRequest.newBuilder()
                .uri(getUri(easyReq));

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

        return URI.create(url);
    }

    /**
     * Removes unnecessary end slashes. An URL like "blabla.com/api/user///" becomes "blabla.com/api/user"
     *
     * @param url String to manipulate
     * @return fiex String URL
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
     * @param url        String to manipulate
     * @param pathParams {@link Map] containing the key to replace and the value to replace it with
     * @return String URL with path params replaced
     */
    private static String replacePathParams(String url, Map<String, String> pathParams) {
        for (Map.Entry<String, String> e : pathParams.entrySet()) {
            String paramToReplace = "{" + e.getKey() + "}";
            url = url.replace(paramToReplace, URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }

        return url;
    }

    /**
     * Replaces path params added in the URL string with the pathParams map
     *
     * @param url         String to manipulate
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

            paramsToAdd.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
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
        switch (easyReq.getHttpMethod()) {
            case GET:
                builder.GET();
                break;
            case POST:
                builder.POST(HttpRequest.BodyPublishers.ofString(easyReq.getJsonBody()));
                break;
            case PUT:
                builder.PUT(HttpRequest.BodyPublishers.ofString(easyReq.getJsonBody()));
                break;
            case PATCH:
                builder.method("PATCH", HttpRequest.BodyPublishers.ofString(easyReq.getJsonBody()));
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
}

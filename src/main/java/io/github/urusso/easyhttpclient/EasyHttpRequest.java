package io.github.urusso.easyhttpclient;

import io.github.urusso.easyhttpclient.constant.Headers;
import io.github.urusso.easyhttpclient.constant.HttpMethod;
import io.github.urusso.easyhttpclient.dto.Body;

import java.util.HashMap;
import java.util.Map;

public class EasyHttpRequest {
    private final String url;
    private final HttpMethod httpMethod;
    private final Map<String,String> pathParams;
    private final Map<String,String> queryParams;
    private final Map<String,String> headers;
    private final String fragment;
    private final Body body;

    private EasyHttpRequest(String url, HttpMethod httpMethod, Map<String, String> pathParams,
                            Map<String, String> queryParams, Map<String, String> headers, String fragment, Body body) {

        if(url == null || url.isBlank())
            throw new IllegalArgumentException("Url can't be null or blank");
        if(httpMethod == null)
            throw new IllegalArgumentException("HttpMethod can't be null");

        this.url = url;
        this.httpMethod = httpMethod;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body;
        this.fragment = fragment;
    }

    public static Builder builder(String url) {
        return new Builder(url);
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getFragment() {
        return fragment;
    }

    public Body getBody() {
        return body;
    }

    //*******************************************
    //***************** BUILDER *****************
    //*******************************************
    public static class Builder {
        private final String url;
        private HttpMethod httpMethod;
        private Map<String,String> pathParams;
        private Map<String,String> queryParams;
        private Map<String,String> headers;
        private String fragment;
        private Body body;

        private Builder(String url) {
            this.url = url;
            this.pathParams = new HashMap<>();
            this.queryParams = new HashMap<>();
            this.headers = new HashMap<>();
        }

        public Builder pathParam(String key, String value) {
            this.pathParams.put(key, value);
            return this;
        }

        public Builder pathMap(Map<String,String> pathParams) {
            this.pathParams = pathParams;
            return this;
        }

        public Builder queryParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }

        public Builder queryMap(Map<String,String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder headerMap(Map<String,String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder userAgent(String value) {
            this.headers.put(Headers.USER_AGENT.code, value);
            return this;
        }

        public Builder accept(String value) {
            this.headers.put(Headers.ACCEPT.code, value);
            return this;
        }

        public Builder acceptLanguage(String value) {
            this.headers.put(Headers.ACCEPT_LANGUAGE.code, value);
            return this;
        }

        public Builder acceptEncoding(String value) {
            this.headers.put(Headers.ACCEPT_ENCODING.code, value);
            return this;
        }

        public Builder authorization(String value) {
            this.headers.put(Headers.AUTHORIZATION.code, value);
            return this;
        }

        public Builder contentType(String value) {
            this.headers.put(Headers.CONTENT_TYPE.code, value);
            return this;
        }

        public Builder cookie(String value) {
            this.headers.put(Headers.COOKIE.code, value);
            return this;
        }

        public Builder referer(String value) {
            this.headers.put(Headers.REFERER.code, value);
            return this;
        }

        public Builder origin(String value) {
            this.headers.put(Headers.ORIGIN.code, value);
            return this;
        }

        public Builder fragment(String fragment) {
            this.fragment = fragment;
            return this;
        }

        public Builder body(Object body, Class<?> type) {
            this.body = new Body(body, type);
            return this;
        }

        public Builder GET() {
            this.httpMethod = io.github.urusso.easyhttpclient.constant.HttpMethod.GET;
            return this;
        }

        public Builder POST() {
            this.httpMethod = io.github.urusso.easyhttpclient.constant.HttpMethod.POST;
            return this;
        }

        public Builder PUT() {
            this.httpMethod = HttpMethod.PUT;
            return this;
        }

        public Builder PATCH() {
            this.httpMethod = HttpMethod.PATCH;
            return this;
        }

        public Builder DELETE() {
            this.httpMethod = HttpMethod.DELETE;
            return this;
        }

        public Builder HEAD() {
            this.httpMethod = HttpMethod.HEAD;
            return this;
        }

        public EasyHttpRequest build() {
            return new EasyHttpRequest(url, httpMethod, pathParams, queryParams, headers, fragment, body);
        }
    }
}

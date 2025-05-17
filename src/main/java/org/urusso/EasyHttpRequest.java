package org.urusso;

import org.urusso.enums.HttpMethod;

import java.util.HashMap;
import java.util.Map;

public class EasyHttpRequest {
    private final String url;
    private final HttpMethod httpMethod;
    private final Map<String,String> pathParams;
    private final Map<String,String> queryParams;
    private final Map<String,String> headers;
    private final String jsonBody;

    private EasyHttpRequest(String url, HttpMethod httpMethod, Map<String, String> pathParams,
                            Map<String, String> queryParams, Map<String, String> headers, String jsonBody) {

        if(url == null || url.isBlank())
            throw new IllegalArgumentException("Url can't be null or blank");
        if(httpMethod == null)
            throw new IllegalArgumentException("HttpMethod can't be null");

        this.url = url;
        this.httpMethod = httpMethod;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
        this.headers = headers;
        this.jsonBody = jsonBody;
    }

    public static EasyHttpRequestBuilder builder(String url) {
        return new EasyHttpRequestBuilder(url);
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

    public String getJsonBody() {
        return jsonBody;
    }

    public static class EasyHttpRequestBuilder {
        private final String url;
        private HttpMethod httpMethod;
        private Map<String,String> pathParams;
        private Map<String,String> queryParams;
        private Map<String,String> headers;
        private String jsonBody = "{}";

        private EasyHttpRequestBuilder(String url) {
            this.url = url;
            this.pathParams = new HashMap<>();
            this.queryParams = new HashMap<>();
            this.headers = new HashMap<>();
        }

        public EasyHttpRequestBuilder pathParam(String key, String value) {
            this.pathParams.put(key, value);
            return this;
        }

        public EasyHttpRequestBuilder pathMap(Map<String,String> pathParams) {
            this.pathParams = pathParams;
            return this;
        }

        public EasyHttpRequestBuilder queryParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }

        public EasyHttpRequestBuilder queryMap(Map<String,String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public EasyHttpRequestBuilder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public EasyHttpRequestBuilder headerMap(Map<String,String> headers) {
            this.headers = headers;
            return this;
        }

        public EasyHttpRequestBuilder jsonBody(String jsonBody) {
            this.jsonBody = jsonBody;
            return this;
        }

        public EasyHttpRequestBuilder GET() {
            this.httpMethod = HttpMethod.GET;
            return this;
        }

        public EasyHttpRequestBuilder POST() {
            this.httpMethod = HttpMethod.POST;
            return this;
        }

        public EasyHttpRequestBuilder PUT() {
            this.httpMethod = HttpMethod.PUT;
            return this;
        }

        public EasyHttpRequestBuilder PATCH() {
            this.httpMethod = HttpMethod.PATCH;
            return this;
        }

        public EasyHttpRequestBuilder DELETE() {
            this.httpMethod = HttpMethod.DELETE;
            return this;
        }

        public EasyHttpRequestBuilder HEAD() {
            this.httpMethod = HttpMethod.HEAD;
            return this;
        }

        public EasyHttpRequest build() {
            return new EasyHttpRequest(url, httpMethod, pathParams, queryParams, headers, jsonBody);
        }
    }
}

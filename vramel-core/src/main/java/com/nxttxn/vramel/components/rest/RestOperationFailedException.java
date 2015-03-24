
package com.nxttxn.vramel.components.rest;

import java.util.Map;

import org.apache.camel.CamelException;
import org.apache.camel.util.ObjectHelper;

public class RestOperationFailedException extends CamelException {
    private final String uri;
    private final String redirectLocation;
    private final int statusCode;
    private final String statusText;
    private final Map<String, String> responseHeaders;
    private final String responseBody;

    public RestOperationFailedException(String uri, int statusCode, String statusText, String location, Map<String, String> responseHeaders, String responseBody) {
        super("HTTP operation failed invoking " + uri + " with statusCode: " + statusCode + (location != null ? ", redirectLocation: " + location : ""));
        this.uri = uri;
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.redirectLocation = location;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    public String getUri() {
        return uri;
    }

    public boolean isRedirectError() {
        return statusCode >= 300 && statusCode < 400;
    }

    public boolean hasRedirectLocation() {
        return ObjectHelper.isNotEmpty(redirectLocation);
    }

    public String getRedirectLocation() {
        return redirectLocation;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

}
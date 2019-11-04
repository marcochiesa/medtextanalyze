package org.getmarco.medtextanalyze.support;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
This class is meant to be returned from an AWS Lambda function configured for
API Gateway proxy integration .
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProxyResponse {

    /** HTTP response status code - 200 OK. */
    public static final int STATUS_CODE_OK = 200;
    /** HTTP response status code - 500 Internal Server Error. */
    public static final int STATUS_CODE_SERVER_ERROR = 500;

    private int statusCode;
    private Map<String, String> headers;
    private String body;
    private boolean isBase64Encoded;

    /**
     * Builder class for {@link ProxyResponse}.
     */
    public static class ProxyResponseBuilder {
        private int statusCode = STATUS_CODE_OK;
        private Map<String, String> headers = new HashMap<>();
        private String body = "";
        private boolean isBase64Encoded = false;

        /**
         * Sets the HTTP response status code.
         * @param statusCode the status
         * @return this builder for method chaining
         */
        @SuppressWarnings("checkstyle:hiddenfield")
        public ProxyResponseBuilder withStatusCode(final int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        /**
         * Sets the HTTP response status code to 200 OK.
         * @return this builder for method chaining
         */
        public ProxyResponseBuilder withOkStatus() {
            this.statusCode = STATUS_CODE_OK;
            return this;
        }

        /**
         * Adds CORS HTTP headers to the configured response.
         * @return this builder for method chaining
         */
        public ProxyResponseBuilder withCorsHeaders() {
            this.headers.put("Access-Control-Allow-Origin", "*");
            this.headers.put("Access-Control-Allow-Methods", ProxyRequest.HTTP_POST + "," + ProxyRequest.HTTP_OPTIONS);
            this.headers.put("Access-Control-Allow-Headers", "*");
            return this;
        }

        /**
         * Adds Content-Type header to the configured response.
         * @param contentType the content type to use
         * @return this builder for method chaining
         */
        public ProxyResponseBuilder withContentType(final String contentType) {
            this.headers.put("Content-Type", contentType);
            return this;
        }

        /**
         * Adds an HTTP header to the configured response.
         * @param header the HTTP header key
         * @param value the HTTP header value
         * @return this builder for method chaining
         */
        public ProxyResponseBuilder withHeader(final String header, final String value) {
            this.headers.put(header, value);
            return this;
        }

        /**
         * Sets the HTTP headers for the configured response.
         * @param headers HTTP header keys/values
         * @return this builder for method chaining
         */
        @SuppressWarnings("checkstyle:hiddenfield")
        public ProxyResponseBuilder withHeaders(final Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the body for the configured response.
         * @param body the HTTP response body
         * @return this builder for method chaining
         */
        @SuppressWarnings("checkstyle:hiddenfield")
        public ProxyResponseBuilder withBody(final String body) {
            this.body = body;
            return this;
        }

        /**
         * Declares whether the response body is Base64 encoded.
         * @param base64Encoded whether the response is Base64
         * @return this builder for method chaining
         */
        public ProxyResponseBuilder withBase64Encoded(final boolean base64Encoded) {
            isBase64Encoded = base64Encoded;
            return this;
        }

        /**
         * Build the {@link ProxyResponse}.
         * @return the response
         */
        public ProxyResponse build() {
            return new ProxyResponse(statusCode, headers, body, isBase64Encoded);
        }
    }
}

package org.getmarco.medtextanalyze.support;

import com.amazonaws.services.lambda.runtime.Context;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ProxyRequest {
    /** HTTP request method value - GET. */
    public static final String HTTP_GET = "GET";
    /** HTTP request method value - POST. */
    public static final String HTTP_POST = "POST";
    /** HTTP request method value - OPTIONS. */
    public static final String HTTP_OPTIONS = "OPTIONS";
    private String resource;
    private String path;
    private String httpMethod;
    private Map<String, String> headers;
    private Map<String, String> queryStringParameters;
    private Map<String, String> pathParameters;
    private Map<String, String> stageVariables;
    private Context context;
    private String body;
    private Boolean isBase64Encoded;

    /**
     * Determine if this request represents a HTTP GET.
     * @return true if request is HTTP GET
     */
    public boolean isHttpGet() {
        return HTTP_GET.equals(this.httpMethod);
    }

    /**
     * Determine if this request represents a HTTP POST.
     * @return true if request is HTTP POST
     */
    public boolean isHttpPost() {
        return HTTP_POST.equals(this.httpMethod);
    }

    /**
     * Determine if this request represents HTTP OPTIONS.
     * @return true if request is HTTP OPTIONS
     */
    public boolean isHttpOptions() {
        return HTTP_OPTIONS.equals(this.httpMethod);
    }

    /**
     * Returns string representation of this object for debugging.
     * @return string representation
     */
    @Override
    public String toString() {
        return "ProxyRequest{"
          + "resource='" + resource + '\''
          + ", path='" + path + '\''
          + ", httpMethod='" + httpMethod + '\''
          + ", headers=" + headers
          + ", queryStringParameters=" + stringifyQueryStringParameters()
          + ", pathParameters=" + pathParameters
          + ", stageVariables=" + stageVariables
          + ", context=" + context
          + ", body='" + stringifyBody() + '\''
          + ", isBase64Encoded=" + isBase64Encoded
          + '}';
    }

    private String stringifyQueryStringParameters() {
        if (this.queryStringParameters == null)
            return "nully querystringparameters stringify";
        List<String> list = this.queryStringParameters.entrySet().stream()
          .sorted(Comparator.comparing(Map.Entry::getKey))
          .map(Map.Entry::getKey).collect(Collectors.toList());
        return String.join(", ", list);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private String stringifyBody() {
        if (this.body == null)
            return "nully body stringify";
        return this.body.substring(0, Math.min(this.body.length(), 300));
    }
}

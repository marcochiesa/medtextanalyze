package org.getmarco.medtextanalyze.functions;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.comprehendmedical.AWSComprehendMedical;
import com.amazonaws.services.comprehendmedical.AWSComprehendMedicalClient;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;

import org.getmarco.medtextanalyze.support.Analyzer;
import org.getmarco.medtextanalyze.support.ProxyRequest;
import org.getmarco.medtextanalyze.support.ProxyResponse;

public abstract class FunctionSupport implements RequestHandler<ProxyRequest, ProxyResponse> {
    private static final String UPLOAD_BUCKET_VAR_NAME = "MED_UPLOAD_BUCKET";
    private static final String AWS_REGION_VAR_NAME = "MED_AWS_REGION";
    private static final int PRESIGNED_URL_VALIDITY = 30 * 60 * 1000; //30 minutes

    private ObjectMapper mapper;
    private AmazonS3 s3Client;
    private AmazonTextract textractClient;
    private AWSComprehendMedical comprehendMedicalClient;
    private Analyzer analyzer;
    private LambdaLogger logger;

    /**
     * Returns a Jackson {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper}.
     * @return the object mapper
     */
    protected ObjectMapper getObjectMapper() {
        if (this.mapper == null)
            this.mapper = new ObjectMapper();
        return this.mapper;
    }

    /**
     * Returns an AWS S3 client.
     * @return the S3 client
     */
    protected AmazonS3 getS3Client() {
        if (this.s3Client == null) {
            String region = getRegion();
            this.s3Client = AmazonS3ClientBuilder.standard().withRegion(region).build();
        }
        return this.s3Client;
    }

    /**
     * Returns an AWS Textract client.
     * @return the Textract client
     */
    protected AmazonTextract getTextractClient() {
        if (this.textractClient == null) {
            String region = getRegion();
            this.textractClient = AmazonTextractClientBuilder.standard().withRegion(region).build();
        }
        return this.textractClient;
    }

    /**
     * Returns an AWS Comprehend Medical client.
     * @return the Comprehend Medical client
     */
    protected AWSComprehendMedical getComprehendMedicalClient() {
        if (this.comprehendMedicalClient == null) {
            String region = getRegion();
            this.comprehendMedicalClient = AWSComprehendMedicalClient.builder()
              .withCredentials(new DefaultAWSCredentialsProviderChain())
              .withRegion(region).build();
        }
        return this.comprehendMedicalClient;
    }

    /**
     * Returns a {@link Analyzer Analyzer}.
     * @return the analyzer
     */
    protected Analyzer getAnalyzer() {
        if (this.analyzer == null)
            this.analyzer = new Analyzer();
        return this.analyzer;
    }

    /**
     * Returns the name of the configured S3 bucket for this application.
     * @return S3 bucket name
     */
    protected String getUploadBucket() {
        String bucket = System.getenv(UPLOAD_BUCKET_VAR_NAME);
        requiredValue(bucket, "bucket name");
        return bucket;
    }

    /**
     * Returns the configured region value for this application.
     * @return AWS region name
     */
    protected String getRegion() {
        String region = System.getenv(AWS_REGION_VAR_NAME);
        requiredValue(region, "missing region");
        return region;
    }

    /**
     * Output handler for AWS Lambda function using AWS API Gateway proxy integration.
     * @param request
     * @param context
     * @return
     */
    @Override
    public final ProxyResponse handleRequest(final ProxyRequest request, final Context context) {
        // Don't forget or all logging attempts will throw NPE.
        this.logger = context.getLogger();

        log(String.format("requestId: %s, fxn: %s, ver: %s",
          context.getAwsRequestId(), context.getFunctionName(),
          context.getFunctionVersion()));
        log("received proxy request: " + request.toString());

        //CORS Preflight
        if (request.isHttpOptions()) {
            return new ProxyResponse.ProxyResponseBuilder()
              .withOkStatus()
              .withCorsHeaders()
              .build();
        }
        return createResponse(request);
    }

    /**
     * Generate the function's {@link ProxyResponse ProxyResponse}
     * to return to AWS API Gateway (Lambda proxy integration).
     * @param request the request from API Gateway
     * @return the response for API Gateway
     */
    protected final ProxyResponse createResponse(final ProxyRequest request) {
        try {
            ProxyResponse response =  new ProxyResponse.ProxyResponseBuilder()
              .withStatusCode(ProxyResponse.STATUS_CODE_OK)
              .withCorsHeaders()
              .withContentType(getContentType())
              .withBody(createBody(request))
              .build();
            return response;
        } catch (Exception e) {
            return getError("error - " + e.getMessage());
        }
    }

    /**
     * Subclasses may override to set response content type as needed.
     * @return the content type
     */
    protected String getContentType() {
        return "application/json";
    }

    /**
     * Generate the response body content for this function. Subclasses must provide as needed.
     * @param request the request from API Gateway
     * @return the response body content
     * @throws Exception as needed by subclass
     */
    protected abstract String createBody(ProxyRequest request) throws Exception;

    private ProxyResponse getError(final String message) {
        return new ProxyResponse.ProxyResponseBuilder()
          .withStatusCode(ProxyResponse.STATUS_CODE_SERVER_ERROR)
          .withCorsHeaders()
          .withBody("{\"message\": \"" + message + "\"}")
          .build();
    }

    private GeneratePresignedUrlRequest signedUrlRequest(final String bucket, final String key) {
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += PRESIGNED_URL_VALIDITY;
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key).withExpiration(expiration);
        return request;
    }

    /**
     * Generate a pre-signed S3 URL for uploading a file.
     * @param bucket the S3 bucket name
     * @param key the S3 object key
     * @throws SdkClientException for problems pre-signing the request for the S3 resource
     * @return the pre-signed URL
     */
    protected URL signedPutUrl(final String bucket, final String key) throws SdkClientException {
        GeneratePresignedUrlRequest request = signedUrlRequest(bucket, key);
        request.withMethod(HttpMethod.PUT);
        URL url = getS3Client().generatePresignedUrl(request);
        return url;
    }

    /**
     * Check if given string is not null or empty.
     * @param s the string to check
     * @return true if not null or empty
     */
    protected boolean hasLength(final String s) {
        return s != null && !s.isEmpty();
    }

    /**
     * Check a required value. Throws {@link IllegalArgumentException} if null or empty.
     * @param value the value to check
     * @param label descriptive label for the value
     * @throws IllegalArgumentException if required value is null or empty
     */
    protected void requiredValue(final String value, final String label) throws IllegalArgumentException {
        if (!hasLength(value))
            throw new IllegalArgumentException("missing " + label);
    }

    /**
     * Log a message using the {@link com.amazonaws.services.lambda.runtime.LambdaLogger LambdaLogger} provided to this
     * Lambda function {@link com.amazonaws.services.lambda.runtime.RequestHandler RequestHandler}.
     * @param message the log message
     */
    protected void log(final String message) {
        this.logger.log(message);
    }

    /**
     * Serialize an object to a JSON string using a Jackson
     * {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper}.
     * @param object the object to serialize
     * @return the JSON string representation
     * @throws IOException if error processing JSON
     */
    protected String jsonify(final Object object) throws IOException {
        return getObjectMapper().writeValueAsString(object);
    }

    /**
     * Deserialize an object from a JSON string using a Jackson
     * {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper}.
     * @param value the JSON string representation
     * @param clazz the class for object
     * @param <X> the object type
     * @return the desired object
     * @throws IOException if error processing JSON
     */
    protected <X> X unjsonify(final String value, final Class<X> clazz) throws IOException {
        X object;
        try {
            object = getObjectMapper().readValue(value, clazz);
        } catch (IOException e) {
            log("unable to deserialize '" + clazz.getName() + "' from value: " + value);
            throw e;
        }
        return object;
    }
}

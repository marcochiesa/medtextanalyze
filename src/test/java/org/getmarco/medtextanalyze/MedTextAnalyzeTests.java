package org.getmarco.medtextanalyze;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.getmarco.medtextanalyze.functions.SignedUrlForUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

// CHECKSTYLE:OFF
import static io.restassured.RestAssured.*;
//import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
// CHECKSTYLE:ON

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class MedTextAnalyzeTests {

    private static final String HOST_PREFIX = "0h11k0ni5m";
    private static final String API_HOST = HOST_PREFIX + ".execute-api.us-east-1.amazonaws.com";
    private static final String API_ENDPOINT = "https://" + API_HOST + "/Prod/";
    private static final String UPLOAD_URL = API_ENDPOINT + "uploadurl";
    private static final int STATUS_OK = 200;
    private static final int PORT_SSL = 443;

    private ObjectMapper mapper;
    private CloseableHttpClient httpClient;

    /**
     * Pre-test setup.
     */
    @BeforeEach
    public void setup() {
        // Original - HttpClient/ObjectMapper
        mapper = new ObjectMapper();
        httpClient = HttpClientBuilder.create().build();

        // Rest-assured
        baseURI = API_ENDPOINT;
        port = PORT_SSL;
    }

    /**
     * Sample test.
     * @throws IOException for any network or response parsing errors
     */
    @Test
    @Order(1)
    public void testUploadUrl() throws IOException {
        // Get a pre-signed upload URL
        CloseableHttpResponse response = this.httpClient.execute(new HttpGet(UPLOAD_URL));
        assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);

        String content = EntityUtils.toString(response.getEntity());
        SignedUrlForUpload.Output output = mapper.readValue(content, SignedUrlForUpload.Output.class);
        assertNotNull(output.getBucket());
        assertFalse(output.getBucket().isEmpty());
        assertNotNull(output.getKey());
        assertFalse(output.getKey().isEmpty());
        assertNotNull(output.getLink());
        assertFalse(output.getLink().isEmpty());
    }

    /**
     * Sample test using rest-assured.
     */
    @Test
    @Order(2)
    public void testUploadUrl2() {
        SignedUrlForUpload.Output output = get("uploadurl")
          .then().assertThat().statusCode(STATUS_OK)
          .extract().response().as(SignedUrlForUpload.Output.class);
        assertNotNull(output.getBucket());
        assertFalse(output.getBucket().isEmpty());
        assertNotNull(output.getKey());
        assertFalse(output.getKey().isEmpty());
        assertNotNull(output.getLink());
        assertFalse(output.getLink().isEmpty());
    }
}

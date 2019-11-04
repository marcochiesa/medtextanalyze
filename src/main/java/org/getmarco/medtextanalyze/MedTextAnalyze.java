package org.getmarco.medtextanalyze;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

import lombok.extern.apachecommons.CommonsLog;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.getmarco.medtextanalyze.functions.EntitiesFromText;
import org.getmarco.medtextanalyze.functions.SignedUrlForUpload;
import org.getmarco.medtextanalyze.functions.TextFromImage;

@CommonsLog
public class MedTextAnalyze {

    private static final String HOST_PREFIX = "0h11k0ni5m";
    private static final String API_HOST = HOST_PREFIX + ".execute-api.us-east-1.amazonaws.com";
    private static final String API_ENDPOINT = "https://" + API_HOST + "/Prod/";
    private static final String UPLOAD_URL = API_ENDPOINT + "uploadurl";
    private static final String IMAGE_TEXT = API_ENDPOINT + "imagetext";
    private static final String TEXT_ENTITIES = API_ENDPOINT + "textentities";

    private final ObjectMapper mapper;
    private final CloseableHttpClient httpClient;

    /**
     * Class constructor.
     */
    public MedTextAnalyze() {
        mapper = new ObjectMapper();
        httpClient = HttpClientBuilder.create().build();
    }

    /**
     * Run sample process to upload an image, detect text in the image, and extract medical domain entities from the
     * text.
     * @param imageFile the image to analyze
     * @throws Exception for any network or response parsing errors
     */
    public void submitImageAndAnalyze(final File imageFile) throws Exception {
        // Get a pre-signed upload URL
        SignedUrlForUpload.Output uploadUrlOutput = getUploadUrl();
        String bucket = uploadUrlOutput.getBucket();
        String key = uploadUrlOutput.getKey();
        String uploadUrl = uploadUrlOutput.getLink();
        log.info(String.format("upload bucket: %s, key: %s, link: %s", bucket, key, uploadUrl));

        // Upload a sample image to S3
        HttpResponse uploadFileResponse = uploadFile(imageFile, uploadUrl);
        log.info("upload put response status: " + uploadFileResponse.getStatusLine().getStatusCode());

        // Get text contained in the image
        TextFromImage.Output textFromImageOutput = pullTextFromImage(bucket, key);
        log.info("image text: " + textFromImageOutput.getText().replaceAll("\\n", " "));

        // Get entities for text
        EntitiesFromText.Output entitiesFromTextOutput = findTextEntities(textFromImageOutput.getText());
        log.info("entities: " + entitiesFromTextOutput.getText());
    }

    private SignedUrlForUpload.Output getUploadUrl() throws IOException, ParseException {
        CloseableHttpResponse response = this.httpClient.execute(new HttpGet(UPLOAD_URL));
        String responseContent = EntityUtils.toString(response.getEntity());
        SignedUrlForUpload.Output output = fromJson(responseContent, SignedUrlForUpload.Output.class);
        return output;
    }

    private HttpResponse uploadFile(final File file, final String url) throws IOException {
        HttpPut put = new HttpPut(url);
        put.setEntity(new FileEntity(file));
        CloseableHttpResponse response = this.httpClient.execute(put);
        return response;
    }

    private TextFromImage.Output pullTextFromImage(final String bucket, final String key) throws IOException {
        HttpPost post = new HttpPost(IMAGE_TEXT);
        String input = toJson(new TextFromImage.Input(bucket, key));
        post.setEntity(new StringEntity(input));
        CloseableHttpResponse response = this.httpClient.execute(post);
        String responseContent = EntityUtils.toString(response.getEntity());
        TextFromImage.Output output = fromJson(responseContent, TextFromImage.Output.class);
        return output;
    }

    private EntitiesFromText.Output findTextEntities(final String text) throws IOException {
        EntitiesFromText.Output textEntitiesContent;
        HttpPost post = new HttpPost(TEXT_ENTITIES);
        String input = toJson(new EntitiesFromText.Input(text));
        post.setEntity(new StringEntity(input));
        CloseableHttpResponse response = this.httpClient.execute(post);
        String responseContent = EntityUtils.toString(response.getEntity());
        EntitiesFromText.Output output = fromJson(responseContent, EntitiesFromText.Output.class);
        return output;
    }

    private String toJson(final Object object) throws IOException {
        return this.mapper.writeValueAsString(object);
    }

    private <X> X fromJson(final String value, final Class<X> clazz) throws IOException {
        X object;
        try {
            object = this.mapper.readValue(value, clazz);
        } catch (IOException e) {
            log.error("unable to deserialize '" + clazz.getName() + "' from value: " + value);
            throw e;
        }
        return object;
    }

    /**
     * Program entry point.
     *
     * @param args program arguments
     */
    public static void main(final String[] args) {
        CloseableHttpResponse response;

        String uploadFilePath = "fax/image-2.png";
        MedTextAnalyze medText = new MedTextAnalyze();
        try {
            medText.submitImageAndAnalyze(new File(uploadFilePath));
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("finished");
    }

//    /**
//     * Adds AWS Comprehend Medical client to the
//     * {@link org.springframework.context.ApplicationContext ApplicationContext}.
//     *
//     * @return the aws comprehend medical client
//     */
//    @Bean
//    public AWSComprehendMedical comprehendClient() {
//        return AWSComprehendMedicalClient.builder()
//          .withCredentials(new DefaultAWSCredentialsProviderChain())
//          .withRegion(Regions.US_EAST_1).build();
//    }
//
//    /**
//     * Adds AWS Textract client to the {@link org.springframework.context.ApplicationContext ApplicationContext}.
//     *
//     * @return the aws textract client
//     */
//    @Bean
//    public AmazonTextract textractClient() {
//        String region = config.getRegion();
//        if (!StringUtils.hasText(region)) {
//            throw new IllegalStateException("missing aws region");
//        }
//        String accessKeyId = config.getAccessKeyId();
//        String secretAccessKey = config.getSecretAccessKey();
//        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey)) {
//            AWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
//            return AmazonTextractClientBuilder.standard()
//              .withCredentials(new AWSStaticCredentialsProvider(credentials))
//              .withRegion(region).build();
//        }
//
//        return AmazonTextractClientBuilder.standard().withRegion(region).build();
//    }
//
//    /**
//     * Adds an AWS S3 client to the {@link org.springframework.context.ApplicationContext ApplicationContext}.
//     *
//     * @return the aws S3 client
//     */
//    @Bean
//    public AmazonS3 s3Client() {
//        String region = config.getRegion();
//        if (!StringUtils.hasText(region)) {
//            throw new IllegalStateException("missing aws region");
//        }
//        String accessKeyId = config.getAccessKeyId();
//        String secretAccessKey = config.getSecretAccessKey();
//        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey)) {
//            AWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
//            return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
//              .withRegion(region).build();
//        }
//
//        return AmazonS3ClientBuilder.standard().withRegion(region).build();
//    }
//
//    /**
//     * Adds a Jackson {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper} to the
//     * {@link org.springframework.context.ApplicationContext ApplicationContext}.
//     *
//     * @return the object mapper
//     */
//    @Bean
//    @Primary
//    public ObjectMapper objectMapper() {
//        return new ObjectMapper();
//    }
}

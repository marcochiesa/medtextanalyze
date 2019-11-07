package org.getmarco.medtextanalyze;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.getmarco.medtextanalyze.functions.TextFromPdf;

@CommonsLog
public class MedTextAnalyze {

    private static final String HOST_PREFIX = "0h11k0ni5m";
    private static final String API_HOST = HOST_PREFIX + ".execute-api.us-east-1.amazonaws.com";
    private static final String API_ENDPOINT = "https://" + API_HOST + "/Prod/";
    private static final String UPLOAD_URL = API_ENDPOINT + "uploadurl";
    private static final String IMAGE_TEXT = API_ENDPOINT + "imagetext";
    private static final String PDF_TEXT = API_ENDPOINT + "pdftext";
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
        SignedUrlForUpload.Output uploadInfo = uploadFile(imageFile);

        // Get text contained in the image
        TextFromImage.Output textFromImageOutput = pullTextFromImage(uploadInfo.getBucket(), uploadInfo.getKey());
        log.info("image text: " + textFromImageOutput.getText().replaceAll("\\n", " "));

        // Get entities for text
        EntitiesFromText.Output entitiesFromTextOutput = findTextEntities(textFromImageOutput.getText());
        log.info("entities: " + entitiesFromTextOutput.getText());
    }

    /**
     * Run sample process to upload a PDF, detect text from it, and extract medical domain entities from the
     * text.
     * @param pdfFile the image to analyze
     * @throws Exception for any network or response parsing errors
     */
    public void submitPdfAndAnalyze(final File pdfFile) throws Exception {
        SignedUrlForUpload.Output uploadInfo = uploadFile(pdfFile);

        // Get text contained in the image
        TextFromPdf.Output textFromPdfOutput = pullTextFromPdf(uploadInfo.getBucket(), uploadInfo.getKey());
        log.info("pdf text: " + textFromPdfOutput.getText().replaceAll("\\n", " "));

        // Get entities for text
        EntitiesFromText.Output entitiesFromTextOutput = findTextEntities(textFromPdfOutput.getText());
        log.info("entities: " + entitiesFromTextOutput.getText());
    }

    /**
     * Run sample process to upload a PDF, detect text from it, and get a regex match.
     * @param pdfFile the image to analyze
     * @param pattern regex pattern to match with
     * @throws Exception for any network or response parsing errors
     * @return matcher on the detected text
     */
    public Matcher submitPdfAndMatchText(final File pdfFile, final Pattern pattern) throws Exception {
        SignedUrlForUpload.Output uploadInfo = uploadFile(pdfFile);

        // Get text contained in the image
        TextFromPdf.Output textFromPdfOutput = pullTextFromPdf(uploadInfo.getBucket(), uploadInfo.getKey());
        log.info("pdf text: " + textFromPdfOutput.getText().replaceAll("\\n", " "));

        Matcher matcher = pattern.matcher(textFromPdfOutput.getText());
        return matcher;
    }

    private SignedUrlForUpload.Output uploadFile(final File imageFile) throws Exception {
        // Get a pre-signed upload URL
        SignedUrlForUpload.Output uploadUrlOutput = getUploadUrl();
        String bucket = uploadUrlOutput.getBucket();
        String key = uploadUrlOutput.getKey();
        String uploadUrl = uploadUrlOutput.getLink();
        log.info(String.format("upload bucket: %s, key: %s, link: %s", bucket, key, uploadUrl));

        // Upload a sample image to S3
        HttpResponse uploadFileResponse = doUploadFile(imageFile, uploadUrl);
        log.info("upload put response status: " + uploadFileResponse.getStatusLine().getStatusCode());

        return uploadUrlOutput;
    }

    private SignedUrlForUpload.Output getUploadUrl() throws IOException, ParseException {
        CloseableHttpResponse response = this.httpClient.execute(new HttpGet(UPLOAD_URL));
        String responseContent = EntityUtils.toString(response.getEntity());
        SignedUrlForUpload.Output output = fromJson(responseContent, SignedUrlForUpload.Output.class);
        return output;
    }

    private HttpResponse doUploadFile(final File file, final String url) throws IOException {
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

    private TextFromPdf.Output pullTextFromPdf(final String bucket, final String key) throws IOException {
        HttpPost post = new HttpPost(PDF_TEXT);
        String input = toJson(new TextFromPdf.Input(bucket, key));
        post.setEntity(new StringEntity(input));
        CloseableHttpResponse response = this.httpClient.execute(post);
        String responseContent = EntityUtils.toString(response.getEntity());
        TextFromPdf.Output output = fromJson(responseContent, TextFromPdf.Output.class);
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
        String filePath = "fax/image-2.png";
        MedTextAnalyze medText = new MedTextAnalyze();
        try {
            medText.submitImageAndAnalyze(new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
        }

        filePath = "fax/test.pdf";
        try {
            medText.submitPdfAndAnalyze(new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("finished");
    }
}

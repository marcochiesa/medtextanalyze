package org.getmarco.medtextanalyze.support;

import com.amazonaws.services.comprehendmedical.AWSComprehendMedical;
import com.amazonaws.services.comprehendmedical.model.DetectEntitiesRequest;
import com.amazonaws.services.comprehendmedical.model.DetectEntitiesResult;
import com.amazonaws.services.comprehendmedical.model.Entity;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.model.Block;
import com.amazonaws.services.textract.model.DetectDocumentTextRequest;
import com.amazonaws.services.textract.model.DetectDocumentTextResult;
import com.amazonaws.services.textract.model.Document;
import com.amazonaws.services.textract.model.S3Object;
import com.amazonaws.util.IOUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

public final class Analyzer {
    private static final int IMAGE_RESOLUTION_DPI = 300;

    @Getter
    @Setter
    private AmazonTextract textractClient;

    @Getter
    @Setter
    private AWSComprehendMedical comprehendClient;

    private String detectText(final DetectDocumentTextRequest request) {
        DetectDocumentTextResult result = textractClient.detectDocumentText(request);
        StringBuilder s = new StringBuilder();
        for (Block block : result.getBlocks()) {
            if ("LINE".equals(block.getBlockType())) {
                s.append(block.getText()).append("\n");
            }
        }
        return s.toString();
    }

    /**
     * Use the AWS Textract detect document text API to process an image stored in S3.
     * @param bucket the S3 bucket
     * @param name the S3 object key
     * @return text contents detected in the image
     * @throws IOException if the file cannot be opened or read from
     */
    public String detectTextImageS3(final String bucket, final String name) {
        S3Object s3Object = new S3Object().withBucket(bucket).withName(name);
        DetectDocumentTextRequest request = new DetectDocumentTextRequest()
          .withDocument(new Document().withS3Object(s3Object));
        return detectText(request);
    }

    private String detectText(final ByteBuffer imageBytes) {
        DetectDocumentTextRequest request = new DetectDocumentTextRequest()
          .withDocument(new Document().withBytes(imageBytes));
        return detectText(request);
    }

    /**
     * Use the AWS Textract detect document text API to process an image.
     * @param filename the local file path for the image
     * @return text contents detected in the image
     * @throws IOException if the file cannot be opened or read from
     */
    public String detectTextImage(final String filename) throws IOException {
        File file = new File(filename);
        ByteBuffer imageBytes = null;
        try (InputStream inputStream = new FileInputStream(file)) {
            imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }
        return detectText(imageBytes);
    }

    /**
     * Use the AWS Textract detect document text API to process a PDF.
     * @param filename the local file path for the PDF file
     * @return text contents detected in the PDF
     */
    public String detectTextPDF(final String filename) {
        BufferedImage bim = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        ByteBuffer imageBytes = null;
        StringBuilder s = new StringBuilder();
        try (PDDocument document = PDDocument.load(new File(filename))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                bim = pdfRenderer.renderImageWithDPI(page, IMAGE_RESOLUTION_DPI, ImageType.RGB);
//              String fileName = "fax/image-" + page + ".png";
//              ImageIOUtil.writeImage(bim, fileName, IMAGE_RESOLUTION_DPI);
                byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIOUtil.writeImage(bim, "png", byteArrayOutputStream);
                byteArrayOutputStream.flush();
                imageBytes = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
                s.append(detectText(imageBytes));
            }
        } catch (IOException e) {
            // to-do
        }
        return s.toString();
    }

    /**
     * Identify medical domain entities in the give text.
     * @param text the text to analyze
     * @return report of identified entities
     */
    public String getEntities(final String text) {
        DetectEntitiesRequest comprehendRequest = new DetectEntitiesRequest();
        comprehendRequest.setText(text);

        DetectEntitiesResult comprehendResult = comprehendClient.detectEntities(comprehendRequest);
        return comprehendResult.getEntities().stream().map(this::printEntity).collect(Collectors.joining(""));
    }

    private String printEntity(final Entity entity) {
        StringBuilder s = new StringBuilder();
        if (entity.getCategory().equals("MEDICATION")) {
            s.append(printMedication(entity));
        } else if (entity.getCategory().equals("PROTECTED_HEALTH_INFORMATION") && entity.getType().equals("NAME")) {
            s.append(printPhi(entity));
        } else {
            return "";
        }
        s.append("-----").append("\n");
        return s.toString();
    }

    private String printPhi(final Entity entity) {
        return "PHI - " + entity.getType().toLowerCase() + ": " + entity.getText() + "\n";
    }

    private String printMedication(final Entity entity) {
        StringBuilder s = new StringBuilder();
        s.append("Medication: " + entity.getText() + "\n");
        Optional.ofNullable(entity.getAttributes()).map(Collection::stream).orElseGet(Stream::empty)
          .map(x -> x.getType().toLowerCase() + ": " + x.getText() + "\n").forEach(s::append);
        return s.toString();
    }
}

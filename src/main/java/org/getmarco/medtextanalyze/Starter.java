package org.getmarco.medtextanalyze;

import com.amazonaws.services.comprehendmedical.AWSComprehendMedical;
import com.amazonaws.services.comprehendmedical.model.Attribute;
import com.amazonaws.services.comprehendmedical.model.DetectEntitiesRequest;
import com.amazonaws.services.comprehendmedical.model.DetectEntitiesResult;
import com.amazonaws.services.comprehendmedical.model.Entity;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.model.Block;
import com.amazonaws.services.textract.model.DetectDocumentTextRequest;
import com.amazonaws.services.textract.model.DetectDocumentTextResult;
import com.amazonaws.services.textract.model.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

@Component
public class Starter implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(Starter.class);
    private static final int IMAGE_RESOLUTION_DPI = 300;

    @Autowired
    private AWSComprehendMedical comprehendClient;
    @Autowired
    private AmazonTextract textractClient;

    /**
     * Sample code to try out AWS Textract and Comprehend Medical services.
     *
     * @param args incoming main method arguments
     * @throws Exception on error
     */
    @Override
    public void run(final String... args) throws Exception {
        try (PDDocument document = PDDocument.load(new File("fax/FXd6e0b90ec40134add6c5e8fc32fc48a6.orig.pdf"))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, IMAGE_RESOLUTION_DPI, ImageType.RGB);
                String fileName = "fax/image-" + page + ".png";
                ImageIOUtil.writeImage(bim, fileName, IMAGE_RESOLUTION_DPI);
            }
        } catch (IOException e) {
            LOG.error("Exception while trying to create pdf document - " + e);
        }

        File file = new File("fax/image-2.png");
        ByteBuffer buf = ByteBuffer.allocateDirect((int) file.length());
        InputStream is = new FileInputStream(file);
        int b;
        int count = 0;
        while ((b = is.read()) != -1) {
            count++;
            buf.put((byte) b);
        }
        LOG.info("count: " + count);
        buf.flip();
        DetectDocumentTextRequest textRequest = new DetectDocumentTextRequest().withDocument(new Document()
          .withBytes(buf));
        DetectDocumentTextResult textResult = textractClient.detectDocumentText(textRequest);
        List<Block> blocks = textResult.getBlocks();
        StringBuilder s = new StringBuilder();
        for (Block block : blocks) {
            if ("LINE".equals(block.getBlockType())) {
                s.append(block.getText()).append("\n");
            }
        }
        String extractedText = s.toString();
        //LOG.info(extractedText);

        DetectEntitiesRequest comprehendRequest = new DetectEntitiesRequest();
        comprehendRequest.setText(extractedText);

        DetectEntitiesResult comprehendResult = comprehendClient.detectEntities(comprehendRequest);
        //comprehendResult.getEntities().forEach(System.out::println);

        List<Entity> entities = comprehendResult.getEntities();
        for (Entity entity : entities) {
            if (entity.getCategory().equals("MEDICATION")) {
                printMedication(entity);
            } else if (entity.getCategory().equals("PROTECTED_HEALTH_INFORMATION") && entity.getType().equals("NAME")) {
                printPhi(entity);
            } else {
                continue;
            }
            LOG.info("-----");
        }
    }

    private void printPhi(final Entity entity) {
        LOG.info("PHI - " + StringUtils.capitalize(entity.getType().toLowerCase()) + ": " + entity.getText());
    }

    private void printMedication(final Entity entity) {
        LOG.info("Medication: " + entity.getText());
        List<Attribute> attributes = entity.getAttributes();
        if (attributes != null) {
            for (Attribute attribute : attributes) {
                LOG.info(StringUtils.capitalize(attribute.getType().toLowerCase()) + ": "
                  + attribute.getText());
            }
        }
    }
}

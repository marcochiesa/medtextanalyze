package org.getmarco.medtextanalyze;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.comprehendmedical.AWSComprehendMedical;
import com.amazonaws.services.comprehendmedical.AWSComprehendMedicalClient;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class MedtextanalyzeApplication {

    @Autowired
    private Config config;

    /**
     * Program entry point.
     *
     * @param args program arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(MedtextanalyzeApplication.class, args);
    }

    /**
     * Adds AWS Comprehend Medical client to the
     * {@link org.springframework.context.ApplicationContext ApplicationContext}.
     *
     * @return the aws comprehend medical client
     */
    @Bean
    public AWSComprehendMedical comprehendClient() {
        return AWSComprehendMedicalClient.builder()
          .withCredentials(new DefaultAWSCredentialsProviderChain())
          .withRegion(Regions.US_EAST_1).build();
    }

    /**
     * Adds AWS Textract client to the {@link org.springframework.context.ApplicationContext ApplicationContext}.
     *
     * @return the aws textract client
     */
    @Bean
    public AmazonTextract textractClient() {
        String region = config.getRegion();
        if (!StringUtils.hasText(region)) {
            throw new IllegalStateException("missing aws region");
        }
        String accessKeyId = config.getAccessKeyId();
        String secretAccessKey = config.getSecretAccessKey();
        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey)) {
            AWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
            return AmazonTextractClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
              .withRegion(region).build();
        }

        return AmazonTextractClientBuilder.standard().withRegion(region).build();
    }
}

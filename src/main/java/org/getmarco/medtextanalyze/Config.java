package org.getmarco.medtextanalyze;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "analyzer")
public class Config {
    private String accessKeyId;
    private String secretAccessKey;
    private String region;
}

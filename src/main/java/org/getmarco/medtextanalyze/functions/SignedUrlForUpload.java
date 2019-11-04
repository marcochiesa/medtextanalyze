package org.getmarco.medtextanalyze.functions;

import java.net.URL;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.getmarco.medtextanalyze.support.ProxyRequest;

public class SignedUrlForUpload extends FunctionSupport {

    /**
     * Generate response body content for this function.
     * @param request the API Gateway proxy request
     * @return the body content for the function response
     */
    @Override
    public String createBody(final ProxyRequest request) throws Exception {
        String bucket = getUploadBucket();
        String key = UUID.randomUUID().toString();
        log("creating pre-signed URL for upload to bucket '" + bucket + "' and key '" + key + "'");
        URL url = signedPutUrl(bucket, key);
        log("created url: " + url.toString());
        return jsonify(new Output(bucket, key, url.toString()));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output extends FunctionOutput {
        private String bucket;
        private String key;
        private String link;
    }
}

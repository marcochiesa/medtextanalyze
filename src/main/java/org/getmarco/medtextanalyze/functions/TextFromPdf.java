package org.getmarco.medtextanalyze.functions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.getmarco.medtextanalyze.support.ProxyRequest;

public class TextFromPdf extends FunctionSupport {

    /**
     * Constructor.
     */
    public TextFromPdf() {
        getAnalyzer().setTextractClient(getTextractClient());
    }

    /**
     * Generate response body content for this function.
     * @param request the API Gateway proxy request
     * @return the body content for the function response
     */
    @Override
    protected String createBody(final ProxyRequest request) throws Exception {
        Input input = unjsonify(request.getBody(), Input.class);
        requiredValue(input.getBucket(), "bucket name");
        requiredValue(input.getKey(), "object key");

        log("detect text for pdf in bucket '" + input.getBucket() + "' and key '" + input.getKey() + "'");
        String text = getAnalyzer().detectTextPdf2(input.getBucket(), input.getKey());
        return jsonify(new Output(text));
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output extends FunctionOutput {
        private String text;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String bucket;
        private String key;
    }
}

package org.getmarco.medtextanalyze.functions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.getmarco.medtextanalyze.support.ProxyRequest;

public class EntitiesFromText extends FunctionSupport {

    /**
     * Constructor.
     */
    public EntitiesFromText() {
        getAnalyzer().setComprehendClient(getComprehendMedicalClient());
    }

    /**
     * Generate response body content for this function.
     * @param request the API Gateway proxy request
     * @return the body content for the function response
     */
    @Override
    public String createBody(final ProxyRequest request) throws Exception {
        Input input = unjsonify(request.getBody(), Input.class);
        requiredValue(input.getText(), "text input");
        log("get entities for input text: " + input.getText());
        String text = getAnalyzer().getEntities(input.getText());
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
        private String text;
    }
}

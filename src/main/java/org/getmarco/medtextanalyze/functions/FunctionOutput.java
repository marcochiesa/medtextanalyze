package org.getmarco.medtextanalyze.functions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionOutput {
    @SuppressWarnings("checkstyle:javadocvariable")
    public enum Status { SUCCESS, FAILURE }
    private Status status;
    // API Gateway timeout will return JSON with just message property
    private String message;

    /**
     * Constructor defaults status to success.
     */
    public FunctionOutput() {
        this.status = Status.SUCCESS;
    }
}

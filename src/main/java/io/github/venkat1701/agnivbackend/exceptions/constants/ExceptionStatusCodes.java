package io.github.venkat1701.agnivbackend.exceptions.constants;

public enum ExceptionStatusCodes {

    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    BAD_REQUEST(400, "Bad Request"),
    NOT_FOUND(404, "Not Found"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    CONFLICT(409, "Conflict"),
    PRECONDITION_FAILED(412, "Precondition Failed"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    BAD_GATEWAY(502, "Bad Gateway"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    OLLAMA_TIMEOUT(524, "LLM Timeout");

    private int code;
    private String name;
    ExceptionStatusCodes(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}

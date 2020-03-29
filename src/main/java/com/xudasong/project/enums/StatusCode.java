package com.xudasong.project.enums;

public enum StatusCode {

    SUCCESS(200,"成功"),
    FAIL(500,"失败"),
    PARAM_INVALID(400,"参数不合法");

    private final int code;
    private final String message;

    private StatusCode(int code, String message){
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

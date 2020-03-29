package com.xudasong.project.response;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xudasong.project.enums.StatusCode;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> implements Serializable {

    @JsonProperty("code")
    @ApiModelProperty(value = "返回码",required = true)
    protected int code;

    @JsonProperty("message")
    @ApiModelProperty(value = "消息",required = true)
    protected String message;

    @JsonProperty("data")
    @ApiModelProperty(value = "返回数据",required = true)
    protected T data;

    public static <T> CommonResponse<T> success(){
        return new CommonResponse<>(StatusCode.SUCCESS.getCode(),StatusCode.SUCCESS.getMessage(),null);
    }

    public static <T> CommonResponse<T> success(String message){
        return new CommonResponse<>(StatusCode.SUCCESS.getCode(),message,null);
    }

    public static <T> CommonResponse<T> success(T data){
        return new CommonResponse<>(StatusCode.SUCCESS.getCode(),StatusCode.SUCCESS.getMessage(),data);
    }

    public static <T> CommonResponse<T> success(String message,T data){
        return new CommonResponse<>(StatusCode.SUCCESS.getCode(),message,data);
    }

    public static <T> CommonResponse<Void> fail(){
        return new CommonResponse<>(StatusCode.FAIL.getCode(),StatusCode.FAIL.getMessage(),null);
    }

    public static <T> CommonResponse<T> fail(int code,String message){
        return new CommonResponse<>(code,message,null);
    }

    public static <T> CommonResponse<T> fail(int code,String message,T data){
        return new CommonResponse<>(code,message,data);
    }

    /** 参数不合法 */
    public static <T> CommonResponse<T> invaildParam(String message){
        return new CommonResponse<>(StatusCode.PARAM_INVALID.getCode(),message,null);
    }

    /** 判断当前结果是否成功返回 */
    public boolean isSuccess(){
        return StatusCode.SUCCESS.getCode() == this.code;
    }

    @Override
    public String toString(){
        return "{" +
                "\"code\":" + code +
                ",\"message\":\"" + message + '\"' +
                (null != data ? (",data:" + JSONObject.toJSONString(data)) : "") +
                '}';
    }

}

package com.xudasong.project.utils;

import com.xudasong.project.model.GateWayProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class RequestResolveUtils {

    @Autowired
    private GateWayProperties gateWayProperties;

    public void setGateWayProperties(GateWayProperties gateWayProperties){
        this.gateWayProperties = gateWayProperties;
    }

    private static RequestResolveUtils requestResolveUtils;

    @PostConstruct
    public void init(){
        requestResolveUtils = this;
        requestResolveUtils.gateWayProperties = this.gateWayProperties;
    }

    /**
     * 获取请求token
     * @param request
     * @return
     */
    public static String extractToken(ServerHttpRequest request){
        String token = null;
        List<String> tokenFromHeaders = request.getHeaders().get("token");
        if (!CollectionUtils.isEmpty(tokenFromHeaders) && tokenFromHeaders.size() >0 ){
            token = tokenFromHeaders.get(0);
        }
        return token;
    }

    /**
     * 获取请求体内容
     * @param request
     * @return
     */
    public static String resolveBodyFromRequest(ServerHttpRequest request){
        Flux<DataBuffer> body = request.getBody();
        StringBuilder sb = new StringBuilder();
        body.subscribe(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            String bodyStr = new String(bytes, StandardCharsets.UTF_8);
            sb.append(bodyStr);
        });
        return sb.toString();
    }

}

package com.xudasong.project.service;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

public interface SecurityFilter {

    /**
     * 转发地址
     * @param request
     * @param response
     * @param builder
     * @return
     */
    Mono<Void> forward(ServerHttpRequest request, ServerHttpResponse response, ServerHttpRequest.Builder builder);

    /**
     * 是否过滤掉权限校验
     * @param request
     * @return
     */
    boolean filterCheck(ServerHttpRequest request);

}

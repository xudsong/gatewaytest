package com.xudasong.project.config;

import com.xudasong.project.model.GateWayProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.xml.ws.WebFault;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 跨域
 */
@Configuration
@Slf4j
public class RouteConfiguration {

    private static final String ALLOWED_HEADERS = "x-requested-with, authorization, content-type, authority-user,credential, x-xsrf-token,token,username,client";
    private static final String MAX_AGE = "18000L";

    @Autowired
    private GateWayProperties gateWayProperties;

    @Value("${spring.profiles.active}")
    private String env;

    @Bean
    public WebFilter corsFilter(){
        return (ServerWebExchange ctx, WebFilterChain chain) -> {
            ServerHttpRequest request = ctx.getRequest();
            ServerHttpResponse response = ctx.getResponse();

            if (CorsUtils.isCorsRequest(request)){
                HttpHeaders headers = response.getHeaders();
                String origin = request.getHeaders().getOrigin();
                log.info("客户端请求的域名origin为：{}",origin);
                headers.add("Access-Control-Max-Age",MAX_AGE);
                headers.add("Access-Control-Allow-Credentials","true");
                headers.add("Access-Control-Allow-Origin",origin);

                if (request.getMethod() == HttpMethod.OPTIONS){
                    headers.add("Access-Control-Allow-Methods", request.getHeaders().getAccessControlRequestMethod().name());
                    String exposeHeaders = request.getHeaders().getAccessControlRequestHeaders().stream().collect(Collectors.joining(","));
                    log.info("允许暴露的请求头为：{}",exposeHeaders);
                    if (StringUtils.isNotEmpty(exposeHeaders)){
                        headers.add("Access-Control-Allow-Headers",ALLOWED_HEADERS+","+exposeHeaders);
                        headers.add("Access-Control-Expose-Headers",exposeHeaders);
                    }
                    response.setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }else {
                    headers.add("Access-Control-Allow-Methods",request.getMethod().name());
                    headers.add("Access-Control-Allow-Headers",ALLOWED_HEADERS);
                    headers.add("Access-Control-Expose-Headers",ALLOWED_HEADERS);
                }
            }
            return chain.filter(ctx);
        };
    }

    @Bean
    public RouteDefinitionLocator discoveryClientRouteDefinitionLocator(DiscoveryClient discoveryClient, DiscoveryLocatorProperties properties){
        return new DiscoveryClientRouteDefinitionLocator(discoveryClient,properties);
    }

}

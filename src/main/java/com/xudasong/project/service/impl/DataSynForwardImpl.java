package com.xudasong.project.service.impl;

import com.google.common.collect.Lists;
import com.xudasong.project.service.SecurityFilter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.management.Query;
import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DataSynForwardImpl implements SecurityFilter {

    private List<String> allowIpList;
    private String localIp;
    @Value("${forward.inner.route}")
    private String innerRoute;
    private URI forwardTo;
    private List<String> forwardUrls;

    private static final String GATEWAY_KEY = "gateway";

    @Value("${forward.to}")
    private void loadForwardTo(String forwardToStr){
        if (StringUtils.isNotBlank(forwardToStr)){
            forwardTo = URI.create(forwardToStr);
        }
    }

    @Value("${forward.needurls}")
    private void loadForwardUrl(String forwardUrl){
        forwardUrls = Lists.newArrayList();
        if (StringUtils.isNotBlank(forwardUrl)){
            forwardUrls.addAll(Arrays.asList(forwardUrl.split(",")));
        }
    }

    @Value("${nolimit.iplist}")
    private void loadAllowIpList(String allowIps){
        allowIpList = Lists.newArrayList();
        if (StringUtils.isNotBlank(allowIps)){
            allowIpList.addAll(Arrays.asList(allowIps.split(",")));
        }
    }

    @PostConstruct
    public void init(){
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        }catch (Exception e){
            throw new RuntimeException("获取地址Ip异常");
        }
        localIp = inetAddress.getHostAddress();
    }

    /**
     * 转发地址
     * @param request
     * @param response
     * @param builder
     * @return
     */
    @Override
    public Mono<Void> forward(ServerHttpRequest request, ServerHttpResponse response, ServerHttpRequest.Builder builder) {
        URI reqUri = request.getURI();
        String urlStr = buildUrl(innerRoute,reqUri.getPath(), reqUri.getQuery());
        //如果没设置值，直接忽略
        if (Objects.isNull(forwardTo) || CollectionUtils.isEmpty(forwardUrls) || StringUtils.isEmpty(urlStr)){
            return null;
        }
        AntPathMatcher pathMatcher = new AntPathMatcher();
        String location = reqUri.getPath();
        for (String forwardUri:forwardUrls){
            if (pathMatcher.match(forwardUri,location)){
                log.info("[数据同步]{}需要转发到请求地址：{}",location,forwardTo);
                return buildAndSend(request,response,urlStr,builder);
            }
        }
        return null;
    }

    @Override
    public boolean filterCheck(ServerHttpRequest request) {
        //如果没设置值，直接忽略
        if (CollectionUtils.isEmpty(allowIpList)){
            return false;
        }
        String currIp = request.getRemoteAddress().getAddress().getHostAddress();
        log.info("[数据同步] 获取当前ip:{}",currIp);
        return allowIpList.contains(currIp);
    }

    private String buildUrl(String url,String path, String query){
        StringBuilder urlStr = new StringBuilder(url);
        if (StringUtils.isEmpty(url)){
            return null;
        }
        //加入上下文
        if (StringUtils.isNotBlank(path)){
            if (url.endsWith("/") && path.startsWith("/")){
                urlStr.append(path.substring(1));
            }else if (!url.endsWith("/") && !path.startsWith("/")){
                urlStr.append("/").append(path);
            }else {
                urlStr.append(path);
            }
        }
        //加入请求参数
        if (StringUtils.isNotBlank(query)){
            urlStr.append("?").append(query);
        }
        return urlStr.toString();
    }

    private Mono<Void> buildAndSend(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,String urlStr,ServerHttpRequest.Builder builder){
        MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap<>();
        multiValueMap.add("targetMethod",request.getMethod().name().toUpperCase());
        multiValueMap.add("targetUrl",urlStr);
        if (Objects.nonNull(builder)){
            HttpHeaders headers = builder.build().getHeaders();
            Iterator<String> iter = headers.keySet().iterator();
            String key;
            while (iter.hasNext()){
                key = iter.next();
                multiValueMap.put(key,headers.get(key));
            }
        }
        multiValueMap.put(GATEWAY_KEY,Lists.newArrayList(localIp));
        //设置网关ip
        WebClient.RequestBodySpec requestBodySpec = WebClient.create(forwardTo.toString()).post().headers((h)->{h.addAll(multiValueMap);});
        log.info("[数据同步]正在转发请求到 {}",forwardTo.toString());

        WebClient.ResponseSpec responseSpec = null;
        log.info("[数据同步]获取当前的请求body");
        if (!"GET".equals(request.getMethod().name().toUpperCase())){
            BodyInserter<Flux<DataBuffer>, ReactiveHttpOutputMessage> inserter = null;
            Flux<DataBuffer> body = request.getBody();
            if (Objects.nonNull(body)){
                log.info("[数据同步]获取当前的请求body");
                inserter = BodyInserters.fromObject(body);
                responseSpec = requestBodySpec.body(inserter).retrieve();
            }
        }else {
            log.info("[数据同步]请求不追加body");
            responseSpec = requestBodySpec.retrieve();
        }

        //把结果转换成过滤器用的
        return response.writeWith(responseSpec.bodyToMono(DataBuffer.class));
    }

}

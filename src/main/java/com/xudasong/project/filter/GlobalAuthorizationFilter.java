package com.xudasong.project.filter;

import com.alibaba.fastjson.JSON;
import com.sun.xml.internal.bind.api.impl.NameConverter;
import com.xudasong.project.enums.StatusCode;
import com.xudasong.project.filter.databuffer.PartnerServerWebExchangeDecorator;
import com.xudasong.project.model.GateWayProperties;
import com.xudasong.project.response.CommonResponse;
import com.xudasong.project.service.SecurityFilter;
import com.xudasong.project.service.impl.RedisServiceImpl;
import com.xudasong.project.utils.PathUtils;
import com.xudasong.project.utils.RequestResolveUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.omg.CORBA.PRIVATE_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class GlobalAuthorizationFilter implements GlobalFilter, Ordered {

    @Autowired(required = false)
    private List<SecurityFilter>  securityFilters;

    @Autowired
    private GateWayProperties gateWayProperties;

    @Autowired
    private RedisServiceImpl redisService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //设置请求body内容多次获取
        PartnerServerWebExchangeDecorator exchangeDecorator = new PartnerServerWebExchangeDecorator(exchange);
        ServerHttpRequest request = exchangeDecorator.getRequest();
        ServerHttpResponse response = exchangeDecorator.getResponse();

        //获取请求路径和方法
        String uri = request.getURI().toString();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();
        log.info("当前请求的方法：{}，请求的资源路径url为：{}",method.name(),uri);

        try{
            //是否资源库匹配到结果
            boolean resourceMatch = false;
            //是否在本地配置文件匹配到结果
            boolean propertyMatch = resourceMatch ? false: PathUtils.checkWhite(path,method);
            //资源库匹配不到且本地匹配也不存在属于黑名单
            if (!resourceMatch && !propertyMatch){
                log.info("请求的资源路径非法；当前请求的方法为：{}，请求的路径为：{}",method.name(),uri);
                Mono<Void> error = errorHandling(response, 305, "请求的资源路径非法");
                return error;
            }

            //匹配是否需要鉴权认证
            boolean release = false;
            boolean needAuth = true;
            if (propertyMatch){
                release = PathUtils.checkRelease(path, method);
                needAuth = PathUtils.checkAuth(path,method);
            }

            //1.获取直接放行的请求
            //追加部分ip也直接放行 add new
            if (!CollectionUtils.isEmpty(securityFilters)){
                for (SecurityFilter securityFilter : securityFilters){
                    if (securityFilter.filterCheck(request)){
                        return chain.filter(exchangeDecorator);
                    }
                }
            }

            if (release){

                //转发之前需要追加地址判断，是否请求到内网 add new
                if (!CollectionUtils.isEmpty(securityFilters)){
                    ServerHttpRequest.Builder builder = request.mutate();
                    Mono<Void> mono;
                    for (SecurityFilter securityFilter : securityFilters){
                        if (null != (mono = securityFilter.forward(request, response, builder))){
                            return mono;
                        }
                    }
                }

                return chain.filter(exchangeDecorator);
            }

            //2. 认证
            String token = null;
            String userInfo = null;
            if (needAuth) {
                token = RequestResolveUtils.extractToken(request);
                log.info("请求头中的token为：{}；当前请求的方法为：{}，请求的路径为：{}", token, method.name(), uri);
                if (StringUtils.isEmpty(token)) {
                    log.info("请先登录！");
                    Mono<Void> error = errorHandling(response, 307, "对不起，您还没有登录，请先登录！");
                    return error;
                }

                //判断是否过期
                userInfo = redisService.get(token);
                log.info("redis中存储的用户信息为：{}", userInfo);
                if (StringUtils.isEmpty(userInfo)) {
                    log.warn("您的token已过期，请重新登录认证。");
                    Mono<Void> error = errorHandling(response, 306, "您的登录信息已过期，请重新登录");
                    return error;
                }
            }

                //请求头动态添加用户信息，网关对用户信息进行UTF-8编码，微服务获取用户信息后解码
                String encodeUserInfo = null;
                try {
                    encodeUserInfo = URLEncoder.encode(userInfo, "UTF-8");
                }catch (UnsupportedEncodingException ex){
                    ex.printStackTrace();
                    log.warn("userInfo编码失败，userInfo:{},异常信息为：{}",userInfo,ex);
                    Mono<Void> error = errorHandling(response, 308, StatusCode.FAIL.getMessage());
                    return error;
                }
                ServerHttpRequest.Builder builder = request.mutate();
                builder.header("Authority-User", encodeUserInfo);

                //转发之前需要追加地址判断，是否请求到内网 add new
                if (!CollectionUtils.isEmpty(securityFilters)){
                    Mono<Void> mono;
                    for (SecurityFilter securityFilter : securityFilters){
                        if (null != (mono = securityFilter.forward(request, response, builder))){
                            return mono;
                        }
                    }
                }

                Mono<Void> result = chain.filter(exchangeDecorator.mutate().request(builder.build()).build());
                return result;

        }catch (Exception e){
            e.printStackTrace();
            log.warn("系统异常，异常信息为：{}", e.getLocalizedMessage());
            Mono<Void> error = errorHandling(response, 309, StatusCode.FAIL.getMessage());
            return error;
        }
    }


    @Override
    public int getOrder() {
        return 0;
    }

    private Mono<Void> errorHandling(ServerHttpResponse response, int code, String errorMsg){
        CommonResponse<Boolean> failResult = CommonResponse.fail(code,errorMsg,false);
        DataBuffer wrap = response.bufferFactory().wrap(JSON.toJSONString(failResult).getBytes(StandardCharsets.UTF_8));
        response.getHeaders().add("Content-Type", "text/plain;charset=UTF-8");
        return response.writeWith(Mono.just(wrap));
    }


}

package com.xudasong.project.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

@Slf4j
public class GlobalLoadBalancerFilter extends LoadBalancerClientFilter {

    public GlobalLoadBalancerFilter(LoadBalancerClient loadBalancer, LoadBalancerProperties properties) {
        super(loadBalancer, properties);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        if (url == null || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))){
            return chain.filter(exchange);
        }
        addOriginalRequestUrl(exchange, url);
        log.info("LoadBalancerClientFilter url before: " + url);
        final ServiceInstance instance = choose(exchange);
        if (instance == null){
            log.warn("不能找到服务实例：{}",url.getHost());
            throw new NotFoundException("不能找到服务实例: " + url.getHost());
        }

        URI uri = exchange.getRequest().getURI();
        String overrideScheme = null;
        if (schemePrefix != null){
            overrideScheme = uri.getScheme();
        }

        URI requestUrl = loadBalancer.reconstructURI(new DelegatingServiceInstance(instance, overrideScheme), uri);
        log.info("LoadBalancerClientFilter url chosen: {}", requestUrl);
        log.info("网关转发后，注册中心负载均衡选择的服务实例为：{}", requestUrl);
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR,requestUrl);
        return chain.filter(exchange);
    }

    class DelegatingServiceInstance implements ServiceInstance{
        final ServiceInstance delegate;
        private String overrideScheme;

        DelegatingServiceInstance(ServiceInstance delegate, String overrideScheme){
            this.delegate = delegate;
            this.overrideScheme = overrideScheme;
        }

        @Override
        public String getServiceId() {
            return delegate.getServiceId();
        }

        @Override
        public String getHost() {
            return delegate.getHost();
        }

        @Override
        public int getPort() {
            return delegate.getPort();
        }

        @Override
        public boolean isSecure() {
            return delegate.isSecure();
        }

        @Override
        public URI getUri() {
            return delegate.getUri();
        }

        @Override
        public Map<String, String> getMetadata() {
            return delegate.getMetadata();
        }

        @Override
        public String getScheme() {
            String scheme = delegate.getScheme();
            if (scheme != null){
                return scheme;
            }
            return this.overrideScheme;
        }
    }
}

package com.xudasong.project.config;

import com.xudasong.project.filter.GlobalAuthorizationFilter;
import com.xudasong.project.filter.GlobalLoadBalancerFilter;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfiguration {

    @Bean
    public GlobalAuthorizationFilter globalAuthorizationFilter(){
        return new GlobalAuthorizationFilter();
    }

    @Bean
    public GlobalLoadBalancerFilter globalLoadBalancerFilter(LoadBalancerClient loadBalancerClient, LoadBalancerProperties properties){
        return new GlobalLoadBalancerFilter(loadBalancerClient, properties);
    }

}

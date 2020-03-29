package com.xudasong.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class RedisConfiguration {

    @Value("${spring.redis.nodes}")
    private String clusterNode;

    @Value("${spring.redis.pwd}")
    private String pwd;

    @Value("${spring.redis.experiesTime}")
    private Integer expiresSeconds;

    @Value("${spring.redis.maxRedirects}")
    private Integer maxRedirects;

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
        String[] serverArray = clusterNode.split(",");
        Set<RedisNode> nodes = new HashSet<>();
        for (String ipPort : serverArray){
            String[] ipPortPair = ipPort.split(":");
            nodes.add(new RedisNode(ipPortPair[0].trim(), Integer.valueOf(ipPortPair[1].trim())));
        }
        redisClusterConfiguration.setClusterNodes(nodes);
        redisClusterConfiguration.setPassword(RedisPassword.of(pwd));
        redisClusterConfiguration.setMaxRedirects(maxRedirects);
        return new JedisConnectionFactory(redisClusterConfiguration);
    }

}

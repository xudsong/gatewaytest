package com.xudasong.project.service.impl;

import com.xudasong.project.service.IRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements IRedisService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean set(String key, String val) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) ->connection.set(serializer.serialize(key), serializer.serialize(val)));
    }

    @Override
    public boolean set(String key, String val, int time, TimeUnit unit) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) ->connection.set(serializer.serialize(key), serializer.serialize(val), Expiration.from(time, unit), RedisStringCommands.SetOption.UPSERT));
    }

    @Override
    public boolean hset(String key, String field, String val) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) ->
            connection.hSet(serializer.serialize(key), serializer.serialize(field), serializer.serialize(val)));
    }

    @Override
    public boolean hset(String key, String field, String val, int time, TimeUnit unit) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) ->
                connection.hSet(serializer.serialize(key), serializer.serialize(field), serializer.serialize(val))
                && connection.expire(serializer.serialize(key), Expiration.from(time, unit).getExpirationTime()));
    }

    @Override
    public String get(String key) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) -> serializer.deserialize(connection.get(serializer.serialize(key))));
    }

    @Override
    public String hget(String key, String field) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) ->
                serializer.deserialize(connection.hGet(serializer.serialize(key), serializer.serialize(field))));
    }

    @Override
    //不建议使用hGetAll, 性能上有问题
    public boolean delete(String key) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) ->connection.del(serializer.serialize(key))) == 1;
    }

    @Override
    public Long incr(String key) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) ->connection.incr(serializer.serialize(key)));
    }

    @Override
    public boolean setNX(String key, String value) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) ->connection.setNX(serializer.serialize(key), serializer.serialize(value)));
    }

    @Override
    public boolean expire(String key, int time, TimeUnit unit) {
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        return redisTemplate.execute((RedisConnection connection) ->
                connection.expire(serializer.serialize(key), Expiration.from(time, unit).getExpirationTime()));
    }
}

package com.xudasong.project.service;

import java.util.concurrent.TimeUnit;

/**
 * 操作Redis集群
 */
public interface IRedisService {

    /**
     * 存入Redis
     * @param key 键
     * @param val 值
     * @return 是否成功存入
     */
    public boolean set(String key, String val);

    /**
     * 存入Redis
     * @param key
     * @param val
     * @param time 预计过期时间
     * @param unit 时间单位
     * @return
     */
    public boolean set(String key, String val, int time, TimeUnit unit);

    /**
     * 存入Redis
     * @param key
     * @param field 散列字段
     * @param val
     * @return
     */
    public boolean hset(String key, String field, String val);

    /**
     * 存入Redis
     * @param key
     * @param field
     * @param val
     * @param time
     * @param unit
     * @return
     */
    public boolean hset(String key, String field, String val, int time, TimeUnit unit);

    /**
     * 获取值
     * @param key 键
     * @return 字符串
     */
    public String get(String key);

    /**
     * 获取值
     * @param key
     * @param field 散列字段
     * @return
     */
    public String hget(String key, String field);

    /**
     * 删除键
     * @param key
     * @return
     */
    public boolean delete(String key);

    /**
     * 加1操作
     * @param key
     * @return
     */
    public Long incr(String key);

    /**
     * key不存在则设置，存在做任何操作
     * @param key
     * @param value
     * @return
     */
    public boolean setNX(String key, String value);

    /**
     * 设置key有效时间
     * @param key
     * @param time
     * @param unit
     * @return
     */
    public boolean expire(String key, int time, TimeUnit unit);
}

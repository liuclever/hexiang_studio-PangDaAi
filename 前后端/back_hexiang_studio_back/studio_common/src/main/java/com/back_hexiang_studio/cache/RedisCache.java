package com.back_hexiang_studio.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RedisCache {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 默认过期时间（5分钟）
    private static final long DEFAULT_TTL = 5 * 60;

    /**
     * 核心逻辑：
     *
     * 先从 Redis 获取 key 对应的值
     *
     * 如果缓存不存在：
     * 调用 supplier.get() 获取数据
     * 把数据写入 Redis，设置过期时间
     * 返回数据
     */
    public <T> T get(String key, Supplier<T> supplier, long ttlSeconds) {
        T value = (T) redisTemplate.opsForValue().get(key);
        if (value != null) {
            return value;
        }

        // 缓存不存在，执行supplier获取数据
        value = supplier.get();
        if (value != null) {
            //opsForValue() 是拿出来操作 字符串类型数据 的工具
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        }
        return value;
    }


    /**
     * 使用默认过期时间获取缓存
     */
    public <T> T get(String key, Supplier<T> supplier) {
        return get(key, supplier, DEFAULT_TTL);
    }

    /**
     * 删除指定的缓存
     */
    public void remove(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 删除指定前缀的所有缓存
     */
    public void removeByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 清空所有缓存（慎用）
     */
    public void clear() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }


    /**
     * 设置缓存对象
     */
    public void setCacheObject(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存对象
     */
    public <T> T getCacheObject(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存对象
     */
    public void deleteObject(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 判断缓存是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
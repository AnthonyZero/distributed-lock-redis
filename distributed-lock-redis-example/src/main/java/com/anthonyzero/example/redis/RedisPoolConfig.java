package com.anthonyzero.example.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * redis连接池配置
 */
@Component
public class RedisPoolConfig {

    @Autowired
    private RedisProperties redisProperties;

    @Bean
    public JedisPool jedisPoolFactory() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(redisProperties.getPoolMaxTotal());
        jedisPoolConfig.setMaxIdle(redisProperties.getPoolMaxIdle());
        jedisPoolConfig.setMaxWaitMillis(redisProperties.getPoolMaxWait() * 1000);
        String password = null;
        if (!StringUtils.isEmpty(redisProperties.getPassword())) {
            password = redisProperties.getPassword();
        }
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, redisProperties.getHost(), redisProperties.getPort(),
                redisProperties.getTimeout() * 1000, password, 0);
        return jedisPool;
    }
}

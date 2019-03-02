package com.anthonyzero.example.config;

import com.anthonyzero.distributed.lock.RedisLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.JedisPool;

/**
 * 锁配置
 */
@Configuration
public class DistributedLockConfig {

    @Autowired
    private JedisPool jedisPool;

    /**
     * 默认分布式锁(不开启续期)
     * @return
     */
    @Bean(name = "defaultLock")
    @Primary
    public RedisLock defaultBuild() {
        RedisLock redisLock = new RedisLock.Builder(jedisPool)
                .expireTime(50) //过期时间50秒 不设置默认为60秒
                .build();
        return redisLock;
    }

    /**
     * 分布式锁(开启了续期)
     * @return
     */
    @Bean(name = "renewalLock")
    public RedisLock openRenewalBuild() {
        RedisLock redisLock = new RedisLock.Builder(jedisPool)
                .expireTime(60) //设置过期时间
                .openRenewal(true) //开启守护线程续期 默认不开启
                .renewalPercentage(0.5) //每次续期的时间占过期时间的比例 0到1的范围
                .build();
        return redisLock;
    }
}

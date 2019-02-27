package com.anthonyzero.distributed.lock;

import com.anthonyzero.distributed.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

/**
 * redis分布式锁
 */
public class RedisLock {
    private static Logger logger = LoggerFactory.getLogger(RedisLock.class);
    //set命令成功返回
    private static final String LOCK_SUCCESS_MSG = "OK";
    //执行解锁lua脚本成功返回
    private static final Long UNLOCK_SUCCESS_MSG = 1L;
    // 一秒等于1000毫秒
    private static final int TIME = 1000;
    // 2.6.12版本以上set的参数 只在键不存在时才对键进行设置 等价于setnx
    private static final String SET_IF_NOT_EXIST = "NX";
    // 2.6.12版本以上set的参数 将键的过期时间设置为毫秒数 等价于psetex
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private JedisPool jedisPool;
    // 解锁lua脚本
    private String unlockScript;
    // 续期lua脚本
    private String renewalScript;
    //是否续期
    private boolean openRenewal;
    //续期时间比例
    private double renewalPercentage;
    //过期时间
    private int expireTime;

    private RedisLock(Builder builder) {
        this.jedisPool = builder.jedisPool;
        this.expireTime = builder.expireTime;
        this.openRenewal = builder.openRenewal;
        this.renewalPercentage = builder.renewalPercentage;
        this.unlockScript = FileUtil.getLuaScript("unlock.lua");
    }




    public static class Builder {
        //默认关闭后台线程续期
        private static final boolean DEFAULT_CLOSE_RENEWAL = false;
        //默认每次续期的时间比例为0.6（续期时间=过期时间*0.6）
        private static final double DEFAULT_RENEWAL_TIME_PERCENTAGE = 0.6;
        //默认锁过期时间（秒）
        private static final int DEFAULT_EXPIRE_SECONDS = 60;
        private JedisPool jedisPool = null;
        //是否续期
        private boolean openRenewal = DEFAULT_CLOSE_RENEWAL;
        //续期时间比例
        private double renewalPercentage = DEFAULT_RENEWAL_TIME_PERCENTAGE;
        //过期时间
        private int expireTime = DEFAULT_EXPIRE_SECONDS;

        public Builder(JedisPool jedisPool) {
            this.jedisPool = jedisPool;
        }

        public Builder openRenewal(boolean openRenewal) {
            this.openRenewal = openRenewal;
            return this;
        }

        public Builder renewalPercentage(double renewalPercentage) {
            this.renewalPercentage = renewalPercentage;
            return this;
        }

        public Builder expireTime(int expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public RedisLock build() {
            return new RedisLock(this);
        }
    }
}

package com.anthonyzero.distributed.lock;

import com.anthonyzero.distributed.expire.ExpirationRenewalProcessor;
import com.anthonyzero.distributed.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.LocalTime;
import java.util.Collections;

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
    //后台守护线程
    private Thread daemonThread;
    private ExpirationRenewalProcessor processor;

    private RedisLock(Builder builder) {
        this.jedisPool = builder.jedisPool;
        this.expireTime = builder.expireTime;
        this.openRenewal = builder.openRenewal;
        this.renewalPercentage = builder.renewalPercentage;
        this.unlockScript = FileUtil.getLuaScript("unlock.lua");
        this.renewalScript = FileUtil.getLuaScript("renewal.lua");
    }

    /**
     * 开启守护线程 定时刷新
     */
    private void scheduleExpirationRenewal(ExpirationRenewalProcessor processor){
        this.processor = processor;
        this.daemonThread = new Thread(processor);
        this.daemonThread.setDaemon(true);
        this.daemonThread.start();
    }

    /**
     * 加锁
     * @param key 键
     * @param request 客户端唯一标识（代表是谁获取了锁）
     * @return
     */
    public boolean lock(String key, String request) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String result = jedis.set(key, request, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime * TIME);
            if (LOCK_SUCCESS_MSG.equals(result)) {
                logger.info("Thread id:"+Thread.currentThread().getId() + "lock success!Time:"+ LocalTime.now());

                //开启后台线程
                if (openRenewal) {
                    long sleepTime = (long)(expireTime*TIME*renewalPercentage);
                    ExpirationRenewalProcessor processor = new ExpirationRenewalProcessor(jedisPool.getResource(),renewalScript, key,
                                request, expireTime*TIME, sleepTime);
                    scheduleExpirationRenewal(processor);
                }
                return true;
            } else {
                logger.info("Thread id:"+Thread.currentThread().getId() + "lock fail,Time:"+ LocalTime.now());
                return false;
            }
        } catch (Exception ex) {
            logger.error("lock error");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }

    /**
     * 释放锁
     * @param key 键
     * @param request 获取锁的客户端的唯一标识
     * @return
     */
    public boolean unlock(String key, String request) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Object result = jedis.eval(unlockScript, Collections.singletonList(key), Collections.singletonList(request));
            if (UNLOCK_SUCCESS_MSG.equals(result)) {
                logger.info("Thread id:"+Thread.currentThread().getId() + "unlock success!Time:"+ LocalTime.now());
                return true;
            } else {
                logger.info("Thread id:"+Thread.currentThread().getId() + "unlock fail,Time:"+ LocalTime.now());
                return false;
            }
        } catch (Exception ex) {
            logger.error("unlock error");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if (openRenewal && processor != null) {
                //停止续期
                processor.stop();
                daemonThread.interrupt();
            }
        }
        return false;
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

        /**
         * 是否开启守护后台线程 自动续期
         * @param openRenewal
         * @return
         */
        public Builder openRenewal(boolean openRenewal) {
            this.openRenewal = openRenewal;
            return this;
        }

        /**
         * 每次续期的时间占过期时间的比例 0到1的范围
         * @param renewalPercentage
         * @return
         */
        public Builder renewalPercentage(double renewalPercentage) {
            this.renewalPercentage = renewalPercentage;
            return this;
        }

        /**
         * 设置过期时间
         * @param expireTime
         * @return
         */
        public Builder expireTime(int expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public RedisLock build() {
            return new RedisLock(this);
        }
    }
}

package com.anthonyzero.distributed.expire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.Collections;

/**
 * 守护线程 自动续期
 */
public class ExpirationRenewalProcessor implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(ExpirationRenewalProcessor.class);
    //expire命令执行成功返回结果
    private static final Long EXPIRE_SUCCESS_MSG = 1L;
    private Jedis jedis;
    // lua 脚本
    private String script;
    //键
    private String key;
    //代表获取锁的客户端标识
    private String request;
    //重置key的过期时间
    private long expireTime;
    //线程每次睡眠的时间
    private long sleepTime;
    //线程结束的标志
    private volatile boolean signal = true;

    public ExpirationRenewalProcessor(Jedis jedis, String script, String key,
                                      String request, long expireTime, long sleepTime) {
        this.jedis = jedis;
        this.script = script;
        this.key = key;
        this.request = request;
        this.expireTime = expireTime;
        this.sleepTime = sleepTime;
    }

    //标志位设置为结束标识
    public void stop() {
        this.signal = false;
    }

    @Override
    public void run() {
        while (signal) {
            try {
                Thread.sleep(sleepTime);
                Object result = jedis.eval(script, Collections.singletonList(key), Arrays.asList(request, String.valueOf(expireTime)));
                if (EXPIRE_SUCCESS_MSG.equals(result)) {
                    logger.info("Daemon thread renewal success");
                } else {
                    logger.info("Daemon thread renewal fail");
                    this.stop();
                }
            } catch (InterruptedException e) {
                logger.info("Daemon thread is interrupted forcibly");
            } catch (Exception e) {
                logger.info("Daemon thread run error");
                this.stop();
            }
        }
        if (jedis != null) {
            jedis.close();
        }
        logger.info("Daemon thread stopped");
    }
}

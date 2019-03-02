package com.anthonyzero.example;

import com.anthonyzero.distributed.lock.RedisLock;
import com.anthonyzero.example.bootstrap.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@SpringBootTest(classes = Application.class)
@RunWith(SpringRunner.class)
public class RedisLockTestUnit {

    /**
     * 默认锁（不开启续期）
     */
    @Autowired
    private RedisLock redisLock;

    /**
     * 锁（开启续期）
     */
    @Autowired
    @Qualifier("renewalLock")
    private RedisLock renewalLock;

    @Test
    public void start() {
        String key = "key";
        String request = UUID.randomUUID().toString();
        boolean flag = renewalLock.lock(key, request, 10);
        if (flag) { //获取锁成功
            try {
                //模拟业务执行
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                renewalLock.unlock(key, request);
            }
        }
        /*boolean flag = redisLock.lock(key, request, 40);
        if (flag) { //获取锁成功
            try {
                //模拟业务执行
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                redisLock.unlock(key, request);
            }
        }*/
    }
}

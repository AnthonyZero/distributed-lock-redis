package com.anthonyzero.example.bootstrap;

import com.anthonyzero.distributed.lock.RedisLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;
import java.util.concurrent.*;

@SpringBootApplication(scanBasePackages = "com.anthonyzero.example")
public class Application {

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext applicationContext =
                SpringApplication.run(Application.class, args);

        //获取分布式锁（开启自动续期）
        RedisLock redisLock = (RedisLock) applicationContext.getBean("renewalLock");
        ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 900,
                1, TimeUnit.SECONDS, new SynchronousQueue<>());
        for (int i = 0; i < 900; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2 * 1000);
                        executorTask(redisLock);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        executor.shutdown();
    }

    private static void executorTask(RedisLock redisLock) throws InterruptedException {
        String key = "key";
        String request = UUID.randomUUID().toString();
        boolean flag = redisLock.lock(key, request, 3);
        if (flag) {
            try {
                //do somethings
                Thread.sleep(2 * 1000);
            } finally {
                redisLock.unlock(key, request);
            }
        }
    }
 }

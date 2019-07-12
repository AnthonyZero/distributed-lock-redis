# distributed-lock-redis

> 基于Redis实现的简单分布式锁

## 项目结构

- [distributed-lock-redis-core](https://github.com/AnthonyZero/distributed-lock-redis/tree/master/distributed-lock-redis-core)：锁核心源码实现，使用前请Git到本地install
- [distributed-lock-redis-example](https://github.com/AnthonyZero/distributed-lock-redis/tree/master/distributed-lock-redis-example)：使用示例

## 环境依赖

> Redis 2.6.12 版本及以上

> Maven

## 特征
- [x] 高性能
- [x] 非阻塞
- [x] 自动续期

## 使用
添加maven依赖
```xml    
<dependency>
    <groupId>com.anthonyzero</groupId>
    <artifactId>distributed-lock-redis-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

注入IOC
```java
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
                .expireTime(60) //设置过期时间 默认为60秒
                .openRenewal(true) //开启守护线程续期 默认不开启
                .renewalPercentage(0.5) //每次续期的时间占过期时间的比例 0到1的范围(默认为0.6)
                .build();
        return redisLock;
    }
}
```

使用分布式锁 模拟业务执行
```java
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
    }
```

## 资料

博客文章说明[Redis分布式锁](https://anthonyzero.github.io/2019/03/03/Redis%E5%88%86%E5%B8%83%E5%BC%8F%E9%94%81/#more)

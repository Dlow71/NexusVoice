package com.nexusvoice.utils;

import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson工具类
 * 提供分布式锁、限流器、布隆过滤器等高级功能
 *
 * @author NexusVoice
 * @since 2025-10-17
 */
@Component
public class RedissonUtils {

    private static final Logger log = LoggerFactory.getLogger(RedissonUtils.class);

    @Autowired
    private RedissonClient redissonClient;

    // ============================= 分布式锁 =============================

    /**
     * 获取锁（可重入锁）
     *
     * @param lockKey 锁key
     * @return RLock对象
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 获取公平锁
     *
     * @param lockKey 锁key
     * @return RLock对象
     */
    public RLock getFairLock(String lockKey) {
        return redissonClient.getFairLock(lockKey);
    }

    /**
     * 获取读写锁
     *
     * @param lockKey 锁key
     * @return RReadWriteLock对象
     */
    public RReadWriteLock getReadWriteLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey);
    }

    /**
     * 加锁
     *
     * @param lockKey 锁key
     * @return 是否成功
     */
    public boolean lock(String lockKey) {
        RLock lock = getLock(lockKey);
        try {
            lock.lock();
            log.debug("获取锁成功，key：{}", lockKey);
            return true;
        } catch (Exception e) {
            log.error("获取锁失败，key：{}", lockKey, e);
            return false;
        }
    }

    /**
     * 加锁，设置超时时间
     *
     * @param lockKey   锁key
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @return 是否成功
     */
    public boolean lock(String lockKey, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            lock.lock(leaseTime, unit);
            log.debug("获取锁成功，key：{}，持有时间：{}{}", lockKey, leaseTime, unit);
            return true;
        } catch (Exception e) {
            log.error("获取锁失败，key：{}", lockKey, e);
            return false;
        }
    }

    /**
     * 尝试加锁
     *
     * @param lockKey 锁key
     * @return 是否成功
     */
    public boolean tryLock(String lockKey) {
        RLock lock = getLock(lockKey);
        try {
            boolean result = lock.tryLock();
            if (result) {
                log.debug("尝试获取锁成功，key：{}", lockKey);
            }
            return result;
        } catch (Exception e) {
            log.error("尝试获取锁失败，key：{}", lockKey, e);
            return false;
        }
    }

    /**
     * 尝试加锁
     *
     * @param lockKey   锁key
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @return 是否成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            boolean result = lock.tryLock(waitTime, leaseTime, unit);
            if (result) {
                log.debug("尝试获取锁成功，key：{}，等待时间：{}{}，持有时间：{}{}", 
                    lockKey, waitTime, unit, leaseTime, unit);
            }
            return result;
        } catch (Exception e) {
            log.error("尝试获取锁失败，key：{}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁key
     */
    public void unlock(String lockKey) {
        RLock lock = getLock(lockKey);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放锁成功，key：{}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放锁失败，key：{}", lockKey, e);
        }
    }

    /**
     * 释放锁
     *
     * @param lock RLock对象
     */
    public void unlock(RLock lock) {
        try {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放锁成功");
            }
        } catch (Exception e) {
            log.error("释放锁失败", e);
        }
    }

    /**
     * 执行带分布式锁的操作
     *
     * @param lockKey  锁key
     * @param supplier 要执行的操作
     * @param <T>      返回值类型
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, -1, -1, TimeUnit.SECONDS, supplier);
    }

    /**
     * 执行带分布式锁的操作
     *
     * @param lockKey   锁key
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @param supplier  要执行的操作
     * @param <T>       返回值类型
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, 
                                  TimeUnit unit, Supplier<T> supplier) {
        RLock lock = getLock(lockKey);
        boolean locked = false;
        try {
            if (waitTime > 0 && leaseTime > 0) {
                locked = lock.tryLock(waitTime, leaseTime, unit);
            } else if (waitTime > 0) {
                locked = lock.tryLock(waitTime, unit);
            } else {
                lock.lock();
                locked = true;
            }
            
            if (locked) {
                log.debug("执行带锁操作，key：{}", lockKey);
                return supplier.get();
            } else {
                log.warn("获取锁失败，无法执行操作，key：{}", lockKey);
                return null;
            }
        } catch (Exception e) {
            log.error("执行带锁操作失败，key：{}", lockKey, e);
            throw new RuntimeException("执行带锁操作失败", e);
        } finally {
            if (locked) {
                unlock(lock);
            }
        }
    }

    // ============================= 限流器 =============================

    /**
     * 获取限流器
     *
     * @param key 限流器key
     * @return RRateLimiter对象
     */
    public RRateLimiter getRateLimiter(String key) {
        return redissonClient.getRateLimiter(key);
    }

    /**
     * 初始化限流器
     *
     * @param key               限流器key
     * @param rate              速率
     * @param rateInterval      速率时间间隔
     * @param rateIntervalUnit  速率时间间隔单位
     * @return 是否成功
     */
    public boolean initRateLimiter(String key, long rate, long rateInterval, 
                                    RateIntervalUnit rateIntervalUnit) {
        RRateLimiter rateLimiter = getRateLimiter(key);
        try {
            return rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, rateIntervalUnit);
        } catch (Exception e) {
            log.error("初始化限流器失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 尝试获取许可
     *
     * @param key     限流器key
     * @param permits 许可数量
     * @return 是否成功
     */
    public boolean tryAcquire(String key, long permits) {
        RRateLimiter rateLimiter = getRateLimiter(key);
        try {
            return rateLimiter.tryAcquire(permits);
        } catch (Exception e) {
            log.error("获取限流许可失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 尝试获取许可（带超时）
     *
     * @param key     限流器key
     * @param permits 许可数量
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否成功
     */
    public boolean tryAcquire(String key, long permits, long timeout, TimeUnit unit) {
        RRateLimiter rateLimiter = getRateLimiter(key);
        try {
            return rateLimiter.tryAcquire(permits, timeout, unit);
        } catch (Exception e) {
            log.error("获取限流许可失败，key：{}", key, e);
            return false;
        }
    }

    // ============================= 布隆过滤器 =============================

    /**
     * 获取布隆过滤器
     *
     * @param key 布隆过滤器key
     * @param <T> 元素类型
     * @return RBloomFilter对象
     */
    public <T> RBloomFilter<T> getBloomFilter(String key) {
        return redissonClient.getBloomFilter(key);
    }

    /**
     * 初始化布隆过滤器
     *
     * @param key                布隆过滤器key
     * @param expectedInsertions 预期插入数量
     * @param falseProbability   误判率
     * @param <T>                元素类型
     * @return 是否成功
     */
    public <T> boolean initBloomFilter(String key, long expectedInsertions, double falseProbability) {
        RBloomFilter<T> bloomFilter = getBloomFilter(key);
        try {
            return bloomFilter.tryInit(expectedInsertions, falseProbability);
        } catch (Exception e) {
            log.error("初始化布隆过滤器失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 添加元素到布隆过滤器
     *
     * @param key     布隆过滤器key
     * @param element 元素
     * @param <T>     元素类型
     * @return 是否成功
     */
    public <T> boolean bloomAdd(String key, T element) {
        RBloomFilter<T> bloomFilter = getBloomFilter(key);
        try {
            return bloomFilter.add(element);
        } catch (Exception e) {
            log.error("添加元素到布隆过滤器失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 批量添加元素到布隆过滤器
     *
     * @param key      布隆过滤器key
     * @param elements 元素集合
     * @param <T>      元素类型
     * @return 添加的数量
     */
    public <T> long bloomAdd(String key, Collection<T> elements) {
        RBloomFilter<T> bloomFilter = getBloomFilter(key);
        try {
            long count = 0;
            for (T element : elements) {
                if (bloomFilter.add(element)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            log.error("批量添加元素到布隆过滤器失败，key：{}", key, e);
            return 0;
        }
    }

    /**
     * 判断元素是否存在于布隆过滤器
     *
     * @param key     布隆过滤器key
     * @param element 元素
     * @param <T>     元素类型
     * @return 是否存在
     */
    public <T> boolean bloomContains(String key, T element) {
        RBloomFilter<T> bloomFilter = getBloomFilter(key);
        try {
            return bloomFilter.contains(element);
        } catch (Exception e) {
            log.error("判断元素是否存在于布隆过滤器失败，key：{}", key, e);
            return false;
        }
    }

    // ============================= 分布式集合 =============================

    /**
     * 获取Map
     *
     * @param name Map名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return RMap对象
     */
    public <K, V> RMap<K, V> getMap(String name) {
        return redissonClient.getMap(name);
    }

    /**
     * 获取Set
     *
     * @param name Set名称
     * @param <V>  值类型
     * @return RSet对象
     */
    public <V> RSet<V> getSet(String name) {
        return redissonClient.getSet(name);
    }

    /**
     * 获取List
     *
     * @param name List名称
     * @param <V>  值类型
     * @return RList对象
     */
    public <V> RList<V> getList(String name) {
        return redissonClient.getList(name);
    }

    /**
     * 获取Queue
     *
     * @param name Queue名称
     * @param <V>  值类型
     * @return RQueue对象
     */
    public <V> RQueue<V> getQueue(String name) {
        return redissonClient.getQueue(name);
    }

    /**
     * 获取Deque
     *
     * @param name Deque名称
     * @param <V>  值类型
     * @return RDeque对象
     */
    public <V> RDeque<V> getDeque(String name) {
        return redissonClient.getDeque(name);
    }

    // ============================= 分布式原子类 =============================

    /**
     * 获取原子Long
     *
     * @param name 名称
     * @return RAtomicLong对象
     */
    public RAtomicLong getAtomicLong(String name) {
        return redissonClient.getAtomicLong(name);
    }

    /**
     * 获取原子Double
     *
     * @param name 名称
     * @return RAtomicDouble对象
     */
    public RAtomicDouble getAtomicDouble(String name) {
        return redissonClient.getAtomicDouble(name);
    }

    /**
     * 原子递增
     *
     * @param name 名称
     * @return 递增后的值
     */
    public long incrementAtomic(String name) {
        return getAtomicLong(name).incrementAndGet();
    }

    /**
     * 原子递增
     *
     * @param name  名称
     * @param delta 增量
     * @return 递增后的值
     */
    public long incrementAtomic(String name, long delta) {
        return getAtomicLong(name).addAndGet(delta);
    }

    /**
     * 原子递减
     *
     * @param name 名称
     * @return 递减后的值
     */
    public long decrementAtomic(String name) {
        return getAtomicLong(name).decrementAndGet();
    }

    /**
     * 原子递减
     *
     * @param name  名称
     * @param delta 减量
     * @return 递减后的值
     */
    public long decrementAtomic(String name, long delta) {
        return getAtomicLong(name).addAndGet(-delta);
    }

    // ============================= 消息队列 =============================

    /**
     * 获取消息主题
     *
     * @param name 主题名称
     * @return RTopic对象
     */
    public RTopic getTopic(String name) {
        return redissonClient.getTopic(name);
    }

    /**
     * 发布消息
     *
     * @param topicName 主题名称
     * @param message   消息
     * @return 接收到消息的客户端数量
     */
    public long publish(String topicName, Object message) {
        RTopic topic = getTopic(topicName);
        try {
            return topic.publish(message);
        } catch (Exception e) {
            log.error("发布消息失败，topic：{}", topicName, e);
            return 0;
        }
    }

    /**
     * 订阅消息
     *
     * @param topicName 主题名称
     * @param listener  消息监听器
     * @param clazz     消息类型
     * @param <M>       消息类型
     * @return 监听器ID
     */
    public <M> int subscribe(String topicName, MessageListener<M> listener, Class<M> clazz) {
        RTopic topic = getTopic(topicName);
        try {
            return topic.addListener(clazz, listener);
        } catch (Exception e) {
            log.error("订阅消息失败，topic：{}", topicName, e);
            return -1;
        }
    }

    /**
     * 取消订阅
     *
     * @param topicName  主题名称
     * @param listenerId 监听器ID
     */
    public void unsubscribe(String topicName, Integer listenerId) {
        RTopic topic = getTopic(topicName);
        try {
            topic.removeListener(listenerId);
        } catch (Exception e) {
            log.error("取消订阅失败，topic：{}，listenerId：{}", topicName, listenerId, e);
        }
    }

    // ============================= 延迟队列 =============================

    /**
     * 获取延迟队列
     *
     * @param queueName 队列名称
     * @param <V>       值类型
     * @return RDelayedQueue对象
     */
    public <V> RDelayedQueue<V> getDelayedQueue(String queueName) {
        RQueue<V> queue = getQueue(queueName);
        return redissonClient.getDelayedQueue(queue);
    }

    /**
     * 添加延迟任务
     *
     * @param queueName 队列名称
     * @param value     任务内容
     * @param delay     延迟时间
     * @param timeUnit  时间单位
     * @param <V>       值类型
     */
    public <V> void offerDelayed(String queueName, V value, long delay, TimeUnit timeUnit) {
        RDelayedQueue<V> delayedQueue = getDelayedQueue(queueName);
        try {
            delayedQueue.offer(value, delay, timeUnit);
            log.debug("添加延迟任务成功，queue：{}，延迟：{}{}", queueName, delay, timeUnit);
        } catch (Exception e) {
            log.error("添加延迟任务失败，queue：{}", queueName, e);
        }
    }

    // ============================= 信号量 =============================

    /**
     * 获取信号量
     *
     * @param name 信号量名称
     * @return RSemaphore对象
     */
    public RSemaphore getSemaphore(String name) {
        return redissonClient.getSemaphore(name);
    }

    /**
     * 获取许可信号量
     *
     * @param name 信号量名称
     * @return RPermitExpirableSemaphore对象
     */
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String name) {
        return redissonClient.getPermitExpirableSemaphore(name);
    }

    /**
     * 尝试获取信号量
     *
     * @param name    信号量名称
     * @param permits 许可数量
     * @return 是否成功
     */
    public boolean tryAcquireSemaphore(String name, int permits) {
        RSemaphore semaphore = getSemaphore(name);
        try {
            return semaphore.tryAcquire(permits);
        } catch (Exception e) {
            log.error("获取信号量失败，name：{}", name, e);
            return false;
        }
    }

    /**
     * 释放信号量
     *
     * @param name    信号量名称
     * @param permits 许可数量
     */
    public void releaseSemaphore(String name, int permits) {
        RSemaphore semaphore = getSemaphore(name);
        try {
            semaphore.release(permits);
        } catch (Exception e) {
            log.error("释放信号量失败，name：{}", name, e);
        }
    }

    // ============================= 闭锁 =============================

    /**
     * 获取闭锁
     *
     * @param name 闭锁名称
     * @return RCountDownLatch对象
     */
    public RCountDownLatch getCountDownLatch(String name) {
        return redissonClient.getCountDownLatch(name);
    }

    /**
     * 设置闭锁计数
     *
     * @param name  闭锁名称
     * @param count 计数
     * @return 是否成功
     */
    public boolean setCountDownLatch(String name, long count) {
        RCountDownLatch countDownLatch = getCountDownLatch(name);
        try {
            return countDownLatch.trySetCount(count);
        } catch (Exception e) {
            log.error("设置闭锁计数失败，name：{}", name, e);
            return false;
        }
    }

    /**
     * 闭锁计数减一
     *
     * @param name 闭锁名称
     */
    public void countDown(String name) {
        RCountDownLatch countDownLatch = getCountDownLatch(name);
        try {
            countDownLatch.countDown();
        } catch (Exception e) {
            log.error("闭锁计数减一失败，name：{}", name, e);
        }
    }

    /**
     * 等待闭锁
     *
     * @param name 闭锁名称
     */
    public void await(String name) {
        RCountDownLatch countDownLatch = getCountDownLatch(name);
        try {
            countDownLatch.await();
        } catch (Exception e) {
            log.error("等待闭锁失败，name：{}", name, e);
        }
    }

    /**
     * 等待闭锁（带超时）
     *
     * @param name    闭锁名称
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否成功
     */
    public boolean await(String name, long timeout, TimeUnit unit) {
        RCountDownLatch countDownLatch = getCountDownLatch(name);
        try {
            return countDownLatch.await(timeout, unit);
        } catch (Exception e) {
            log.error("等待闭锁失败，name：{}", name, e);
            return false;
        }
    }

    // ============================= 其他工具方法 =============================

    /**
     * 获取所有键
     *
     * @param pattern 匹配模式
     * @return 键集合
     */
    public Iterable<String> getKeys(String pattern) {
        return redissonClient.getKeys().getKeysByPattern(pattern);
    }

    /**
     * 删除键
     *
     * @param keys 键
     * @return 删除的数量
     */
    public long deleteKeys(String... keys) {
        return redissonClient.getKeys().delete(keys);
    }

    /**
     * 批量删除键
     *
     * @param pattern 匹配模式
     * @return 删除的数量
     */
    public long deleteKeysByPattern(String pattern) {
        return redissonClient.getKeys().deleteByPattern(pattern);
    }

    /**
     * 获取Redisson客户端信息
     *
     * @return 信息Map
     */
    public Map<String, String> getClientInfo() {
        Map<String, String> info = new HashMap<>();
        try {
            String yaml = redissonClient.getConfig().toYAML();
            String[] lines = yaml.split("\n");
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    info.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception e) {
            log.error("获取Redisson客户端信息失败", e);
            info.put("error", e.getMessage());
        }
        return info;
    }
}

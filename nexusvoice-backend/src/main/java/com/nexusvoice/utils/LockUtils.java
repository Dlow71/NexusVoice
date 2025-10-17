package com.nexusvoice.utils;

import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 * 封装常用的分布式锁操作场景，支持注解式加锁，自动释放锁
 *
 * @author NexusVoice
 * @since 2025-10-17
 */
@Component
public class LockUtils {

    private static final Logger log = LoggerFactory.getLogger(LockUtils.class);

    @Autowired
    private RedissonUtils redissonUtils;

    /**
     * 默认等待锁的时间（秒）
     */
    private static final long DEFAULT_WAIT_TIME = 5;

    /**
     * 默认锁持有时间（秒）
     */
    private static final long DEFAULT_LEASE_TIME = 30;

    /**
     * 锁超时告警阈值（秒）
     */
    private static final long LOCK_TIMEOUT_WARNING_THRESHOLD = 10;

    /**
     * 执行带锁的操作（使用默认配置）
     *
     * @param lockKey  锁键
     * @param runnable 要执行的操作
     */
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, runnable);
    }

    /**
     * 执行带锁的操作
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @param runnable  要执行的操作
     */
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable) {
        RLock lock = redissonUtils.getLock(lockKey);
        boolean acquired = false;
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("尝试获取分布式锁，key：{}", lockKey);
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            
            if (!acquired) {
                log.warn("获取分布式锁失败，key：{}，等待时间：{}{}", lockKey, waitTime, unit);
                throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
            }
            
            log.debug("成功获取分布式锁，key：{}", lockKey);
            runnable.run();
            
            // 检查执行时间是否超过警告阈值
            long executionTime = System.currentTimeMillis() - startTime;
            if (executionTime > LOCK_TIMEOUT_WARNING_THRESHOLD * 1000) {
                log.warn("锁操作执行时间过长，key：{}，耗时：{}ms", lockKey, executionTime);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，key：{}", lockKey, e);
            throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行锁操作失败，key：{}", lockKey, e);
            throw new RuntimeException("执行锁操作失败", e);
        } finally {
            if (acquired) {
                try {
                    lock.unlock();
                    log.debug("释放分布式锁，key：{}", lockKey);
                } catch (Exception e) {
                    log.error("释放分布式锁失败，key：{}", lockKey, e);
                }
            }
        }
    }

    /**
     * 执行带锁的操作（返回结果）
     *
     * @param lockKey  锁键
     * @param supplier 要执行的操作
     * @param <T>      返回类型
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, supplier);
    }

    /**
     * 执行带锁的操作（返回结果）
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @param supplier  要执行的操作
     * @param <T>       返回类型
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        RLock lock = redissonUtils.getLock(lockKey);
        boolean acquired = false;
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("尝试获取分布式锁，key：{}", lockKey);
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            
            if (!acquired) {
                log.warn("获取分布式锁失败，key：{}，等待时间：{}{}", lockKey, waitTime, unit);
                throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
            }
            
            log.debug("成功获取分布式锁，key：{}", lockKey);
            T result = supplier.get();
            
            // 检查执行时间是否超过警告阈值
            long executionTime = System.currentTimeMillis() - startTime;
            if (executionTime > LOCK_TIMEOUT_WARNING_THRESHOLD * 1000) {
                log.warn("锁操作执行时间过长，key：{}，耗时：{}ms", lockKey, executionTime);
            }
            
            return result;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，key：{}", lockKey, e);
            throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行锁操作失败，key：{}", lockKey, e);
            throw new RuntimeException("执行锁操作失败", e);
        } finally {
            if (acquired) {
                try {
                    lock.unlock();
                    log.debug("释放分布式锁，key：{}", lockKey);
                } catch (Exception e) {
                    log.error("释放分布式锁失败，key：{}", lockKey, e);
                }
            }
        }
    }

    /**
     * 执行带公平锁的操作
     *
     * @param lockKey  锁键
     * @param runnable 要执行的操作
     */
    public void executeWithFairLock(String lockKey, Runnable runnable) {
        executeWithFairLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, runnable);
    }

    /**
     * 执行带公平锁的操作
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @param runnable  要执行的操作
     */
    public void executeWithFairLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable) {
        RLock lock = redissonUtils.getFairLock(lockKey);
        boolean acquired = false;
        
        try {
            log.debug("尝试获取公平锁，key：{}", lockKey);
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            
            if (!acquired) {
                log.warn("获取公平锁失败，key：{}，等待时间：{}{}", lockKey, waitTime, unit);
                throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
            }
            
            log.debug("成功获取公平锁，key：{}", lockKey);
            runnable.run();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取公平锁被中断，key：{}", lockKey, e);
            throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行公平锁操作失败，key：{}", lockKey, e);
            throw new RuntimeException("执行公平锁操作失败", e);
        } finally {
            if (acquired) {
                try {
                    lock.unlock();
                    log.debug("释放公平锁，key：{}", lockKey);
                } catch (Exception e) {
                    log.error("释放公平锁失败，key：{}", lockKey, e);
                }
            }
        }
    }

    /**
     * 执行带读锁的操作
     *
     * @param lockKey  锁键
     * @param supplier 要执行的操作
     * @param <T>      返回类型
     * @return 执行结果
     */
    public <T> T executeWithReadLock(String lockKey, Supplier<T> supplier) {
        RReadWriteLock rwLock = redissonUtils.getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        boolean acquired = false;
        
        try {
            log.debug("尝试获取读锁，key：{}", lockKey);
            acquired = readLock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            
            if (!acquired) {
                log.warn("获取读锁失败，key：{}", lockKey);
                throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
            }
            
            log.debug("成功获取读锁，key：{}", lockKey);
            return supplier.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取读锁被中断，key：{}", lockKey, e);
            throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行读锁操作失败，key：{}", lockKey, e);
            throw new RuntimeException("执行读锁操作失败", e);
        } finally {
            if (acquired) {
                try {
                    readLock.unlock();
                    log.debug("释放读锁，key：{}", lockKey);
                } catch (Exception e) {
                    log.error("释放读锁失败，key：{}", lockKey, e);
                }
            }
        }
    }

    /**
     * 执行带写锁的操作
     *
     * @param lockKey  锁键
     * @param runnable 要执行的操作
     */
    public void executeWithWriteLock(String lockKey, Runnable runnable) {
        RReadWriteLock rwLock = redissonUtils.getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();
        boolean acquired = false;
        
        try {
            log.debug("尝试获取写锁，key：{}", lockKey);
            acquired = writeLock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            
            if (!acquired) {
                log.warn("获取写锁失败，key：{}", lockKey);
                throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
            }
            
            log.debug("成功获取写锁，key：{}", lockKey);
            runnable.run();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取写锁被中断，key：{}", lockKey, e);
            throw new BizException(ErrorCodeEnum.REDIS_LOCK_ACQUIRE_FAILED);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行写锁操作失败，key：{}", lockKey, e);
            throw new RuntimeException("执行写锁操作失败", e);
        } finally {
            if (acquired) {
                try {
                    writeLock.unlock();
                    log.debug("释放写锁，key：{}", lockKey);
                } catch (Exception e) {
                    log.error("释放写锁失败，key：{}", lockKey, e);
                }
            }
        }
    }

    /**
     * 尝试获取锁（不等待）
     *
     * @param lockKey 锁键
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey) {
        return redissonUtils.tryLock(lockKey);
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        return redissonUtils.tryLock(lockKey, waitTime, leaseTime, unit);
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁键
     */
    public void unlock(String lockKey) {
        redissonUtils.unlock(lockKey);
    }

    /**
     * 构建锁键
     *
     * @param prefix 前缀
     * @param keys   键值
     * @return 锁键
     */
    public static String buildLockKey(String prefix, Object... keys) {
        if (keys == null || keys.length == 0) {
            return "lock:" + prefix;
        }
        
        StringBuilder sb = new StringBuilder("lock:").append(prefix);
        for (Object key : keys) {
            sb.append(":").append(key);
        }
        return sb.toString();
    }

    /**
     * 检查是否持有锁
     *
     * @param lockKey 锁键
     * @return 是否持有锁
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        RLock lock = redissonUtils.getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 检查锁是否被任何线程持有
     *
     * @param lockKey 锁键
     * @return 是否被持有
     */
    public boolean isLocked(String lockKey) {
        RLock lock = redissonUtils.getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * 强制解锁（慎用）
     *
     * @param lockKey 锁键
     */
    public void forceUnlock(String lockKey) {
        RLock lock = redissonUtils.getLock(lockKey);
        lock.forceUnlock();
        log.warn("强制解锁，key：{}", lockKey);
    }
}

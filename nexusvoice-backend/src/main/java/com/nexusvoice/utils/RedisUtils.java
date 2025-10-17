package com.nexusvoice.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 * 封装常用的Redis操作，提供便捷的API接口
 *
 * @author NexusVoice
 * @since 2025-10-17
 */
@Component
public class RedisUtils {

    private static final Logger log = LoggerFactory.getLogger(RedisUtils.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ============================= 通用操作 =============================

    /**
     * 指定缓存失效时间
     *
     * @param key     键
     * @param timeout 时间（秒）
     * @return 设置是否成功
     */
    public boolean expire(String key, long timeout) {
        try {
            if (timeout > 0) {
                redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("设置键{}的过期时间失败", key, e);
            return false;
        }
    }

    /**
     * 指定缓存失效时间
     *
     * @param key      键
     * @param timeout  时间
     * @param timeUnit 时间单位
     * @return 设置是否成功
     */
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        try {
            if (timeout > 0) {
                redisTemplate.expire(key, timeout, timeUnit);
            }
            return true;
        } catch (Exception e) {
            log.error("设置键{}的过期时间失败", key, e);
            return false;
        }
    }

    /**
     * 根据key获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间（秒） 返回0代表为永久有效
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : 0;
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("判断键{}是否存在时失败", key, e);
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param keys 可以传一个值或多个
     */
    public void delete(String... keys) {
        if (keys != null && keys.length > 0) {
            if (keys.length == 1) {
                redisTemplate.delete(keys[0]);
                log.debug("删除缓存键：{}", keys[0]);
            } else {
                List<String> keyList = Arrays.asList(keys);
                redisTemplate.delete(keyList);
                log.debug("批量删除缓存键：{}", keyList);
            }
        }
    }

    /**
     * 根据前缀批量删除缓存
     *
     * @param prefix 前缀
     */
    public void deleteByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete(keys);
            log.info("批量删除前缀为{}的缓存，共{}个", prefix, keys.size());
        }
    }

    // ============================= String操作 =============================

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取字符串类型的值
     *
     * @param key 键
     * @return 字符串值
     */
    public String getString(String key) {
        return key == null ? null : stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 时间（秒） timeout要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false失败
     */
    public boolean setEx(String key, Object value, long timeout) {
        try {
            if (timeout > 0) {
                redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key      键
     * @param value    值
     * @param timeout  时间
     * @param timeUnit 时间单位
     * @return true成功 false失败
     */
    public boolean setEx(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            if (timeout > 0) {
                redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 设置字符串类型的值
     *
     * @param key     键
     * @param value   字符串值
     * @param timeout 过期时间（秒）
     * @return 是否成功
     */
    public boolean setString(String key, String value, long timeout) {
        try {
            if (timeout > 0) {
                stringRedisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
            } else {
                stringRedisTemplate.opsForValue().set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("设置字符串缓存失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 只有在key不存在时设置key的值
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean setIfAbsent(String key, Object value) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("setIfAbsent失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几（大于0）
     * @return 递增后的值
     */
    public long increment(String key, long delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("递增因子必须大于0");
        }
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0;
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几（大于0）
     * @return 递减后的值
     */
    public long decrement(String key, long delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("递减因子必须大于0");
        }
        Long result = redisTemplate.opsForValue().increment(key, -delta);
        return result != null ? result : 0;
    }

    // ============================= Hash操作 =============================

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hGet(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hSetAll(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("HashSet失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key     键
     * @param map     对应多个键值
     * @param timeout 时间（秒）
     * @return true成功 false失败
     */
    public boolean hSetAll(String key, Map<String, Object> map, long timeout) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (timeout > 0) {
                expire(key, timeout);
            }
            return true;
        } catch (Exception e) {
            log.error("HashSet失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据，如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hSet(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.error("Hash设置失败，key：{}，item：{}", key, item, e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据，如果不存在将创建，并设置时间
     *
     * @param key     键
     * @param item    项
     * @param value   值
     * @param timeout 时间（秒） 注意：如果已存在的hash表有时间，这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hSet(String key, String item, Object value, long timeout) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (timeout > 0) {
                expire(key, timeout);
            }
            return true;
        } catch (Exception e) {
            log.error("Hash设置失败，key：{}，item：{}", key, item, e);
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hDelete(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增 如果不存在，就会创建一个 并把新增后的值返回
     *
     * @param key   键
     * @param item  项
     * @param delta 要增加几（大于0）
     * @return 递增后的值
     */
    public double hIncrement(String key, String item, double delta) {
        return redisTemplate.opsForHash().increment(key, item, delta);
    }

    /**
     * hash递减
     *
     * @param key   键
     * @param item  项
     * @param delta 要减少记（小于0）
     * @return 递减后的值
     */
    public double hDecrement(String key, String item, double delta) {
        return redisTemplate.opsForHash().increment(key, item, -delta);
    }

    // ============================= Set操作 =============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return Set集合
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("获取Set失败，key：{}", key, e);
            return null;
        }
    }

    /**
     * 根据value从一个set中查询，是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(key, value);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("判断Set成员是否存在失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().add(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Set添加失败，key：{}", key, e);
            return 0;
        }
    }

    /**
     * 将set数据放入缓存，并设置时间
     *
     * @param key     键
     * @param timeout 时间（秒）
     * @param values  值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long timeout, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (timeout > 0) {
                expire(key, timeout);
            }
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Set添加失败，key：{}", key, e);
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long sGetSetSize(String key) {
        try {
            Long result = redisTemplate.opsForSet().size(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取Set大小失败，key：{}", key, e);
            return 0;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long sRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Set移除失败，key：{}", key, e);
            return 0;
        }
    }

    // ============================= List操作 =============================

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束  0到-1代表所有值
     * @return 列表
     */
    public List<Object> lRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("获取List范围失败，key：{}", key, e);
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long lGetListSize(String key) {
        try {
            Long result = redisTemplate.opsForList().size(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取List大小失败，key：{}", key, e);
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引  index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return 值
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.error("获取List索引值失败，key：{}，index：{}", key, index, e);
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lPush(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("List添加失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key     键
     * @param value   值
     * @param timeout 时间（秒）
     * @return 是否成功
     */
    public boolean lPush(String key, Object value, long timeout) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (timeout > 0) {
                expire(key, timeout);
            }
            return true;
        } catch (Exception e) {
            log.error("List添加失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 将list批量放入缓存
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lPushAll(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("List批量添加失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 将list批量放入缓存，并设置时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 时间（秒）
     * @return 是否成功
     */
    public boolean lPushAll(String key, List<Object> value, long timeout) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (timeout > 0) {
                expire(key, timeout);
            }
            return true;
        } catch (Exception e) {
            log.error("List批量添加失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return 是否成功
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            log.error("List更新索引值失败，key：{}，index：{}", key, index, e);
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove != null ? remove : 0;
        } catch (Exception e) {
            log.error("List移除失败，key：{}", key, e);
            return 0;
        }
    }

    // ============================= ZSet操作 =============================

    /**
     * 向有序集合添加一个成员
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return 是否成功
     */
    public boolean zAdd(String key, Object value, double score) {
        try {
            redisTemplate.opsForZSet().add(key, value, score);
            return true;
        } catch (Exception e) {
            log.error("ZSet添加失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 向有序集合添加多个成员
     *
     * @param key    键
     * @param values 值和分数的集合
     * @return 添加的个数
     */
    public long zAdd(String key, Set<ZSetOperations.TypedTuple<Object>> values) {
        try {
            Long result = redisTemplate.opsForZSet().add(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("ZSet批量添加失败，key：{}", key, e);
            return 0;
        }
    }

    /**
     * 移除有序集合中的一个或多个成员
     *
     * @param key    键
     * @param values 值
     * @return 移除的个数
     */
    public long zRemove(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForZSet().remove(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("ZSet移除失败，key：{}", key, e);
            return 0;
        }
    }

    /**
     * 增加有序集合中成员的分数
     *
     * @param key   键
     * @param value 值
     * @param score 增加的分数
     * @return 新的分数
     */
    public double zIncrementScore(String key, Object value, double score) {
        try {
            Double result = redisTemplate.opsForZSet().incrementScore(key, value, score);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("ZSet增加分数失败，key：{}", key, e);
            return 0;
        }
    }

    /**
     * 返回有序集合中成员的排名（从小到大）
     *
     * @param key   键
     * @param value 值
     * @return 排名，0开始
     */
    public long zRank(String key, Object value) {
        try {
            Long result = redisTemplate.opsForZSet().rank(key, value);
            return result != null ? result : -1;
        } catch (Exception e) {
            log.error("获取ZSet排名失败，key：{}", key, e);
            return -1;
        }
    }

    /**
     * 返回有序集合中成员的排名（从大到小）
     *
     * @param key   键
     * @param value 值
     * @return 排名，0开始
     */
    public long zReverseRank(String key, Object value) {
        try {
            Long result = redisTemplate.opsForZSet().reverseRank(key, value);
            return result != null ? result : -1;
        } catch (Exception e) {
            log.error("获取ZSet反向排名失败，key：{}", key, e);
            return -1;
        }
    }

    /**
     * 获取有序集合指定范围内的成员（从小到大）
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 成员集合
     */
    public Set<Object> zRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().range(key, start, end);
        } catch (Exception e) {
            log.error("获取ZSet范围失败，key：{}", key, e);
            return null;
        }
    }

    /**
     * 获取有序集合指定范围内的成员（从大到小）
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 成员集合
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            log.error("获取ZSet反向范围失败，key：{}", key, e);
            return null;
        }
    }

    /**
     * 获取有序集合的成员数量
     *
     * @param key 键
     * @return 成员数量
     */
    public long zSize(String key) {
        try {
            Long result = redisTemplate.opsForZSet().size(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取ZSet大小失败，key：{}", key, e);
            return 0;
        }
    }

    /**
     * 获取有序集合成员的分数
     *
     * @param key   键
     * @param value 值
     * @return 分数
     */
    public double zScore(String key, Object value) {
        try {
            Double result = redisTemplate.opsForZSet().score(key, value);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取ZSet分数失败，key：{}", key, e);
            return 0;
        }
    }
}

package com.nexusvoice.infrastructure.repository.file;

import com.nexusvoice.domain.file.model.TempFile;
import com.nexusvoice.domain.file.repository.TempFileRepository;
import com.nexusvoice.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 临时文件仓储Redis实现
 * 使用Redis存储临时文件元数据，利用TTL自动过期机制
 * 支持多种文件类型和存储提供商
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
@Slf4j
@Repository
public class RedisTempFileRepository implements TempFileRepository {
    
    private static final String KEY_PREFIX = "nexusvoice:temp_file:";
    private static final String EXPIRING_SOON_SET = "nexusvoice:temp_file:expiring_soon";
    private static final String CATEGORY_COUNT_KEY_PREFIX = "nexusvoice:temp_file:count:";
    
    private final StringRedisTemplate redisTemplate;
    
    public RedisTempFileRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public void save(TempFile tempFile, Duration ttl) {
        if (tempFile == null || tempFile.getFileKey() == null) {
            log.warn("临时文件或文件Key为空，跳过保存");
            return;
        }
        
        try {
            String key = buildKey(tempFile.getFileKey());
            String jsonValue = JsonUtils.toJson(tempFile);
            
            // 保存到Redis，设置TTL
            redisTemplate.opsForValue().set(key, jsonValue, ttl);
            
            // 如果TTL小于等于2小时，加入即将过期集合
            if (ttl.toHours() <= 2) {
                redisTemplate.opsForZSet().add(EXPIRING_SOON_SET, 
                    tempFile.getFileKey(), 
                    System.currentTimeMillis() + ttl.toMillis());
            }
            
            // 更新分类计数
            if (tempFile.getFileCategory() != null) {
                String countKey = buildCategoryCountKey(tempFile.getFileCategory());
                redisTemplate.opsForValue().increment(countKey);
            }
            
            log.debug("保存临时文件到Redis，key: {}, 分类: {}, 存储: {}, TTL: {}小时", 
                tempFile.getFileKey(), 
                tempFile.getFileCategory(),
                tempFile.getStorageProvider(),
                ttl.toHours());
                
        } catch (Exception e) {
            log.error("保存临时文件到Redis失败，fileKey: {}", tempFile.getFileKey(), e);
        }
    }
    
    @Override
    public void batchSave(List<TempFile> tempFiles, Duration ttl) {
        if (tempFiles == null || tempFiles.isEmpty()) {
            return;
        }
        
        log.info("批量保存{}个临时文件到Redis，TTL: {}小时", tempFiles.size(), ttl.toHours());
        
        for (TempFile file : tempFiles) {
            save(file, ttl);
        }
    }
    
    @Override
    public Optional<TempFile> findByFileKey(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            String key = buildKey(fileKey);
            String jsonValue = redisTemplate.opsForValue().get(key);
            
            if (jsonValue == null) {
                return Optional.empty();
            }
            
            TempFile tempFile = JsonUtils.fromJson(jsonValue, TempFile.class);
            return Optional.ofNullable(tempFile);
            
        } catch (Exception e) {
            log.error("从Redis查询临时文件失败，fileKey: {}", fileKey, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<TempFile> findExpiringSoon(int limit) {
        List<TempFile> result = new ArrayList<>();
        
        try {
            long nowMillis = System.currentTimeMillis();
            LocalDateTime now = LocalDateTime.now();
            
            // ✅ 修复：查询已经过期的文件（expireAt <= now），而不是即将过期的
            // score是expireAt的时间戳，查询0~now之间的就是已过期的文件
            Set<String> fileKeys = redisTemplate.opsForZSet()
                .rangeByScore(EXPIRING_SOON_SET, 0, nowMillis, 0, limit);
            
            if (fileKeys == null || fileKeys.isEmpty()) {
                return result;
            }
            
            log.debug("查询到{}个已过期的临时文件", fileKeys.size());
            
            // 查询每个文件的详细信息，再次检查过期时间（双重校验）
            for (String fileKey : fileKeys) {
                Optional<TempFile> tempFileOpt = findByFileKey(fileKey);
                if (tempFileOpt.isPresent()) {
                    TempFile tempFile = tempFileOpt.get();
                    LocalDateTime expireAt = tempFile.getExpireAt();
                    
                    log.debug("双重校验：fileKey={}, expireAt={}, now={}, isExpired={}", 
                        fileKey, expireAt, now, expireAt != null && expireAt.isBefore(now));
                    
                    // 双重校验：检查文件对象内部的expireAt字段
                    if (expireAt != null && expireAt.isBefore(now)) {
                        log.info("✅ 文件已过期，加入清理列表：{}, expireAt={}", fileKey, expireAt);
                        result.add(tempFile);
                    } else {
                        log.warn("❌ 文件{}在Sorted Set中标记为过期，但对象内部expireAt未过期，跳过。expireAt={}, now={}", 
                            fileKey, expireAt, now);
                    }
                } else {
                    // Redis主记录已被TTL自动删除，但Sorted Set索引还在（孤儿记录）
                    // 需要清理Sorted Set中的孤儿记录，并将文件加入清理列表
                    log.warn("⚠️ 检测到孤儿记录：fileKey={}，Redis主记录已过期但Sorted Set索引还在，清理索引并标记文件待删除", fileKey);
                    
                    // 从Sorted Set中删除孤儿记录
                    redisTemplate.opsForZSet().remove(EXPIRING_SOON_SET, fileKey);
                    
                    // ⚠️ 关键修复：即使Redis记录已删除，仍需要删除存储中的实际文件
                    // 创建一个最小化的TempFile对象用于删除存储文件
                    // storageProvider从系统配置读取（假设为当前配置的provider）
                    TempFile orphanFile = TempFile.builder()
                            .fileKey(fileKey)
                            .fileUrl(null)  // URL未知，但有fileKey就能删除
                            .storageProvider(null)  // 稍后由清理服务根据系统配置决定
                            .fileCategory(null)
                            .expireAt(LocalDateTime.now().minusHours(1))  // 标记为已过期
                            .build();
                    
                    result.add(orphanFile);
                    log.info("✅ 孤儿文件加入清理列表：{}", fileKey);
                }
            }
            
        } catch (Exception e) {
            log.error("查询已过期的临时文件失败", e);
        }
        
        return result;
    }
    
    @Override
    public void delete(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return;
        }
        
        try {
            // 先查询文件信息，用于更新分类计数
            Optional<TempFile> tempFileOpt = findByFileKey(fileKey);
            
            // 删除文件记录
            String key = buildKey(fileKey);
            redisTemplate.delete(key);
            
            // 从即将过期集合中移除
            redisTemplate.opsForZSet().remove(EXPIRING_SOON_SET, fileKey);
            
            // 更新分类计数
            tempFileOpt.ifPresent(tempFile -> {
                if (tempFile.getFileCategory() != null) {
                    String countKey = buildCategoryCountKey(tempFile.getFileCategory());
                    redisTemplate.opsForValue().decrement(countKey);
                }
            });
            
            log.debug("删除临时文件记录，fileKey: {}", fileKey);
            
        } catch (Exception e) {
            log.error("删除临时文件记录失败，fileKey: {}", fileKey, e);
        }
    }
    
    @Override
    public void batchDelete(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return;
        }
        
        log.info("批量删除{}个临时文件记录", fileKeys.size());
        
        for (String fileKey : fileKeys) {
            delete(fileKey);
        }
    }
    
    @Override
    public long count() {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("统计临时文件数量失败", e);
            return 0;
        }
    }
    
    @Override
    public long countByCategory(TempFile.FileCategory category) {
        if (category == null) {
            return 0;
        }
        
        try {
            String countKey = buildCategoryCountKey(category);
            String count = redisTemplate.opsForValue().get(countKey);
            return count != null ? Long.parseLong(count) : 0;
        } catch (Exception e) {
            log.error("统计分类文件数量失败，分类: {}", category, e);
            return 0;
        }
    }
    
    /**
     * 构建Redis Key
     */
    private String buildKey(String fileKey) {
        return KEY_PREFIX + fileKey;
    }
    
    /**
     * 构建分类计数Key
     */
    private String buildCategoryCountKey(TempFile.FileCategory category) {
        return CATEGORY_COUNT_KEY_PREFIX + category.name().toLowerCase();
    }
}

package com.nexusvoice.domain.developer.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.stereotype.Service;

/**
 * API密钥生成服务
 * 
 * 生成格式：sk-nv-{24位随机字符}-{6位校验码}
 * 例如：sk-nv-a1b2c3d4e5f6g7h8i9j0k1l2-abc123
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Service
public class ApiKeyGeneratorService {
    
    /**
     * API Key前缀
     */
    private static final String API_KEY_PREFIX = "sk-nv-";
    
    /**
     * 随机字符串长度
     */
    private static final int RANDOM_LENGTH = 24;
    
    /**
     * 校验码长度
     */
    private static final int CHECKSUM_LENGTH = 6;
    
    /**
     * 生成API Key
     * 
     * @return 完整的API Key字符串
     */
    public String generate() {
        // 生成24位随机字符串（小写字母+数字）
        String random = RandomUtil.randomString(RANDOM_LENGTH).toLowerCase();
        
        // 生成6位校验码
        String checksum = generateChecksum(random);
        
        // 组合：sk-nv-{random}-{checksum}
        return String.format("%s%s-%s", API_KEY_PREFIX, random, checksum);
    }
    
    /**
     * 计算API Key的SHA256哈希值（用于存储）
     * 
     * @param apiKey 原始API Key
     * @return SHA256哈希值
     */
    public String hash(String apiKey) {
        return DigestUtil.sha256Hex(apiKey);
    }
    
    /**
     * 生成密钥前缀（用于显示）
     * 
     * @param apiKey 原始API Key
     * @return 前缀（如sk-nv-abc123...）
     */
    public String generatePrefix(String apiKey) {
        if (apiKey == null || apiKey.length() < 16) {
            return apiKey;
        }
        // 返回前16个字符 + "..."
        return apiKey.substring(0, 16) + "...";
    }
    
    /**
     * 验证API Key格式是否正确
     * 
     * @param apiKey API Key
     * @return true-格式正确，false-格式错误
     */
    public boolean validateFormat(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        
        // 检查前缀
        if (!apiKey.startsWith(API_KEY_PREFIX)) {
            return false;
        }
        
        // 检查格式：sk-nv-{24位}-{6位}
        String[] parts = apiKey.split("-");
        if (parts.length != 4) {
            return false;
        }
        
        // 检查随机部分长度
        if (parts[2].length() != RANDOM_LENGTH) {
            return false;
        }
        
        // 检查校验码长度
        if (parts[3].length() != CHECKSUM_LENGTH) {
            return false;
        }
        
        // 验证校验码
        String expectedChecksum = generateChecksum(parts[2]);
        return expectedChecksum.equals(parts[3]);
    }
    
    /**
     * 生成校验码
     * 
     * @param input 输入字符串
     * @return 6位校验码
     */
    private String generateChecksum(String input) {
        // 使用hashCode生成整数，取绝对值后转16进制，左侧补零并截取前6位
        int hash = Math.abs(input.hashCode());
        String hex = Integer.toHexString(hash);
        // 左侧补零到至少6位
        if (hex.length() < CHECKSUM_LENGTH) {
            StringBuilder sb = new StringBuilder(CHECKSUM_LENGTH);
            for (int i = 0; i < CHECKSUM_LENGTH - hex.length(); i++) {
                sb.append('0');
            }
            sb.append(hex);
            hex = sb.toString();
        }
        // 返回前6位
        return hex.substring(0, CHECKSUM_LENGTH);
    }
}

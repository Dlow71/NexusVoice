package com.nexusvoice.domain.storage.repository;

import com.nexusvoice.domain.storage.enums.StorageProvider;
import com.nexusvoice.domain.storage.model.StorageConfig;
import com.nexusvoice.domain.storage.model.UploadResult;
import com.nexusvoice.enums.FileTypeEnum;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 存储服务仓储接口
 * 定义统一的存储操作接口，所有存储实现都需要实现此接口
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
public interface StorageRepository {
    
    /**
     * 上传文件（MultipartFile）
     *
     * @param file 文件
     * @param fileType 文件类型
     * @return 上传结果
     */
    UploadResult upload(MultipartFile file, FileTypeEnum fileType);
    
    /**
     * 上传文件（字节数组）
     *
     * @param data 文件数据
     * @param fileName 文件名
     * @param fileType 文件类型
     * @return 上传结果
     */
    UploadResult upload(byte[] data, String fileName, FileTypeEnum fileType);
    
    /**
     * 上传文件（输入流）
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param fileType 文件类型
     * @return 上传结果
     */
    UploadResult upload(InputStream inputStream, String fileName, long fileSize, FileTypeEnum fileType);
    
    /**
     * 上传文件（带自定义元数据）
     *
     * @param file 文件
     * @param fileType 文件类型
     * @param metadata 元数据
     * @return 上传结果
     */
    UploadResult upload(MultipartFile file, FileTypeEnum fileType, Map<String, String> metadata);
    
    /**
     * 删除文件
     *
     * @param fileKey 文件Key
     * @return 是否删除成功
     */
    boolean delete(String fileKey);
    
    /**
     * 批量删除文件
     *
     * @param fileKeys 文件Key列表
     * @return 删除成功的文件Key列表
     */
    List<String> batchDelete(List<String> fileKeys);
    
    /**
     * 获取文件访问URL
     *
     * @param fileKey 文件Key
     * @return 文件访问URL
     */
    String getFileUrl(String fileKey);
    
    /**
     * 获取文件预签名URL（用于临时访问）
     *
     * @param fileKey 文件Key
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL
     */
    String getPresignedUrl(String fileKey, int expireSeconds);
    
    /**
     * 检查文件是否存在
     *
     * @param fileKey 文件Key
     * @return 是否存在
     */
    boolean exists(String fileKey);
    
    /**
     * 获取文件信息
     *
     * @param fileKey 文件Key
     * @return 文件信息（大小、修改时间等）
     */
    Map<String, Object> getFileInfo(String fileKey);
    
    /**
     * 下载文件
     *
     * @param fileKey 文件Key
     * @return 文件输入流
     */
    InputStream download(String fileKey);
    
    /**
     * 复制文件
     *
     * @param sourceKey 源文件Key
     * @param targetKey 目标文件Key
     * @return 是否复制成功
     */
    boolean copy(String sourceKey, String targetKey);
    
    /**
     * 移动文件
     *
     * @param sourceKey 源文件Key
     * @param targetKey 目标文件Key
     * @return 是否移动成功
     */
    boolean move(String sourceKey, String targetKey);
    
    /**
     * 列出目录下的文件
     *
     * @param prefix 目录前缀
     * @param maxKeys 最大返回数量
     * @return 文件Key列表
     */
    List<String> listFiles(String prefix, int maxKeys);
    
    /**
     * 检查存储服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取存储提供商
     *
     * @return 存储提供商
     */
    StorageProvider getProvider();
    
    /**
     * 获取存储配置
     *
     * @return 存储配置
     */
    StorageConfig getConfig();
    
    /**
     * 更新存储配置
     *
     * @param config 新的存储配置
     */
    void updateConfig(StorageConfig config);
    
    /**
     * 初始化存储服务
     */
    void initialize();
    
    /**
     * 销毁存储服务（释放资源）
     */
    void destroy();
}

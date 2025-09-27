package com.nexusvoice.enums;

/**
 * @Author AJ
 * @Date 2025-09-27 00:05
 * @Description 文件类型枚举
 */
public enum FileTypeEnum {
    /**
     * 音频文件
     */
    AUDIO("audio", "音频文件"),
    
    /**
     * 图片文件
     */
    IMAGE("image", "图片文件"),
    
    /**
     * 视频文件
     */
    VIDEO("video", "视频文件"),

    /**
     * 其他文件
     */
    OTHER("other", "其他文件");
    
    private final String code;
    private final String description;
    
    FileTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据文件扩展名判断文件类型
     * @param fileName 文件名
     * @return 文件类型枚举
     */
    public static FileTypeEnum getFileTypeByFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return OTHER;
        }
        
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        
        switch (extension) {
            // 音频文件
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
            case "ogg":
            case "m4a":
            case "wma":
                return AUDIO;
                
            // 图片文件
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
            case "svg":
            case "ico":
            case "tiff":
            case "tif":
                return IMAGE;
                
            // 视频文件
            case "mp4":
            case "avi":
            case "mov":
            case "wmv":
            case "flv":
            case "mkv":
            case "webm":
            case "m4v":
            case "3gp":
                return VIDEO;

            default:
                return OTHER;
        }
    }
}

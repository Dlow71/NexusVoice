# -*- coding: utf-8 -*-
"""
配置管理模块

使用 Pydantic Settings 管理环境变量配置，提供类型安全的配置访问。
支持从 .env 文件或环境变量中读取配置。
"""

import os
from functools import lru_cache
from typing import Optional
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    应用配置类
    
    所有配置项都会从环境变量中读取，如果环境变量不存在则使用默认值。
    配置优先级：环境变量 > .env文件 > 默认值
    """
    
    # ==================== 服务配置 ====================
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    WORKERS: int = 1
    ENV: str = "dev"  # dev 或 prod
    
    # ==================== 数据库配置 ====================
    # PostgreSQL异步连接URL
    # 格式：postgresql+asyncpg://username:password@host:port/database
    DB_URL: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/chat_service"
    
    # PostgreSQL同步连接URL（用于初始化建表）
    # 格式：postgresql://username:password@host:port/database
    DB_URL_SYNC: str = "postgresql://postgres:postgres@localhost:5432/chat_service"
    
    # 数据库连接池配置
    DB_POOL_SIZE: int = 10
    DB_POOL_RECYCLE: int = 3600  # 连接回收时间（秒）
    DB_ECHO: bool = False  # 是否打印SQL语句
    
    # ==================== 日志配置 ====================
    LOG_PATH: str = "logs/server.log"
    LOG_LEVEL: str = "INFO"  # DEBUG/INFO/WARNING/ERROR
    LOG_ROTATION: str = "200 MB"  # 日志轮转大小
    
    # ==================== LLM配置 ====================
    # OpenAI API密钥（必填）
    OPENAI_API_KEY: str
    
    # OpenAI API基础URL（可选）
    OPENAI_BASE_URL: Optional[str] = "https://api.openai.com/v1"
    
    # 默认使用的模型
    DEFAULT_MODEL: str = "gpt-3.5-turbo"
    
    # LLM请求超时时间（秒）
    LLM_TIMEOUT: int = 60
    
    # LLM温度参数（0-2，控制输出随机性）
    LLM_TEMPERATURE: float = 0.7
    
    # 最大生成token数
    LLM_MAX_TOKENS: int = 2000
    
    # ==================== 其他配置 ====================
    # 文件保存路径
    FILE_SAVE_PATH: str = "files/"
    
    # 是否启用CORS跨域
    ENABLE_CORS: bool = True
    
    # Pydantic设置配置
    model_config = SettingsConfigDict(
        # 从.env文件读取配置
        env_file=".env",
        # .env文件编码
        env_file_encoding="utf-8",
        # 允许任意类型（用于兼容性）
        arbitrary_types_allowed=True,
        # 区分大小写
        case_sensitive=True
    )
    
    def is_dev(self) -> bool:
        """判断是否为开发环境"""
        return self.ENV.lower() == "dev"
    
    def is_prod(self) -> bool:
        """判断是否为生产环境"""
        return self.ENV.lower() == "prod"


@lru_cache()
def get_settings() -> Settings:
    """
    获取配置单例
    
    使用 lru_cache 装饰器确保配置对象只创建一次，
    提高性能并确保配置的一致性。
    
    Returns:
        Settings: 配置对象
        
    Examples:
        >>> settings = get_settings()
        >>> print(settings.DB_URL)
        >>> print(settings.OPENAI_API_KEY)
    """
    return Settings()


# 导出配置单例，方便其他模块导入使用
settings = get_settings()

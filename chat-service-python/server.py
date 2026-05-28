# -*- coding: utf-8 -*-
"""
FastAPI应用启动入口

负责创建FastAPI应用实例、注册中间件、注册路由、初始化日志等。
"""

import os
import uvicorn
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from chat_service.util.config import settings
from chat_service.db.db_engine import close_db


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    应用生命周期管理
    
    在应用启动时执行初始化操作，在应用关闭时执行清理操作。
    
    Args:
        app: FastAPI应用实例
        
    Yields:
        None
    """
    # ==================== 启动时执行 ====================
    logger.info("=" * 60)
    logger.info("Chat Service 正在启动...")
    logger.info(f"环境: {settings.ENV}")
    logger.info(f"数据库: {settings.DB_URL}")
    logger.info(f"默认模型: {settings.DEFAULT_MODEL}")
    logger.info("=" * 60)
    
    # 初始化日志
    init_logging()
    
    yield
    
    # ==================== 关闭时执行 ====================
    logger.info("Chat Service 正在关闭...")
    await close_db()
    logger.info("Chat Service 已关闭")


def init_logging():
    """
    初始化日志配置
    
    配置loguru日志系统，包括日志格式、日志文件、日志级别等。
    """
    # 确保日志目录存在
    log_dir = os.path.dirname(settings.LOG_PATH)
    if log_dir:
        os.makedirs(log_dir, exist_ok=True)
    
    # 配置日志格式
    log_format = (
        "<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> | "
        "<level>{level: <8}</level> | "
        "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> | "
        "<level>{message}</level>"
    )
    
    # 添加文件日志处理器
    logger.add(
        settings.LOG_PATH,
        format=log_format,
        rotation=settings.LOG_ROTATION,  # 日志轮转大小
        retention="30 days",  # 保留30天
        compression="zip",  # 压缩旧日志
        level=settings.LOG_LEVEL,
        encoding="utf-8"
    )
    
    logger.info(f"日志系统初始化完成: {settings.LOG_PATH}")


def register_middleware(app: FastAPI):
    """
    注册中间件
    
    添加CORS跨域中间件等。
    
    Args:
        app: FastAPI应用实例
    """
    if settings.ENABLE_CORS:
        # 添加CORS跨域中间件
        app.add_middleware(
            CORSMiddleware,
            allow_origins=["*"],  # 允许所有来源（生产环境应该限制）
            allow_credentials=True,  # 允许携带凭证
            allow_methods=["*"],  # 允许所有HTTP方法
            allow_headers=["*"],  # 允许所有HTTP头
        )
        logger.info("CORS跨域中间件已启用")


def register_router(app: FastAPI):
    """
    注册路由
    
    导入并挂载所有API路由。
    
    Args:
        app: FastAPI应用实例
    """
    from chat_service.api import api_router
    
    # 挂载API路由
    app.include_router(api_router)
    logger.info("API路由已注册")


def create_app() -> FastAPI:
    """
    创建FastAPI应用实例
    
    Returns:
        FastAPI: 配置好的FastAPI应用实例
    """
    # 创建FastAPI应用
    app = FastAPI(
        title="Chat Service API",
        version="1.0.0",
        description="基于 FastAPI + LangChain + PostgreSQL 的聊天服务",
        lifespan=lifespan,  # 生命周期管理
        docs_url="/docs",  # Swagger文档地址
        redoc_url="/redoc"  # ReDoc文档地址
    )
    
    # 注册中间件
    register_middleware(app)
    
    # 注册路由
    register_router(app)
    
    # 添加根路径处理
    @app.get("/")
    async def root():
        """根路径，返回服务信息"""
        return {
            "service": "Chat Service",
            "version": "1.0.0",
            "status": "running",
            "docs": "/docs",
            "health": "/api/v1/health"
        }
    
    return app


# ==================== 创建应用实例 ====================
app = create_app()


# ==================== 主函数（直接运行时执行） ====================
if __name__ == "__main__":
    # 启动服务
    uvicorn.run(
        app="server:app",  # 应用路径
        host=settings.HOST,  # 监听地址
        port=settings.PORT,  # 监听端口
        workers=settings.WORKERS,  # worker进程数
        reload=settings.is_dev(),  # 开发环境启用热重载
        log_level=settings.LOG_LEVEL.lower()  # 日志级别
    )

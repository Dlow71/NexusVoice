# -*- coding: utf-8 -*-
"""
数据库引擎配置模块

负责配置数据库连接、创建Session工厂、初始化数据库表。
使用 SQLModel + AsyncPg 实现异步数据库操作。
"""

import os
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from sqlmodel import SQLModel, create_engine
from loguru import logger

from chat_service.util.config import settings


# ==================== 异步数据库引擎 ====================

# 创建异步引擎（用于运行时数据库操作）
async_engine = create_async_engine(
    settings.DB_URL,
    echo=settings.DB_ECHO,  # 是否打印SQL语句
    pool_size=settings.DB_POOL_SIZE,  # 连接池大小
    pool_recycle=settings.DB_POOL_RECYCLE,  # 连接回收时间
    pool_pre_ping=True  # 连接前先ping，确保连接有效
)

# 创建异步Session工厂
async_session_local = sessionmaker(
    bind=async_engine,
    class_=AsyncSession,
    expire_on_commit=False,  # 提交后不过期对象
    autocommit=False,  # 不自动提交
    autoflush=False  # 不自动刷新
)


async def get_async_session():
    """
    获取异步数据库Session（用于FastAPI依赖注入）
    
    这是一个异步生成器函数，会在请求开始时创建Session，
    请求结束时自动关闭Session，确保资源正确释放。
    
    Yields:
        AsyncSession: 异步数据库Session对象
        
    Examples:
        在FastAPI路由中使用：
        >>> @router.get("/users")
        >>> async def get_users(session: AsyncSession = Depends(get_async_session)):
        >>>     result = await session.exec(select(User))
        >>>     return result.all()
    """
    async with async_session_local() as session:
        try:
            yield session
        except Exception as e:
            # 发生异常时回滚事务
            await session.rollback()
            logger.error(f"数据库操作异常: {str(e)}")
            raise
        finally:
            # 确保Session被关闭
            await session.close()


# ==================== 同步数据库引擎（用于初始化） ====================

def init_db():
    """
    初始化数据库（创建所有表）
    
    这个函数使用同步引擎创建数据库表，通常在应用启动时调用一次。
    注意：这是同步函数，不能在异步上下文中直接调用。
    
    Steps:
        1. 创建同步数据库引擎
        2. 导入所有表模型（确保模型被SQLModel识别）
        3. 调用 SQLModel.metadata.create_all() 创建表
        
    Examples:
        在命令行中初始化数据库：
        >>> python -m chat_service.db.db_engine
    """
    try:
        logger.info("开始初始化数据库...")
        
        # 创建同步引擎
        sync_engine = create_engine(
            settings.DB_URL_SYNC,
            echo=settings.DB_ECHO
        )
        
        # 导入所有表模型（必须在create_all之前导入）
        from chat_service.db.conversation import Conversation
        from chat_service.db.message import Message
        
        # 创建所有表
        SQLModel.metadata.create_all(sync_engine)
        
        logger.info("数据库初始化完成！")
        logger.info(f"数据库连接: {settings.DB_URL_SYNC}")
        logger.info("已创建表: conversations, messages")
        
    except Exception as e:
        logger.error(f"数据库初始化失败: {str(e)}")
        raise


async def close_db():
    """
    关闭数据库连接
    
    这个函数在应用关闭时调用，用于释放数据库连接资源。
    """
    await async_engine.dispose()
    logger.info("数据库连接已关闭")


# ==================== 直接执行初始化 ====================

if __name__ == "__main__":
    # 当直接运行此文件时，初始化数据库
    init_db()

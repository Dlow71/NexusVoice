# -*- coding: utf-8 -*-
"""
日志工具模块

提供统一的日志记录功能，使用 Loguru 库实现。
支持耗时统计、性能监控等功能。
"""

import time
from functools import wraps
from typing import Callable, Any
from loguru import logger


def timer(key: str = ""):
    """
    耗时装饰器
    
    用于统计异步函数的执行时间，并自动记录到日志。
    适用于性能监控和优化。
    
    Args:
        key: 日志标识，默认使用函数名
        
    Returns:
        装饰器函数
        
    Examples:
        >>> @timer("查询用户信息")
        >>> async def get_user(user_id: int):
        >>>     # 业务逻辑
        >>>     pass
        
        输出：查询用户信息 耗时: 123ms
    """
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        async def wrapper(*args, **kwargs) -> Any:
            # 记录开始时间
            start = time.time()
            
            # 执行原函数
            result = await func(*args, **kwargs)
            
            # 计算耗时（毫秒）
            elapsed = int((time.time() - start) * 1000)
            
            # 记录日志
            log_key = key or func.__name__
            logger.info(f"{log_key} 耗时: {elapsed}ms")
            
            return result
        return wrapper
    return decorator


def log_request(func: Callable) -> Callable:
    """
    API请求日志装饰器
    
    自动记录API请求的输入参数和输出结果。
    适用于调试和审计。
    
    Args:
        func: 被装饰的函数
        
    Returns:
        装饰后的函数
        
    Examples:
        >>> @log_request
        >>> async def create_user(request: UserRequest):
        >>>     # 业务逻辑
        >>>     pass
    """
    @wraps(func)
    async def wrapper(*args, **kwargs) -> Any:
        # 记录请求参数
        logger.debug(f"调用 {func.__name__}, 参数: args={args}, kwargs={kwargs}")
        
        try:
            # 执行原函数
            result = await func(*args, **kwargs)
            
            # 记录响应结果（仅在DEBUG模式下）
            logger.debug(f"{func.__name__} 返回: {result}")
            
            return result
        except Exception as e:
            # 记录异常信息
            logger.error(f"{func.__name__} 执行失败: {str(e)}", exc_info=True)
            raise
            
    return wrapper


class LogContext:
    """
    日志上下文管理器
    
    用于在日志中添加上下文信息（如请求ID、用户ID等），
    方便日志追踪和问题定位。
    
    Examples:
        >>> with LogContext(request_id="req-123", user_id=456):
        >>>     logger.info("处理用户请求")
        
        输出：[req-123] [user_id=456] 处理用户请求
    """
    
    def __init__(self, **kwargs):
        """
        初始化日志上下文
        
        Args:
            **kwargs: 上下文键值对
        """
        self.context = kwargs
        self.token = None
    
    def __enter__(self):
        """进入上下文，绑定日志上下文"""
        self.token = logger.contextualize(**self.context)
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """退出上下文，移除日志上下文"""
        if self.token:
            self.token.__exit__(exc_type, exc_val, exc_tb)

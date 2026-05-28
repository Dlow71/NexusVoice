# -*- coding: utf-8 -*-
"""
对话表模型（Conversation）

定义对话（会话）的数据库表结构。
每个对话可以包含多条消息（Message）。
"""

from datetime import datetime
from typing import Optional, List
from sqlmodel import SQLModel, Field, Relationship


class Conversation(SQLModel, table=True):
    """
    对话表模型
    
    存储用户的对话会话信息，每个对话包含一个唯一的标题和创建时间。
    一个对话可以包含多条消息（一对多关系）。
    
    Attributes:
        id: 主键ID，自增
        title: 对话标题（如："关于Python的讨论"）
        created_at: 创建时间，默认为当前时间
        updated_at: 最后更新时间，默认为当前时间
        
    Relationships:
        messages: 该对话下的所有消息（一对多关系）
    """
    
    __tablename__ = "conversations"
    
    # ==================== 主键 ====================
    id: Optional[int] = Field(
        default=None, 
        primary_key=True,
        description="对话ID，自增主键"
    )
    
    # ==================== 业务字段 ====================
    title: str = Field(
        max_length=200,
        index=True,  # 创建索引以提高查询性能
        description="对话标题"
    )
    
    # ==================== 时间字段 ====================
    created_at: datetime = Field(
        default_factory=datetime.now,
        description="创建时间"
    )
    
    updated_at: datetime = Field(
        default_factory=datetime.now,
        description="最后更新时间"
    )
    
    # ==================== 关联关系 ====================
    # 注意：在Python类型提示中使用字符串避免循环导入
    # messages: List["Message"] = Relationship(back_populates="conversation")
    
    class Config:
        """Pydantic配置"""
        # 允许从ORM对象创建模型
        from_attributes = True
        # JSON序列化时的配置
        json_schema_extra = {
            "example": {
                "id": 1,
                "title": "关于AI的讨论",
                "created_at": "2024-01-01T12:00:00",
                "updated_at": "2024-01-01T12:00:00"
            }
        }

# -*- coding: utf-8 -*-
"""
消息表模型（Message）

定义对话消息的数据库表结构。
每条消息属于一个对话，包含角色（用户/助手/系统）和内容。
"""

from datetime import datetime
from typing import Optional
from sqlmodel import SQLModel, Field, Relationship


class Message(SQLModel, table=True):
    """
    消息表模型
    
    存储对话中的每一条消息，包括用户消息和AI助手的回复。
    每条消息都关联到一个对话（多对一关系）。
    
    Attributes:
        id: 主键ID，自增
        conversation_id: 所属对话ID（外键）
        role: 消息角色（user/assistant/system）
            - user: 用户发送的消息
            - assistant: AI助手的回复
            - system: 系统消息（如：你是一个有帮助的AI助手）
        content: 消息内容
        created_at: 创建时间，默认为当前时间
        
    Relationships:
        conversation: 所属的对话对象（多对一关系）
    """
    
    __tablename__ = "messages"
    
    # ==================== 主键 ====================
    id: Optional[int] = Field(
        default=None,
        primary_key=True,
        description="消息ID，自增主键"
    )
    
    # ==================== 外键 ====================
    conversation_id: int = Field(
        foreign_key="conversations.id",
        index=True,  # 创建索引以提高查询性能
        description="所属对话ID"
    )
    
    # ==================== 业务字段 ====================
    role: str = Field(
        max_length=20,
        index=True,
        description="消息角色（user/assistant/system）"
    )
    
    content: str = Field(
        sa_type_kwargs={"type_": "TEXT"},  # 使用TEXT类型存储长文本
        description="消息内容"
    )
    
    # ==================== 时间字段 ====================
    created_at: datetime = Field(
        default_factory=datetime.now,
        description="创建时间"
    )
    
    # ==================== 关联关系 ====================
    # conversation: Optional["Conversation"] = Relationship(back_populates="messages")
    
    class Config:
        """Pydantic配置"""
        # 允许从ORM对象创建模型
        from_attributes = True
        # JSON序列化时的配置
        json_schema_extra = {
            "example": {
                "id": 1,
                "conversation_id": 1,
                "role": "user",
                "content": "你好，请介绍一下自己",
                "created_at": "2024-01-01T12:00:00"
            }
        }

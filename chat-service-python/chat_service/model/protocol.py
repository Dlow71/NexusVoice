# -*- coding: utf-8 -*-
"""
API请求响应协议模型

定义所有API接口的请求和响应数据结构。
使用 Pydantic 进行数据验证和序列化。
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime


# ==================== 通用响应模型 ====================

class ApiResponse(BaseModel):
    """
    统一API响应模型
    
    所有API接口都应该返回这个格式的响应。
    
    Attributes:
        code: 响应状态码（200表示成功，其他表示失败）
        message: 响应消息
        data: 响应数据（可选）
    """
    code: int = Field(default=200, description="响应状态码")
    message: str = Field(default="success", description="响应消息")
    data: Optional[Any] = Field(default=None, description="响应数据")
    
    class Config:
        json_schema_extra = {
            "example": {
                "code": 200,
                "message": "success",
                "data": {"key": "value"}
            }
        }


# ==================== 对话相关请求响应模型 ====================

class CreateConversationRequest(BaseModel):
    """
    创建对话请求模型
    
    Attributes:
        title: 对话标题
    """
    title: str = Field(..., min_length=1, max_length=200, description="对话标题")
    
    class Config:
        json_schema_extra = {
            "example": {
                "title": "关于Python的讨论"
            }
        }


class ConversationResponse(BaseModel):
    """
    对话响应模型
    
    Attributes:
        id: 对话ID
        title: 对话标题
        created_at: 创建时间
        updated_at: 最后更新时间
    """
    id: int = Field(..., description="对话ID")
    title: str = Field(..., description="对话标题")
    created_at: datetime = Field(..., description="创建时间")
    updated_at: datetime = Field(..., description="最后更新时间")
    
    class Config:
        # 允许从ORM模型直接转换
        from_attributes = True
        json_schema_extra = {
            "example": {
                "id": 1,
                "title": "关于AI的讨论",
                "created_at": "2024-01-01T12:00:00",
                "updated_at": "2024-01-01T12:00:00"
            }
        }


# ==================== 消息相关请求响应模型 ====================

class SendMessageRequest(BaseModel):
    """
    发送消息请求模型
    
    Attributes:
        conversation_id: 对话ID（可选，如果不提供则创建新对话）
        content: 消息内容
        model: 使用的AI模型（可选，默认使用配置中的模型）
        temperature: 温度参数（可选，默认使用配置中的值）
        max_tokens: 最大生成token数（可选）
    """
    conversation_id: Optional[int] = Field(
        default=None,
        alias="conversationId",
        description="对话ID，不提供则创建新对话"
    )
    content: str = Field(
        ...,
        min_length=1,
        max_length=10000,
        description="消息内容"
    )
    model: Optional[str] = Field(
        default=None,
        description="AI模型名称（如：gpt-3.5-turbo）"
    )
    temperature: Optional[float] = Field(
        default=None,
        ge=0.0,
        le=2.0,
        description="温度参数，控制输出随机性（0-2）"
    )
    max_tokens: Optional[int] = Field(
        default=None,
        ge=1,
        le=4000,
        description="最大生成token数"
    )
    
    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "conversation_id": 1,
                "content": "请介绍一下Python语言",
                "model": "gpt-3.5-turbo",
                "temperature": 0.7,
                "max_tokens": 2000
            }
        }


class MessageResponse(BaseModel):
    """
    消息响应模型
    
    Attributes:
        id: 消息ID
        conversation_id: 所属对话ID
        role: 消息角色（user/assistant/system）
        content: 消息内容
        created_at: 创建时间
    """
    id: int = Field(..., description="消息ID")
    conversation_id: int = Field(..., description="所属对话ID")
    role: str = Field(..., description="消息角色（user/assistant/system）")
    content: str = Field(..., description="消息内容")
    created_at: datetime = Field(..., description="创建时间")
    
    class Config:
        from_attributes = True
        json_schema_extra = {
            "example": {
                "id": 1,
                "conversation_id": 1,
                "role": "user",
                "content": "你好",
                "created_at": "2024-01-01T12:00:00"
            }
        }


class ChatResponse(BaseModel):
    """
    聊天响应模型（包含用户消息和AI回复）
    
    Attributes:
        conversation_id: 对话ID
        user_message: 用户消息
        assistant_message: AI助手回复
    """
    conversation_id: int = Field(..., description="对话ID")
    user_message: MessageResponse = Field(..., description="用户消息")
    assistant_message: MessageResponse = Field(..., description="AI助手回复")
    
    class Config:
        json_schema_extra = {
            "example": {
                "conversation_id": 1,
                "user_message": {
                    "id": 1,
                    "conversation_id": 1,
                    "role": "user",
                    "content": "你好",
                    "created_at": "2024-01-01T12:00:00"
                },
                "assistant_message": {
                    "id": 2,
                    "conversation_id": 1,
                    "role": "assistant",
                    "content": "你好！我是AI助手，有什么可以帮助你的吗？",
                    "created_at": "2024-01-01T12:00:01"
                }
            }
        }


class ConversationHistoryResponse(BaseModel):
    """
    对话历史响应模型
    
    Attributes:
        conversation: 对话信息
        messages: 消息列表
        total: 消息总数
    """
    conversation: ConversationResponse = Field(..., description="对话信息")
    messages: List[MessageResponse] = Field(..., description="消息列表")
    total: int = Field(..., description="消息总数")
    
    class Config:
        json_schema_extra = {
            "example": {
                "conversation": {
                    "id": 1,
                    "title": "关于AI的讨论",
                    "created_at": "2024-01-01T12:00:00",
                    "updated_at": "2024-01-01T12:00:00"
                },
                "messages": [],
                "total": 0
            }
        }


# ==================== 流式响应模型 ====================

class StreamChunk(BaseModel):
    """
    流式响应数据块
    
    用于SSE（Server-Sent Events）流式响应。
    
    Attributes:
        conversation_id: 对话ID
        delta: 增量文本内容
        is_final: 是否为最后一块数据
    """
    conversation_id: Optional[int] = Field(default=None, description="对话ID")
    delta: str = Field(default="", description="增量文本内容")
    is_final: bool = Field(default=False, description="是否为最后一块数据")
    
    class Config:
        json_schema_extra = {
            "example": {
                "conversation_id": 1,
                "delta": "你好",
                "is_final": False
            }
        }

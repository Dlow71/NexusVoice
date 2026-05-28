# -*- coding: utf-8 -*-
"""
聊天API接口

提供聊天相关的HTTP接口：
- 创建对话
- 发送消息（普通模式）
- 发送消息（流式模式）
- 查询对话历史
- 获取对话列表
"""

import json
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sse_starlette import ServerSentEvent, EventSourceResponse
from loguru import logger

from chat_service.db.db_engine import get_async_session
from chat_service.model.protocol import (
    ApiResponse,
    CreateConversationRequest,
    ConversationResponse,
    SendMessageRequest,
    MessageResponse,
    ChatResponse,
    ConversationHistoryResponse,
    StreamChunk
)
from chat_service.tool.chat_service import ChatService

# 创建路由器
router = APIRouter()


@router.get("/health")
async def health_check():
    """
    健康检查接口
    
    用于检查服务是否正常运行。
    
    Returns:
        简单的状态信息
    """
    return {"status": "ok", "service": "chat-service"}


@router.post("/conversation", response_model=ApiResponse)
async def create_conversation(
    request: CreateConversationRequest,
    session: AsyncSession = Depends(get_async_session)
):
    """
    创建新对话
    
    Args:
        request: 创建对话请求（包含title字段）
        session: 数据库Session（自动注入）
        
    Returns:
        包含对话信息的响应
        
    Raises:
        HTTPException: 创建失败时抛出400错误
    """
    try:
        # 调用业务逻辑层创建对话
        conversation = await ChatService.create_conversation(
            session,
            title=request.title
        )
        
        if not conversation:
            raise HTTPException(status_code=400, detail="创建对话失败")
        
        # 转换为响应模型
        conversation_response = ConversationResponse.model_validate(conversation)
        
        return ApiResponse(
            code=200,
            message="创建对话成功",
            data=conversation_response.model_dump()
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"创建对话异常: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"服务器错误: {str(e)}")


@router.post("/message", response_model=ApiResponse)
async def send_message(
    request: SendMessageRequest,
    session: AsyncSession = Depends(get_async_session)
):
    """
    发送消息并获取AI回复（普通模式）
    
    Args:
        request: 发送消息请求
        session: 数据库Session（自动注入）
        
    Returns:
        包含用户消息和AI回复的响应
        
    Raises:
        HTTPException: 处理失败时抛出错误
    """
    try:
        # 调用业务逻辑层发送消息
        result = await ChatService.send_message(
            session,
            conversation_id=request.conversation_id,
            user_content=request.content,
            model=request.model,
            temperature=request.temperature,
            max_tokens=request.max_tokens
        )
        
        # 转换为响应模型
        chat_response = ChatResponse(
            conversation_id=result["conversation_id"],
            user_message=MessageResponse.model_validate(result["user_message"]),
            assistant_message=MessageResponse.model_validate(result["assistant_message"])
        )
        
        return ApiResponse(
            code=200,
            message="发送消息成功",
            data=chat_response.model_dump()
        )
        
    except Exception as e:
        logger.error(f"发送消息异常: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"服务器错误: {str(e)}")


@router.post("/stream")
async def stream_message(
    request: SendMessageRequest,
    session: AsyncSession = Depends(get_async_session)
):
    """
    发送消息并获取AI回复（流式模式）
    
    使用Server-Sent Events (SSE)实现流式响应，
    客户端可以实时接收AI的回复内容。
    
    Args:
        request: 发送消息请求
        session: 数据库Session（自动注入）
        
    Returns:
        EventSourceResponse: SSE流式响应
        
    Examples:
        前端接收示例（JavaScript）：
        ```javascript
        const eventSource = new EventSource('/api/v1/chat/stream');
        eventSource.onmessage = (event) => {
            const data = JSON.parse(event.data);
            if (data.is_final) {
                eventSource.close();
            } else {
                console.log(data.delta);
            }
        };
        ```
    """
    async def stream_generator():
        """流式生成器函数"""
        try:
            # 调用业务逻辑层流式发送消息
            async for chunk in ChatService.stream_message(
                session,
                conversation_id=request.conversation_id,
                user_content=request.content,
                model=request.model,
                temperature=request.temperature,
                max_tokens=request.max_tokens
            ):
                # 将chunk转换为StreamChunk模型
                stream_chunk = StreamChunk(
                    conversation_id=chunk["conversation_id"],
                    delta=chunk["delta"],
                    is_final=chunk["is_final"]
                )
                
                # 序列化为JSON并发送
                yield ServerSentEvent(
                    data=json.dumps(
                        stream_chunk.model_dump(),
                        ensure_ascii=False  # 支持中文
                    )
                )
            
            # 发送结束标记
            yield ServerSentEvent(data="[DONE]")
            
        except Exception as e:
            logger.error(f"流式发送消息异常: {str(e)}", exc_info=True)
            # 发送错误信息
            error_chunk = StreamChunk(
                conversation_id=request.conversation_id,
                delta=f"[错误] {str(e)}",
                is_final=True
            )
            yield ServerSentEvent(
                data=json.dumps(error_chunk.model_dump(), ensure_ascii=False)
            )
    
    # 返回SSE响应
    return EventSourceResponse(
        stream_generator(),
        ping_message_factory=lambda: ServerSentEvent(data="heartbeat"),
        ping=15  # 每15秒发送一次心跳
    )


@router.get("/conversation/{conversation_id}/messages", response_model=ApiResponse)
async def get_conversation_history(
    conversation_id: int,
    session: AsyncSession = Depends(get_async_session)
):
    """
    获取对话历史
    
    Args:
        conversation_id: 对话ID（路径参数）
        session: 数据库Session（自动注入）
        
    Returns:
        包含对话信息和消息列表的响应
        
    Raises:
        HTTPException: 对话不存在时抛出404错误
    """
    try:
        # 调用业务逻辑层查询对话历史
        result = await ChatService.get_conversation_history(
            session,
            conversation_id
        )
        
        # 转换为响应模型
        history_response = ConversationHistoryResponse(
            conversation=ConversationResponse.model_validate(result["conversation"]),
            messages=[
                MessageResponse.model_validate(msg)
                for msg in result["messages"]
            ],
            total=result["total"]
        )
        
        return ApiResponse(
            code=200,
            message="查询对话历史成功",
            data=history_response.model_dump()
        )
        
    except Exception as e:
        if "不存在" in str(e):
            raise HTTPException(status_code=404, detail=str(e))
        logger.error(f"查询对话历史异常: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"服务器错误: {str(e)}")


@router.get("/conversations", response_model=ApiResponse)
async def list_conversations(
    limit: int = 100,
    session: AsyncSession = Depends(get_async_session)
):
    """
    获取对话列表
    
    Args:
        limit: 最多返回的对话数量（查询参数，默认100）
        session: 数据库Session（自动注入）
        
    Returns:
        包含对话列表的响应
    """
    try:
        # 调用业务逻辑层查询对话列表
        conversations = await ChatService.list_conversations(session, limit)
        
        # 转换为响应模型列表
        conversation_list = [
            ConversationResponse.model_validate(conv)
            for conv in conversations
        ]
        
        return ApiResponse(
            code=200,
            message="查询对话列表成功",
            data={
                "conversations": [conv.model_dump() for conv in conversation_list],
                "total": len(conversation_list)
            }
        )
        
    except Exception as e:
        logger.error(f"查询对话列表异常: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"服务器错误: {str(e)}")

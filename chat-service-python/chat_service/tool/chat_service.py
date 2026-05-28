# -*- coding: utf-8 -*-
"""
聊天业务逻辑服务

封装聊天相关的核心业务逻辑，包括：
- 创建对话
- 发送消息并获取AI回复
- 查询对话历史
- 流式聊天
"""

from typing import Optional, List, Dict, AsyncGenerator
from sqlalchemy.ext.asyncio import AsyncSession
from loguru import logger

from chat_service.db.conversation import Conversation
from chat_service.db.message import Message
from chat_service.db.conversation_op import ConversationOp
from chat_service.db.message_op import MessageOp
from chat_service.util.llm_util import call_llm, stream_llm


class ChatService:
    """
    聊天业务服务类
    
    提供聊天相关的所有业务功能。
    """
    
    @staticmethod
    async def create_conversation(
        session: AsyncSession,
        title: str
    ) -> Optional[Conversation]:
        """
        创建新对话
        
        Args:
            session: 数据库Session
            title: 对话标题
            
        Returns:
            创建的Conversation对象，失败返回None
            
        Examples:
            >>> async with async_session_local() as session:
            >>>     conversation = await ChatService.create_conversation(
            >>>         session, "关于Python的讨论"
            >>>     )
            >>>     await session.commit()
        """
        logger.info(f"创建对话: title={title}")
        
        # 调用数据库操作创建对话
        conversation = await ConversationOp.create(session, title)
        
        if conversation:
            await session.commit()
            logger.info(f"对话创建成功: id={conversation.id}")
        
        return conversation
    
    @staticmethod
    async def send_message(
        session: AsyncSession,
        conversation_id: Optional[int],
        user_content: str,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        max_tokens: Optional[int] = None
    ) -> Dict:
        """
        发送消息并获取AI回复（普通模式）
        
        业务流程：
        1. 如果没有conversation_id，创建新对话
        2. 保存用户消息到数据库
        3. 获取对话历史，构建消息上下文
        4. 调用LLM获取AI回复
        5. 保存AI回复到数据库
        6. 更新对话的最后更新时间
        7. 返回完整的对话信息
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID（可选，不提供则创建新对话）
            user_content: 用户消息内容
            model: AI模型名称（可选）
            temperature: 温度参数（可选）
            max_tokens: 最大生成token数（可选）
            
        Returns:
            包含对话ID、用户消息和AI回复的字典
            
        Raises:
            Exception: 业务处理失败时抛出异常
        """
        try:
            # ==================== 1. 处理对话 ====================
            if conversation_id is None:
                # 创建新对话，使用用户消息的前50个字符作为标题
                title = user_content[:50] if len(user_content) > 50 else user_content
                conversation = await ConversationOp.create(session, title)
                if not conversation:
                    raise Exception("创建对话失败")
                conversation_id = conversation.id
                logger.info(f"创建新对话: id={conversation_id}, title={title}")
            else:
                # 验证对话是否存在
                conversation = await ConversationOp.get_by_id(session, conversation_id)
                if not conversation:
                    raise Exception(f"对话不存在: id={conversation_id}")
            
            # ==================== 2. 保存用户消息 ====================
            user_message = await MessageOp.create(
                session,
                conversation_id=conversation_id,
                role="user",
                content=user_content
            )
            if not user_message:
                raise Exception("保存用户消息失败")
            
            # ==================== 3. 构建消息上下文 ====================
            # 获取对话历史（包括刚保存的用户消息）
            history_messages = await MessageOp.get_by_conversation(
                session,
                conversation_id,
                limit=20,  # 最多取最近20条消息作为上下文
                newest_first=True
            )
            
            # 转换为LLM需要的格式
            llm_messages = [
                {"role": msg.role, "content": msg.content}
                for msg in history_messages
            ]
            
            logger.info(
                f"构建消息上下文: conversation_id={conversation_id}, "
                f"history_count={len(llm_messages)}"
            )
            
            # ==================== 4. 调用LLM ====================
            assistant_content = await call_llm(
                messages=llm_messages,
                model=model,
                temperature=temperature,
                max_tokens=max_tokens
            )
            
            # ==================== 5. 保存AI回复 ====================
            assistant_message = await MessageOp.create(
                session,
                conversation_id=conversation_id,
                role="assistant",
                content=assistant_content
            )
            if not assistant_message:
                raise Exception("保存AI回复失败")
            
            # ==================== 6. 更新对话时间 ====================
            await ConversationOp.update_time(session, conversation_id)
            
            # ==================== 7. 提交事务 ====================
            await session.commit()
            
            logger.info(
                f"消息发送成功: conversation_id={conversation_id}, "
                f"user_message_id={user_message.id}, "
                f"assistant_message_id={assistant_message.id}"
            )
            
            # ==================== 8. 返回结果 ====================
            return {
                "conversation_id": conversation_id,
                "user_message": user_message,
                "assistant_message": assistant_message
            }
            
        except Exception as e:
            # 发生异常时回滚事务
            await session.rollback()
            logger.error(f"发送消息失败: {str(e)}", exc_info=True)
            raise
    
    @staticmethod
    async def stream_message(
        session: AsyncSession,
        conversation_id: Optional[int],
        user_content: str,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        max_tokens: Optional[int] = None
    ) -> AsyncGenerator[Dict, None]:
        """
        发送消息并获取AI回复（流式模式）
        
        与send_message类似，但是以流式方式返回AI回复。
        每次yield一个包含增量文本的字典。
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID（可选，不提供则创建新对话）
            user_content: 用户消息内容
            model: AI模型名称（可选）
            temperature: 温度参数（可选）
            max_tokens: 最大生成token数（可选）
            
        Yields:
            Dict: 包含conversation_id, delta, is_final的字典
            
        Examples:
            >>> async for chunk in ChatService.stream_message(
            >>>     session, 1, "讲个故事"
            >>> ):
            >>>     print(chunk["delta"], end="", flush=True)
        """
        try:
            # ==================== 1. 处理对话 ====================
            if conversation_id is None:
                title = user_content[:50] if len(user_content) > 50 else user_content
                conversation = await ConversationOp.create(session, title)
                if not conversation:
                    raise Exception("创建对话失败")
                conversation_id = conversation.id
                await session.commit()
            else:
                conversation = await ConversationOp.get_by_id(session, conversation_id)
                if not conversation:
                    raise Exception(f"对话不存在: id={conversation_id}")
            
            # ==================== 2. 保存用户消息 ====================
            user_message = await MessageOp.create(
                session,
                conversation_id=conversation_id,
                role="user",
                content=user_content
            )
            await session.commit()
            
            # ==================== 3. 构建消息上下文 ====================
            history_messages = await MessageOp.get_by_conversation(
                session,
                conversation_id,
                limit=20,
                newest_first=True
            )
            llm_messages = [
                {"role": msg.role, "content": msg.content}
                for msg in history_messages
            ]
            
            # ==================== 4. 流式调用LLM ====================
            full_content = ""  # 累积完整的AI回复内容
            
            async for chunk in stream_llm(
                messages=llm_messages,
                model=model,
                temperature=temperature,
                max_tokens=max_tokens
            ):
                # 累积内容
                full_content += chunk
                
                # 返回增量数据
                yield {
                    "conversation_id": conversation_id,
                    "delta": chunk,
                    "is_final": False
                }
            
            # ==================== 5. 保存完整的AI回复 ====================
            assistant_message = await MessageOp.create(
                session,
                conversation_id=conversation_id,
                role="assistant",
                content=full_content
            )
            
            # ==================== 6. 更新对话时间 ====================
            await ConversationOp.update_time(session, conversation_id)
            await session.commit()
            
            # ==================== 7. 返回结束标记 ====================
            yield {
                "conversation_id": conversation_id,
                "delta": "",
                "is_final": True
            }
            
            logger.info(
                f"流式消息发送完成: conversation_id={conversation_id}, "
                f"content_length={len(full_content)}"
            )
            
        except Exception as e:
            await session.rollback()
            logger.error(f"流式发送消息失败: {str(e)}", exc_info=True)
            # 抛出异常时也返回错误信息
            yield {
                "conversation_id": conversation_id,
                "delta": f"[错误] {str(e)}",
                "is_final": True
            }
    
    @staticmethod
    async def get_conversation_history(
        session: AsyncSession,
        conversation_id: int
    ) -> Dict:
        """
        获取对话历史
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID
            
        Returns:
            包含对话信息和消息列表的字典
            
        Raises:
            Exception: 对话不存在时抛出异常
        """
        # 查询对话
        conversation = await ConversationOp.get_by_id(session, conversation_id)
        if not conversation:
            raise Exception(f"对话不存在: id={conversation_id}")
        
        # 查询消息列表
        messages = await MessageOp.get_by_conversation(session, conversation_id)
        
        logger.info(
            f"查询对话历史: conversation_id={conversation_id}, "
            f"message_count={len(messages)}"
        )
        
        return {
            "conversation": conversation,
            "messages": messages,
            "total": len(messages)
        }
    
    @staticmethod
    async def list_conversations(
        session: AsyncSession,
        limit: int = 100
    ) -> List[Conversation]:
        """
        获取对话列表
        
        Args:
            session: 数据库Session
            limit: 最多返回的对话数量
            
        Returns:
            对话列表
        """
        conversations = await ConversationOp.list_all(session, limit)
        logger.info(f"查询对话列表: 返回{len(conversations)}条记录")
        return conversations

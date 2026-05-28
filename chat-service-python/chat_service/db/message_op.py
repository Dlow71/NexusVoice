# -*- coding: utf-8 -*-
"""
消息表操作类（Message Operations）

提供消息表的CRUD操作（创建、查询、更新、删除）。
所有操作都是异步的，使用 AsyncSession 进行数据库访问。
"""

from typing import Optional, List
from datetime import datetime
from sqlmodel import select
from sqlalchemy.ext.asyncio import AsyncSession
from loguru import logger

from chat_service.db.message import Message


class MessageOp:
    """
    消息表操作类
    
    封装消息表的所有数据库操作，提供简洁的API。
    所有方法都是静态方法，不需要实例化即可使用。
    """
    
    @staticmethod
    async def get_by_id(session: AsyncSession, message_id: int) -> Optional[Message]:
        """
        根据ID查询消息
        
        Args:
            session: 数据库Session
            message_id: 消息ID
            
        Returns:
            Message对象，如果不存在则返回None
        """
        try:
            result = await session.get(Message, message_id)
            return result
        except Exception as e:
            logger.error(f"查询消息失败 (ID={message_id}): {str(e)}")
            return None
    
    @staticmethod
    async def create(
        session: AsyncSession,
        conversation_id: int,
        role: str,
        content: str
    ) -> Optional[Message]:
        """
        创建新消息
        
        Args:
            session: 数据库Session
            conversation_id: 所属对话ID
            role: 消息角色（user/assistant/system）
            content: 消息内容
            
        Returns:
            创建成功返回Message对象，失败返回None
            
        Examples:
            >>> async with async_session_local() as session:
            >>>     message = await MessageOp.create(
            >>>         session,
            >>>         conversation_id=1,
            >>>         role="user",
            >>>         content="你好"
            >>>     )
            >>>     await session.commit()
        """
        try:
            # 创建消息对象
            message = Message(
                conversation_id=conversation_id,
                role=role,
                content=content,
                created_at=datetime.now()
            )
            
            # 添加到Session
            session.add(message)
            
            # 刷新对象以获取自增ID
            await session.flush()
            await session.refresh(message)
            
            logger.info(
                f"创建消息成功: ID={message.id}, "
                f"conversation_id={conversation_id}, "
                f"role={role}, "
                f"content_length={len(content)}"
            )
            return message
            
        except Exception as e:
            logger.error(f"创建消息失败: {str(e)}")
            await session.rollback()
            return None
    
    @staticmethod
    async def get_by_conversation(
        session: AsyncSession,
        conversation_id: int,
        limit: Optional[int] = None,
        newest_first: bool = False
    ) -> List[Message]:
        """
        查询对话的消息列表
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID
            limit: 最多返回的消息数，默认返回全部
            newest_first: 是否按最新消息在前排序（用于构建上下文）
            
        Returns:
            消息列表，默认按时间正序排列
            
        Examples:
            >>> async with async_session_local() as session:
            >>>     # 获取最新20条消息用于LLM上下文
            >>>     messages = await MessageOp.get_by_conversation(
            >>>         session,
            >>>         1,
            >>>         limit=20,
            >>>         newest_first=True
            >>>     )
        """
        try:
            # 基础查询：过滤会话ID并指定排序方式
            statement = (
                select(Message)
                .where(Message.conversation_id == conversation_id)
                .order_by(
                    Message.created_at.desc() if newest_first else Message.created_at.asc()
                )
            )
            
            # 可选限制返回条数
            if limit is not None:
                statement = statement.limit(limit)
            
            # 执行查询
            result = await session.exec(statement)
            messages = result.all()
            
            # 如果按最新在前排序，调用方通常仍然希望得到按时间正序的数据
            if newest_first:
                messages = list(reversed(messages))
            
            logger.info(
                f"查询对话消息成功: conversation_id={conversation_id}, "
                f"返回{len(messages)}条消息"
            )
            return messages
            
        except Exception as e:
            logger.error(f"查询对话消息失败: {str(e)}")
            return []
    
    @staticmethod
    async def delete(session: AsyncSession, message_id: int) -> bool:
        """
        删除消息
        
        Args:
            session: 数据库Session
            message_id: 消息ID
            
        Returns:
            删除成功返回True，失败返回False
        """
        try:
            message = await session.get(Message, message_id)
            if not message:
                logger.warning(f"消息不存在: ID={message_id}")
                return False
            
            await session.delete(message)
            await session.flush()
            
            logger.info(f"删除消息成功: ID={message_id}")
            return True
            
        except Exception as e:
            logger.error(f"删除消息失败: {str(e)}")
            await session.rollback()
            return False
    
    @staticmethod
    async def delete_by_conversation(
        session: AsyncSession,
        conversation_id: int
    ) -> int:
        """
        删除对话的所有消息
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID
            
        Returns:
            删除的消息数量
        """
        try:
            # 查询所有消息
            statement = select(Message).where(
                Message.conversation_id == conversation_id
            )
            result = await session.exec(statement)
            messages = result.all()
            
            # 删除所有消息
            count = 0
            for message in messages:
                await session.delete(message)
                count += 1
            
            await session.flush()
            
            logger.info(
                f"删除对话消息成功: conversation_id={conversation_id}, "
                f"删除{count}条消息"
            )
            return count
            
        except Exception as e:
            logger.error(f"删除对话消息失败: {str(e)}")
            await session.rollback()
            return 0
    
    @staticmethod
    async def count_by_conversation(
        session: AsyncSession,
        conversation_id: int
    ) -> int:
        """
        统计对话的消息数量
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID
            
        Returns:
            消息数量
        """
        try:
            statement = select(Message).where(
                Message.conversation_id == conversation_id
            )
            result = await session.exec(statement)
            messages = result.all()
            return len(messages)
        except Exception as e:
            logger.error(f"统计消息数量失败: {str(e)}")
            return 0

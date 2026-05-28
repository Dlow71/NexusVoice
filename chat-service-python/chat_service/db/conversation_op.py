# -*- coding: utf-8 -*-
"""
对话表操作类（Conversation Operations）

提供对话表的CRUD操作（创建、查询、更新、删除）。
所有操作都是异步的，使用 AsyncSession 进行数据库访问。
"""

from typing import Optional, List
from datetime import datetime
from sqlmodel import select
from sqlalchemy.ext.asyncio import AsyncSession
from loguru import logger

from chat_service.db.conversation import Conversation


class ConversationOp:
    """
    对话表操作类
    
    封装对话表的所有数据库操作，提供简洁的API。
    所有方法都是静态方法，不需要实例化即可使用。
    """
    
    @staticmethod
    async def get_by_id(session: AsyncSession, conversation_id: int) -> Optional[Conversation]:
        """
        根据ID查询对话
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID
            
        Returns:
            Conversation对象，如果不存在则返回None
            
        Examples:
            >>> async with async_session_local() as session:
            >>>     conversation = await ConversationOp.get_by_id(session, 1)
            >>>     if conversation:
            >>>         print(conversation.title)
        """
        try:
            result = await session.get(Conversation, conversation_id)
            return result
        except Exception as e:
            logger.error(f"查询对话失败 (ID={conversation_id}): {str(e)}")
            return None
    
    @staticmethod
    async def create(session: AsyncSession, title: str) -> Optional[Conversation]:
        """
        创建新对话
        
        Args:
            session: 数据库Session
            title: 对话标题
            
        Returns:
            创建成功返回Conversation对象，失败返回None
            
        Examples:
            >>> async with async_session_local() as session:
            >>>     conversation = await ConversationOp.create(
            >>>         session, 
            >>>         title="关于Python的讨论"
            >>>     )
            >>>     await session.commit()
            >>>     print(f"创建对话成功，ID: {conversation.id}")
        """
        try:
            # 创建对话对象
            conversation = Conversation(
                title=title,
                created_at=datetime.now(),
                updated_at=datetime.now()
            )
            
            # 添加到Session
            session.add(conversation)
            
            # 刷新对象以获取自增ID
            await session.flush()
            await session.refresh(conversation)
            
            logger.info(f"创建对话成功: ID={conversation.id}, title={title}")
            return conversation
            
        except Exception as e:
            logger.error(f"创建对话失败: {str(e)}")
            await session.rollback()
            return None
    
    @staticmethod
    async def update_title(
        session: AsyncSession, 
        conversation_id: int, 
        new_title: str
    ) -> bool:
        """
        更新对话标题
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID
            new_title: 新标题
            
        Returns:
            更新成功返回True，失败返回False
            
        Examples:
            >>> async with async_session_local() as session:
            >>>     success = await ConversationOp.update_title(
            >>>         session, 1, "新的对话标题"
            >>>     )
            >>>     if success:
            >>>         await session.commit()
        """
        try:
            # 查询对话
            conversation = await session.get(Conversation, conversation_id)
            if not conversation:
                logger.warning(f"对话不存在: ID={conversation_id}")
                return False
            
            # 更新标题和时间
            conversation.title = new_title
            conversation.updated_at = datetime.now()
            
            session.add(conversation)
            await session.flush()
            
            logger.info(f"更新对话标题成功: ID={conversation_id}, new_title={new_title}")
            return True
            
        except Exception as e:
            logger.error(f"更新对话标题失败: {str(e)}")
            await session.rollback()
            return False
    
    @staticmethod
    async def update_time(session: AsyncSession, conversation_id: int) -> bool:
        """
        更新对话的最后更新时间
        
        通常在添加新消息时调用，标记对话有新活动。
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID
            
        Returns:
            更新成功返回True，失败返回False
        """
        try:
            conversation = await session.get(Conversation, conversation_id)
            if not conversation:
                return False
            
            conversation.updated_at = datetime.now()
            session.add(conversation)
            await session.flush()
            
            return True
        except Exception as e:
            logger.error(f"更新对话时间失败: {str(e)}")
            return False
    
    @staticmethod
    async def delete(session: AsyncSession, conversation_id: int) -> bool:
        """
        删除对话
        
        注意：删除对话时，关联的消息也会被级联删除（需要在数据库层配置外键约束）。
        
        Args:
            session: 数据库Session
            conversation_id: 对话ID
            
        Returns:
            删除成功返回True，失败返回False
        """
        try:
            conversation = await session.get(Conversation, conversation_id)
            if not conversation:
                logger.warning(f"对话不存在: ID={conversation_id}")
                return False
            
            await session.delete(conversation)
            await session.flush()
            
            logger.info(f"删除对话成功: ID={conversation_id}")
            return True
            
        except Exception as e:
            logger.error(f"删除对话失败: {str(e)}")
            await session.rollback()
            return False
    
    @staticmethod
    async def list_all(session: AsyncSession, limit: int = 100) -> List[Conversation]:
        """
        查询所有对话（按更新时间倒序）
        
        Args:
            session: 数据库Session
            limit: 最多返回的记录数，默认100
            
        Returns:
            对话列表
            
        Examples:
            >>> async with async_session_local() as session:
            >>>     conversations = await ConversationOp.list_all(session, limit=10)
            >>>     for conv in conversations:
            >>>         print(f"{conv.id}: {conv.title}")
        """
        try:
            # 构建查询语句：按更新时间倒序，限制返回数量
            statement = (
                select(Conversation)
                .order_by(Conversation.updated_at.desc())
                .limit(limit)
            )
            
            # 执行查询
            result = await session.exec(statement)
            conversations = result.all()
            
            logger.info(f"查询对话列表成功: 返回{len(conversations)}条记录")
            return conversations
            
        except Exception as e:
            logger.error(f"查询对话列表失败: {str(e)}")
            return []

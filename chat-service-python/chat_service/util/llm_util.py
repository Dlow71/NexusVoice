# -*- coding: utf-8 -*-
"""
LLM调用工具模块

封装 LangChain 的 OpenAI 调用，提供统一的聊天接口。
支持普通模式和流式模式。
"""

from typing import List, Dict, Optional, AsyncGenerator
from loguru import logger
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage

from chat_service.util.config import settings


def create_chat_model(
    model: Optional[str] = None,
    temperature: Optional[float] = None,
    max_tokens: Optional[int] = None,
    streaming: bool = False
) -> ChatOpenAI:
    """
    创建ChatOpenAI模型实例
    
    根据配置和参数创建LangChain的ChatOpenAI对象。
    
    Args:
        model: 模型名称，默认使用配置中的DEFAULT_MODEL
        temperature: 温度参数（0-2），控制输出随机性，默认使用配置中的值
        max_tokens: 最大生成token数，默认使用配置中的值
        streaming: 是否启用流式输出
        
    Returns:
        ChatOpenAI实例
        
    Examples:
        >>> chat_model = create_chat_model(model="gpt-3.5-turbo", temperature=0.7)
        >>> response = await chat_model.ainvoke([HumanMessage(content="你好")])
    """
    return ChatOpenAI(
        model=model or settings.DEFAULT_MODEL,
        temperature=temperature if temperature is not None else settings.LLM_TEMPERATURE,
        max_tokens=max_tokens or settings.LLM_MAX_TOKENS,
        timeout=settings.LLM_TIMEOUT,
        streaming=streaming,
        # OpenAI API配置
        openai_api_key=settings.OPENAI_API_KEY,
        openai_api_base=settings.OPENAI_BASE_URL
    )


def convert_messages(messages: List[Dict[str, str]]) -> List:
    """
    将消息字典列表转换为LangChain消息对象列表
    
    Args:
        messages: 消息字典列表，每个字典包含role和content字段
            例如：[{"role": "user", "content": "你好"}]
            
    Returns:
        LangChain消息对象列表
        
    Examples:
        >>> msgs = [
        >>>     {"role": "system", "content": "你是一个有帮助的AI助手"},
        >>>     {"role": "user", "content": "你好"}
        >>> ]
        >>> langchain_msgs = convert_messages(msgs)
    """
    langchain_messages = []
    
    for msg in messages:
        role = msg.get("role", "user")
        content = msg.get("content", "")
        
        if role == "system":
            langchain_messages.append(SystemMessage(content=content))
        elif role == "assistant":
            langchain_messages.append(AIMessage(content=content))
        else:  # user或其他角色都当作用户消息
            langchain_messages.append(HumanMessage(content=content))
    
    return langchain_messages


async def call_llm(
    messages: List[Dict[str, str]],
    model: Optional[str] = None,
    temperature: Optional[float] = None,
    max_tokens: Optional[int] = None
) -> str:
    """
    调用LLM（普通模式，一次性返回完整结果）
    
    Args:
        messages: 消息列表，格式：[{"role": "user", "content": "你好"}]
        model: 模型名称（可选）
        temperature: 温度参数（可选）
        max_tokens: 最大生成token数（可选）
        
    Returns:
        AI回复的文本内容
        
    Raises:
        Exception: 调用LLM失败时抛出异常
        
    Examples:
        >>> messages = [{"role": "user", "content": "介绍一下Python"}]
        >>> response = await call_llm(messages)
        >>> print(response)
    """
    try:
        # 记录调用信息
        logger.info(
            f"调用LLM: model={model or settings.DEFAULT_MODEL}, "
            f"messages_count={len(messages)}"
        )
        
        # 创建ChatOpenAI实例
        chat_model = create_chat_model(
            model=model,
            temperature=temperature,
            max_tokens=max_tokens,
            streaming=False
        )
        
        # 转换消息格式
        langchain_messages = convert_messages(messages)
        
        # 调用模型
        response = await chat_model.ainvoke(langchain_messages)
        
        # 提取回复内容
        content = response.content
        
        logger.info(f"LLM调用成功，返回内容长度: {len(content)}")
        return content
        
    except Exception as e:
        logger.error(f"LLM调用失败: {str(e)}", exc_info=True)
        raise Exception(f"AI服务调用失败: {str(e)}")


async def stream_llm(
    messages: List[Dict[str, str]],
    model: Optional[str] = None,
    temperature: Optional[float] = None,
    max_tokens: Optional[int] = None
) -> AsyncGenerator[str, None]:
    """
    调用LLM（流式模式，逐块返回结果）
    
    使用异步生成器实现流式输出，每次yield一小块文本内容。
    适用于需要实时显示AI回复的场景。
    
    Args:
        messages: 消息列表，格式：[{"role": "user", "content": "你好"}]
        model: 模型名称（可选）
        temperature: 温度参数（可选）
        max_tokens: 最大生成token数（可选）
        
    Yields:
        str: AI回复的文本片段
        
    Raises:
        Exception: 调用LLM失败时抛出异常
        
    Examples:
        >>> messages = [{"role": "user", "content": "讲个故事"}]
        >>> async for chunk in stream_llm(messages):
        >>>     print(chunk, end="", flush=True)
    """
    try:
        # 记录调用信息
        logger.info(
            f"调用LLM（流式）: model={model or settings.DEFAULT_MODEL}, "
            f"messages_count={len(messages)}"
        )
        
        # 创建ChatOpenAI实例（启用流式）
        chat_model = create_chat_model(
            model=model,
            temperature=temperature,
            max_tokens=max_tokens,
            streaming=True
        )
        
        # 转换消息格式
        langchain_messages = convert_messages(messages)
        
        # 流式调用模型
        chunk_count = 0
        async for chunk in chat_model.astream(langchain_messages):
            if chunk.content:
                chunk_count += 1
                yield chunk.content
        
        logger.info(f"LLM流式调用成功，返回{chunk_count}个chunk")
        
    except Exception as e:
        logger.error(f"LLM流式调用失败: {str(e)}", exc_info=True)
        raise Exception(f"AI服务调用失败: {str(e)}")


async def simple_chat(prompt: str, model: Optional[str] = None) -> str:
    """
    简单聊天接口（便捷方法）
    
    直接传入提示词，快速获取AI回复。
    适用于简单的单轮对话场景。
    
    Args:
        prompt: 用户提示词
        model: 模型名称（可选）
        
    Returns:
        AI回复内容
        
    Examples:
        >>> response = await simple_chat("介绍一下FastAPI")
        >>> print(response)
    """
    messages = [{"role": "user", "content": prompt}]
    return await call_llm(messages, model=model)

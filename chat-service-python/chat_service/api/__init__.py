# -*- coding: utf-8 -*-
"""
API层（API Layer）

定义所有HTTP接口的路由和处理逻辑：
- chat: 聊天相关的API接口
"""

from fastapi import APIRouter

from chat_service.api.chat import router as chat_router

# 创建主路由，所有API都以 /api/v1 为前缀
api_router = APIRouter(prefix="/api/v1")

# 健康检查（与 README 保持一致，位于 /api/v1/health）
@api_router.get("/health")
async def health():
    return {"status": "ok", "service": "chat-service"}

# 挂载子路由
api_router.include_router(chat_router, prefix="/chat", tags=["聊天"])

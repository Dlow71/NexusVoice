# -*- coding: utf-8 -*-
"""
聊天功能测试脚本

用于快速测试聊天服务的各项功能。
使用前请确保服务已启动（python server.py）。
"""

import asyncio
import httpx
import json

# 服务地址
BASE_URL = "http://localhost:8000/api/v1/chat"


async def test_create_conversation():
    """测试创建对话"""
    print("\n" + "=" * 60)
    print("1. 测试创建对话")
    print("=" * 60)
    
    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"{BASE_URL}/conversation",
            json={"title": "测试对话"}
        )
        print(f"状态码: {response.status_code}")
        result = response.json()
        print(f"响应: {json.dumps(result, ensure_ascii=False, indent=2)}")
        
        if result.get("code") == 200:
            conversation_id = result["data"]["id"]
            print(f"✅ 创建成功！对话ID: {conversation_id}")
            return conversation_id
        else:
            print("❌ 创建失败！")
            return None


async def test_send_message(conversation_id: int):
    """测试发送消息（普通模式）"""
    print("\n" + "=" * 60)
    print("2. 测试发送消息（普通模式）")
    print("=" * 60)
    
    async with httpx.AsyncClient(timeout=60.0) as client:
        response = await client.post(
            f"{BASE_URL}/message",
            json={
                "conversationId": conversation_id,
                "content": "你好，请用一句话介绍一下自己"
            }
        )
        print(f"状态码: {response.status_code}")
        result = response.json()
        
        if result.get("code") == 200:
            user_msg = result["data"]["user_message"]["content"]
            ai_msg = result["data"]["assistant_message"]["content"]
            print(f"👤 用户: {user_msg}")
            print(f"🤖 AI: {ai_msg}")
            print("✅ 发送成功！")
        else:
            print(f"❌ 发送失败: {result}")


async def test_stream_message(conversation_id: int):
    """测试流式发送消息"""
    print("\n" + "=" * 60)
    print("3. 测试发送消息（流式模式）")
    print("=" * 60)
    
    async with httpx.AsyncClient(timeout=60.0) as client:
        async with client.stream(
            "POST",
            f"{BASE_URL}/stream",
            json={
                "conversationId": conversation_id,
                "content": "请数1到5，每个数字单独一行"
            }
        ) as response:
            print(f"状态码: {response.status_code}")
            print("🤖 AI回复（流式）: ", end="", flush=True)
            
            async for line in response.aiter_lines():
                if line.startswith("data: "):
                    data_str = line[6:]  # 去掉 "data: " 前缀
                    
                    if data_str == "[DONE]":
                        break
                    
                    if data_str == "heartbeat":
                        continue
                    
                    try:
                        data = json.loads(data_str)
                        if not data.get("is_final"):
                            print(data["delta"], end="", flush=True)
                    except json.JSONDecodeError:
                        pass
            
            print("\n✅ 流式发送完成！")


async def test_get_history(conversation_id: int):
    """测试查询对话历史"""
    print("\n" + "=" * 60)
    print("4. 测试查询对话历史")
    print("=" * 60)
    
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{BASE_URL}/conversation/{conversation_id}/messages"
        )
        print(f"状态码: {response.status_code}")
        result = response.json()
        
        if result.get("code") == 200:
            messages = result["data"]["messages"]
            total = result["data"]["total"]
            print(f"📊 共有 {total} 条消息:")
            print("-" * 60)
            
            for msg in messages:
                role_emoji = "👤" if msg["role"] == "user" else "🤖"
                print(f"{role_emoji} {msg['role']}: {msg['content'][:100]}...")
            
            print("✅ 查询成功！")
        else:
            print(f"❌ 查询失败: {result}")


async def test_list_conversations():
    """测试查询对话列表"""
    print("\n" + "=" * 60)
    print("5. 测试查询对话列表")
    print("=" * 60)
    
    async with httpx.AsyncClient() as client:
        response = await client.get(f"{BASE_URL}/conversations?limit=10")
        print(f"状态码: {response.status_code}")
        result = response.json()
        
        if result.get("code") == 200:
            conversations = result["data"]["conversations"]
            total = result["data"]["total"]
            print(f"📊 共有 {total} 个对话:")
            print("-" * 60)
            
            for conv in conversations:
                print(f"ID: {conv['id']} | 标题: {conv['title']}")
            
            print("✅ 查询成功！")
        else:
            print(f"❌ 查询失败: {result}")


async def main():
    """主测试函数"""
    print("\n" + "🚀" * 30)
    print("聊天服务功能测试")
    print("🚀" * 30)
    
    try:
        # 1. 创建对话
        conversation_id = await test_create_conversation()
        
        if not conversation_id:
            print("\n❌ 创建对话失败，测试终止！")
            return
        
        # 等待一下
        await asyncio.sleep(1)
        
        # 2. 发送消息（普通模式）
        await test_send_message(conversation_id)
        await asyncio.sleep(1)
        
        # 3. 发送消息（流式模式）
        await test_stream_message(conversation_id)
        await asyncio.sleep(1)
        
        # 4. 查询对话历史
        await test_get_history(conversation_id)
        await asyncio.sleep(1)
        
        # 5. 查询对话列表
        await test_list_conversations()
        
        print("\n" + "✅" * 30)
        print("所有测试完成！")
        print("✅" * 30)
        
    except httpx.ConnectError:
        print("\n❌ 连接失败！请确保服务已启动：python server.py")
    except Exception as e:
        print(f"\n❌ 测试出错: {str(e)}")


if __name__ == "__main__":
    # 运行测试
    asyncio.run(main())

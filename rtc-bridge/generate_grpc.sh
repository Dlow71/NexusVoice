#!/bin/bash

# 生成gRPC Python代码
# 从nexusvoice-backend的proto文件生成

set -e

echo "=== 生成gRPC Python代码 ==="

# 创建生成目录
mkdir -p generated

# 生成Python代码
python3 -m grpc_tools.protoc \
  -I../nexusvoice-backend/src/main/proto \
  --python_out=./generated \
  --grpc_python_out=./generated \
  ../nexusvoice-backend/src/main/proto/common.proto \
  ../nexusvoice-backend/src/main/proto/asr_service.proto \
  ../nexusvoice-backend/src/main/proto/tts_service.proto

# 创建__init__.py
touch generated/__init__.py

echo "✅ gRPC Python代码生成完成"
echo "生成文件："
ls -lh generated/*.py







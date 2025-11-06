# RTC桥接进程 - MVP实现

**技术栈**：Python 3.10+ + GStreamer 1.0 + gRPC

**功能**：
- `rtp2grpc_asr.py` - RTP/Opus音频 → gRPC推送到ASR服务
- `grpc2rtp_tts.py` - gRPC接收TTS音频 → RTP/Opus发送到KMS

---

## 依赖安装

### macOS（开发环境）

```bash
# 1. 安装GStreamer
brew install gstreamer gst-plugins-base gst-plugins-good gst-plugins-bad gst-plugins-ugly

# 2. 安装Python依赖
cd rtc-bridge
pip3 install -r requirements.txt

# 3. 安装PyGObject（GStreamer Python绑定）
brew install pygobject3 gtk+3
pip3 install PyGObject
```

### Ubuntu/Debian（生产环境）

```bash
# 1. 安装GStreamer
sudo apt-get update
sudo apt-get install -y \
    gstreamer1.0-tools \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad \
    gstreamer1.0-plugins-ugly \
    gstreamer1.0-libav \
    python3-gi \
    python3-gi-cairo \
    gir1.2-gstreamer-1.0

# 2. 安装Python依赖
pip3 install -r requirements.txt
```

---

## 生成gRPC Python代码

```bash
# 从nexusvoice-backend的proto文件生成Python代码
cd rtc-bridge

python3 -m grpc_tools.protoc \
  -I../nexusvoice-backend/src/main/proto \
  --python_out=./generated \
  --grpc_python_out=./generated \
  ../nexusvoice-backend/src/main/proto/common.proto \
  ../nexusvoice-backend/src/main/proto/asr_service.proto \
  ../nexusvoice-backend/src/main/proto/tts_service.proto
```

---

## 使用说明

### 启动ASR桥接进程

```bash
python3 rtp2grpc_asr.py \
  --listen-host 127.0.0.1 \
  --listen-port 50060 \
  --grpc-host localhost \
  --grpc-port 50051 \
  --session-id test-session-001
```

### 启动TTS桥接进程

```bash
python3 grpc2rtp_tts.py \
  --grpc-host localhost \
  --grpc-port 50052 \
  --rtp-host 127.0.0.1 \
  --rtp-port 50061 \
  --session-id test-session-001
```

---

## 架构说明

### rtp2grpc-asr（上行）

```
KMS → RTP/Opus(48kHz) → UDP监听(127.0.0.1:50060)
        ↓
    GStreamer解码+重采样
        ↓
    PCM 16kHz mono → 20ms帧切片
        ↓
    gRPC双向流 → ASR服务
```

### grpc2rtp-tts（下行）

```
TTS服务 → gRPC双向流
        ↓
    PCM 24kHz/48kHz mono → 接收
        ↓
    GStreamer重采样+编码
        ↓
    Opus 48kHz → RTP封包 → UDP发送(127.0.0.1:50061) → KMS
```

---

## 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `--listen-host` | 127.0.0.1 | RTP监听地址（ASR） |
| `--listen-port` | 50060 | RTP监听端口（ASR） |
| `--rtp-host` | 127.0.0.1 | RTP目标地址（TTS） |
| `--rtp-port` | 50061 | RTP目标端口（TTS） |
| `--grpc-host` | localhost | gRPC服务地址 |
| `--grpc-port` | 50051/50052 | gRPC服务端口 |
| `--session-id` | - | 会话ID（必填） |
| `--debug` | false | 调试模式 |

---

**版本**：MVP v1.0  
**状态**：开发中  
**更新时间**：2025-11-01







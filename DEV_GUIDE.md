# VocalChat 开发指南

> 面向团队成员的端到端开发文档，覆盖架构设计 → 本地开发 → 团队协作 → 服务器上线全流程。

---

## 目录

- [1. 项目定位](#1-项目定位)
- [2. 架构](#2-架构)
- [3. 包结构](#3-包结构)
- [4. 技术栈](#4-技术栈)
- [5. 数据库设计](#5-数据库设计)
- [6. 功能模块与实现](#6-功能模块与实现)
  - [6.1 用户与认证](#61-用户与认证)
  - [6.2 AI 助手管理](#62-ai-助手管理)
  - [6.3 对话管理](#63-对话管理)
  - [6.4 知识库管理](#64-知识库管理)
  - [6.5 基础设施](#65-基础设施)
  - [6.6 Agent 工具调用](#66-agent-工具调用)
  - [6.7 ASR 语音识别](#67-asr-语音识别)
  - [6.8 TTS 语音合成](#68-tts-语音合成)
- [7. LangChain4j Agent 实现详解](#7-langchain4j-agent-实现详解)
- [8. 开发流程](#8-开发流程)
  - [8.1 本地开发](#81-本地开发)
  - [8.2 团队协作](#82-团队协作)
  - [8.3 服务器上线](#83-服务器上线)
- [9. 附录](#9-附录)
  - [9.5 弹性扩展（时间充裕时）](#97-弹性扩展时间充裕时)

---

## 1. 项目定位

VocalChat 是一个 **AI Agent 语音助手** Web 应用。用户可以：

- 文字/语音与 AI 对话（流式 SSE）
- 创建和管理多个 AI 助手（不同性格、不同知识库）
- **AI Agent 自主调用工具链完成任务**
- **实时语音通话**（ASR + LLM + TTS 全链路）

核心架构原则：**Controller-Service-Mapper 三层架构**，借助 LangChain4j 的 AI Services + Agentic 模块实现 Agent。

---

## 2. 架构

采用 **Controller-Service-Mapper 三层架构**，辅以 Infrastructure 共享层。

### 架构图

```
┌──────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Vite)                      │
│              REST API / SSE / WebSocket                    │
└──────────────────────────────────────────────────────────┘
                            │
┌───────────────────────────┼───────────────────────────────┐
│                    Controller 层                           │
│  UserController | AIAssistantController | KnowledgeBaseCtrl│
│  VoiceChatWebSocketHandler | HealthController              │
│                                                            │
│  职责：接收请求、参数校验、调用 Service、返回 BaseResult            │
└───────────────────────────┼───────────────────────────────┘
                            │
┌───────────────────────────┼───────────────────────────────┐
│                     Service 层                             │
│  UserService | AIAssistantService | KnowledgeBaseService   │
│  VoiceChatService (ASR+TTS+LLM 编排)                        │
│  AgentService (Agent Loop + Tool Calling)                  │
│                                                            │
│  职责：业务逻辑、Agent 编排、工具注册、事务管理                    │
└───────────────────────────┼───────────────────────────────┘
                            │
┌───────────────────────────┼───────────────────────────────┐
│                     Mapper 层                              │
│  UserMapper | AIAssistantMapper | KnowledgeBaseMapper      │
│  DialogueMapper | KnowledgeBaseFileMapper                  │
│                                                            │
│  职责：MyBatis-Plus BaseMapper，数据库 CRUD                   │
└───────────────────────────┼───────────────────────────────┘
                            │
┌───────────────────────────┼───────────────────────────────┐
│                     Infrastructure（共享层）                 │
│  config/     — 配置类（Redis, MinIO/COS, LLM, WebSocket）   │
│  shared/     — 横切工具（JwtUtil, UserContext, ErrorEnum）   │
│  external/   — 外部服务适配（LLM, ASR, TTS, Email, 对象存储）  │
└──────────────────────────────────────────────────────────┘
```

### 各层职责

| 层              | 职责                      | 禁止       |
| -------------- | ----------------------- | -------- |
| Controller     | 接收请求、参数校验、调用 Service    | 不写业务逻辑   |
| Service        | 业务逻辑、Agent 编排、事务管理、工具注册 | 不直接操作数据库 |
| Mapper         | MyBatis-Plus CRUD       | 不写业务逻辑   |
| Infrastructure | 配置、拦截器、外部服务适配、横切工具      | 不侵入业务    |

### 关键设计原则

- **实体直接映射数据库**：`entity/` 下的类用 MyBatis-Plus 注解标注，字段直接对应表列
- **Service 内聚业务**：业务逻辑全部在 Service 层
- **Spring 事件解耦**：跨模块通知（如 LLM 回答完成 → TTS 语音合成）用 Spring 事件，不直接调用
- **统一响应格式**：`@AutoResult` 注解自动包装为 `BaseResult<T>`，前端统一解析
- **统一鉴权**：`UserInterceptor` + `@SkipToken` 控制认证

---

## 3. 包结构

```
backend/src/main/java/host/hunger/vocalchat/
├── VocalchatApplication.java
├── controller/                    # REST 控制器
│   ├── UserController.java
│   ├── AIAssistantController.java
│   ├── KnowledgeBaseController.java
│   └── HealthController.java
├── websocket/                     # WebSocket 端点
│   └── VoiceChatWebSocketHandler.java
├── service/                       # 业务服务
│   ├── UserService.java
│   ├── AIAssistantService.java
│   ├── KnowledgeBaseService.java
│   ├── KnowledgeBaseFileService.java
│   ├── VoiceChatService.java      # ASR → LLM → TTS 编排
│   └── AgentService.java          # Agent 注册中心 + Agent Loop
├── service/agent/                 # Agent 工具定义
│   ├── ToolRegistry.java          # 工具注册表（注解自动扫描）
│   └── tools/                     # 具体工具实现
│       ├── ThemeTool.java         # 浏览器深色/亮色切换
│       ├── ConversationTool.java  # 对话搜索、保存
│       ├── KnowledgeBaseTool.java # 知识库检索、管理
│       ├── EmailTool.java         # 邮件发送
│       ├── AssistantConfigTool.java # 助手配置修改
│       └── TtsTool.java           # TTS 语音播报
├── entity/                        # 数据库实体（对应 DO）
│   ├── User.java
│   ├── AIAssistant.java
│   ├── KnowledgeBase.java
│   ├── KnowledgeBaseFile.java
│   └── Dialogue.java
├── dto/                           # 数据传输对象
│   ├── request/                   # 请求体
│   │   ├── UserRegisterRequest.java
│   │   ├── UserLoginRequest.java
│   │   ├── CreateAssistantRequest.java
│   │   ├── StreamRequest.java
│   │   └── ...
│   └── response/                  # 响应 VO
│       ├── UserInfoVO.java
│       ├── AIAssistantVO.java
│       └── ...
├── mapper/                        # MyBatis-Plus Mapper
│   ├── UserMapper.java
│   ├── AIAssistantMapper.java
│   ├── KnowledgeBaseMapper.java
│   ├── KnowledgeBaseFileMapper.java
│   └── DialogueMapper.java
├── event/                         # Spring 事件
│   ├── QuestionAnsweredEvent.java
│   └── DomainEventListener.java
├── infrastructure/
│   ├── config/                    # 配置类
│   │   ├── WebConfig.java         # 拦截器注册
│   │   ├── RedisConfig.java
│   │   ├── MinIOConfig.java
│   │   ├── LLMConfig.java         # LLM Bean 配置
│   │   ├── AsrConfig.java         # ASR Bean 配置
│   │   ├── TtsConfig.java         # TTS Bean 配置
│   │   └── WebSocketConfig.java
│   ├── external/                  # 外部服务适配
│   │   ├── llm/
│   │   │   └── QwenChatService.java     # 通义千问适配
│   │   ├── asr/
│   │   │   └── TencentAsrService.java   # 腾讯 ASR 适配
│   │   ├── tts/
│   │   │   └── TencentTtsService.java   # 腾讯 TTS 适配
│   │   ├── email/
│   │   │   └── ResendEmailService.java
│   │   └── storage/
│   │       ├── MinIOStorageService.java   # 本地开发
│   │       └── COSObjectStorageService.java # 生产环境
│   └── interceptor/
│       ├── UserInterceptor.java   # Token 鉴权
│       └── RequestInterceptor.java # TraceLog
└── shared/
    ├── context/UserContext.java   # ThreadLocal 用户信息
    ├── util/JwtUtil.java
    ├── enums/ErrorEnum.java
    ├── result/BaseResult.java
    ├── annotation/AutoResult.java
    ├── annotation/SkipToken.java
    └── exception/BaseException.java
```

---

## 4. 技术栈

### 4.1 选型总览

| 层        | 技术               | 版本      | 等级    | 说明                      |
| -------- | ---------------- | ------- | ----- | ----------------------- |
| 语言       | Java             | 21      | 🟢 生产 | 虚拟线程                    |
| 框架       | Spring Boot      | 3.5.4   | 🟢 生产 |                         |
| ORM      | MyBatis-Plus     | 3.5.7   | 🟢 生产 | 无外键约束                   |
| LLM 框架   | LangChain4j      | 1.13.0  | 🟢 生产 | AI Services + Agentic   |
| LLM 平台   | 通义千问 (DashScope) | —       | 🟢 生产 | 流式对话 + Function Calling |
| 邮件       | Resend           | —       | 🟢 生产 | 免费 100 封/天，生产级送达率       |
| ASR      | 腾讯云实时语音识别        | —       | 🟢 生产 | 免费 5 小时/月               |
| TTS      | 腾讯云语音合成          | —       | 🟢 生产 | 免费 100 万字符/月            |
| 数据库      | MySQL            | 8.0     | 🟢 生产 | Docker 自部署              |
| 缓存       | Redis            | 7       | 🟢 生产 | Docker 自部署              |
| 对象存储（生产） | 腾讯云 COS/MinIO    | —       | 🟢 生产 | 免费 50 GB                |
| 前端       | Vue 3 + Vite     | 3.5 / 7 | 🟢 生产 | Composition API         |
| 构建       | Maven            | wrapper | 🟢 生产 |                         |
| 容器化      | Docker Compose   | v2      | 🟢 生产 | 全栈一键部署                  |

## 5. 数据库设计

### 5.1 数据库表

| 表名                    | 说明    | 关键字段                                                                   |
| --------------------- | ----- | ---------------------------------------------------------------------- |
| `user`                | 用户    | id, name, email, password                                              |
| `ai_assistant`        | AI 助手 | id, user_id, name, description, assistant_character, knowledge_base_id |
| `dialogue`            | 对话    | id, ai_assistant_id, contexts(JSON)                                    |
| `knowledge_base`      | 知识库   | id, user_id, name, description, status                                 |
| `knowledge_base_file` | 知识库文件 | id, knowledge_base_id, file_name, storage_key, status                  |

### 5.2 语音与 Agent 相关表

```sql
-- 语音通话会话记录
CREATE TABLE `voice_call_session` (
    `id` VARCHAR(36) NOT NULL COMMENT '主键，UUID',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `ai_assistant_id` VARCHAR(36) NOT NULL COMMENT 'AI助手ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/COMPLETED/HANGUP',
    `duration_seconds` INT DEFAULT 0 COMMENT '通话时长(秒)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 工具调用日志（可选，用于调优和分析）
CREATE TABLE `agent_tool_call_log` (
    `id` VARCHAR(36) NOT NULL COMMENT '主键，UUID',
    `user_id` VARCHAR(36) NOT NULL,
    `session_id` VARCHAR(36) NOT NULL COMMENT '对话或通话 session',
    `tool_name` VARCHAR(100) NOT NULL COMMENT '被调用的工具名称',
    `input_params` TEXT COMMENT '工具入参 JSON',
    `output_result` TEXT COMMENT '工具出参 JSON',
    `duration_ms` INT COMMENT '执行耗时(毫秒)',
    `success` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session` (`session_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 6. 功能模块与实现

### 6.1 用户与认证

| 功能    | 接口                                          | 说明                   |
| ----- | ------------------------------------------- | -------------------- |
| 注册    | `POST /api/public/user/register`            | 邮箱 + 验证码注册，返回 JWT    |
| 登录    | `POST /api/public/user/login`               | 邮箱 + 密码登录，返回 JWT     |
| 发送验证码 | `POST /api/public/user/getVerificationCode` | 6 位数字，Redis 5 分钟有效   |
| 登出    | `POST /api/public/user/logout`              | 清除 Redis 中 Token     |
| 用户信息  | `GET /api/public/user/info`                 | 返回 id/nickName/email |

**鉴权机制：**

- JWT 通过 HTTP `Token` Header 或 WebSocket `?token=` 查询参数传递
- `UserInterceptor` 拦截所有请求验证 Token，写入 `UserContext`（ThreadLocal）
- `@SkipToken` 注解可跳过鉴权（注册/登录/验证码接口）

### 6.2 AI 助手管理

| 功能   | 接口                                            | 说明                 |
| ---- | --------------------------------------------- | ------------------ |
| 创建助手 | `POST /api/aiAssistant/createNewAssistant`    | 名称、描述、角色设定、可选关联知识库 |
| 助手列表 | `GET /api/aiAssistant/aiAssistants`           | 当前用户的所有助手          |
| 修改配置 | `POST /api/aiAssistant/modifyAssistantConfig` | 修改名称/描述/角色/知识库     |
| 删除助手 | `DELETE /api/aiAssistant/deleteAssistant`     | 删除助手及关联对话          |

### 6.3 对话管理

| 功能   | 接口                                              | 说明                   |
| ---- | ----------------------------------------------- | -------------------- |
| 流式对话 | `POST /api/aiAssistant/streamGenerateReply`     | SSE 流式返回，支持深度思考和联网搜索 |
| 对话记录 | `GET /api/aiAssistant/{id}/conversation-log`    | 返回 `[角色, 内容]` 二维数组   |
| 重置对话 | `DELETE /api/aiAssistant/{id}/conversation-log` | 清空并新建空对话             |

**SSE 事件协议：** `started` → `thinking`(可选) → `token`(流式) → `done`，心跳 15 秒，超时 30 分钟。

### 6.4 知识库管理

| 功能    | 接口                                                  | 说明                           |
| ----- | --------------------------------------------------- | ---------------------------- |
| 创建知识库 | `POST /api/knowledge-base`                          | 名称 + 描述                      |
| 知识库列表 | `GET /api/knowledge-base`                           | 当前用户的所有知识库                   |
| 知识库详情 | `GET /api/knowledge-base/{id}`                      |                              |
| 修改知识库 | `PUT /api/knowledge-base/{id}`                      |                              |
| 删除知识库 | `DELETE /api/knowledge-base/{id}`                   | 自动清除关联文件                     |
| 上传文件  | `POST /api/knowledge-base/{id}/file`                | multipart，支持 PDF/TXT/MD/DOCX |
| 文件列表  | `GET /api/knowledge-base/{id}/file`                 |                              |
| 文件状态  | `GET /api/knowledge-base/{id}/file/{fileId}/status` |                              |
| 删除文件  | `DELETE /api/knowledge-base/{id}/file/{fileId}`     |                              |

### 6.5 基础设施

- **统一响应格式**：`@AutoResult` + `BaseResult<T>`（code/message/success/data），Controller 方法标注 `@AutoResult` 即可自动包装
- **全局异常处理**：`GlobalExceptionHandler` 捕获 `BaseException`，自动映射 `ErrorEnum` 错误码
- **请求链路追踪**：`RequestInterceptor` 注入 `TRACE_ID` 到 MDC
- **认证上下文**：`UserContext`（ThreadLocal）存储当前用户信息，通过 `UserContext.require()` 获取

---

### 6.6 Agent 工具调用

#### 设计目标

用户用自然语言提出需求，LLM 自动从工具列表中选择合适的工具，按需链式调用，完成任务。

```
用户："帮我把助手改成活泼风格，然后把知识库里 Spring Boot 文档的要点总结播给我听"

Agent 执行:
  第1轮: LLM 决策 → 调用 AssistantConfigTool(assistantId, character="活泼")
  第2轮: LLM 决策 → 调用 KnowledgeBaseSearchTool(keyword="Spring Boot")
  第3轮: LLM 决策 → 调用 LLM 总结文档
  第4轮: LLM 决策 → 调用 TtsTool(summary)
  第5轮: LLM 判断完成 → 返回结果给用户
```

---

### 6.7 ASR 语音识别

#### 设计目标

浏览器采集麦克风音频 → WebSocket 实时推流到后端 → 后端转发给腾讯云 ASR → 识别结果回传前端。

#### 链路

```
浏览器麦克风 (PCM 16kHz 16bit mono)
    │ WebSocket (二进制帧, 每 40ms 一块)
    ▼
VoiceChatWebSocketHandler
    │ 调用
    ▼
TencentAsrService
    │ WebSocket 连接到腾讯云 ASR
    ▼
腾讯云实时语音识别
    │ 返回识别结果 (中间/最终)
    ▼
VoiceChatWebSocketHandler
    │ WebSocket (JSON 消息)
    │ {"type":"asr_interim","text":"今天"}    ← 中间结果
    │ {"type":"asr_final","text":"今天天气"}  ← 最终结果
    ▼
浏览器播放/显示
```

---

### 6.8 TTS 语音合成

#### 设计目标

LLM 回复文本 → TTS 服务合成语音 → WebSocket 音频帧推给前端播放。

#### 链路

```
LLM 回复完成（QuestionAnsweredEvent）
    │ 文本
    ▼
VoiceChatService.handleAnswer(text)
    │ 调用
    ▼
TencentTtsService.synthesize(text, voiceType, speed)
    │ HTTP / WebSocket 调用腾讯云 TTS
    ▼
音频流 (MP3/PCM bytes)
    │ WebSocket 二进制帧
    ▼
浏览器播放 (<audio> 或 Web Audio API)
```

#### 语音 WebSocket 消息协议

**客户端 → 服务端**

```json
// 控制消息（JSON 文本）
{"type": "config", "aiAssistantId": "xxx", "speaker": "101001"}
{"type": "hangup"}

// 音频数据（二进制帧）
<PCM 16kHz 16bit mono bytes>
```

**服务端 → 客户端**

```json
// 状态/结果消息（JSON 文本）
{"type": "asr_interim", "text": "今天"}           // ASR 中间结果
{"type": "asr_final", "text": "今天天气怎么样"}    // ASR 最终结果
{"type": "llm_token", "text": "今天"}             // LLM 流式 token
{"type": "agent_thinking", "text": "我需要..."}   // Agent 思考过程
{"type": "agent_tool_call", "tool": "xxx", "args": {...}}
{"type": "agent_tool_result", "tool": "xxx", "success": true, "summary": "..."}
{"type": "hangup"}                                // 通话结束
```

---

## 8. 开发流程

### 8.1 本地开发

#### 环境准备

| 软件        | 版本     | 说明            |
| --------- | ------ | ------------- |
| JDK       | 21+    |               |
| Maven     | 3.9+   |               |
| Node.js   | 20+    |               |
| MySQL     | 8.0    | 建议用 Docker    |
| Redis     | 7      | 建议用 Docker    |
| MinIO/COS | latest | 对象存储，用 Docker |

#### 第一步：克隆项目

```bash
git clone <repo-url>
cd vocalchat
git checkout -b dev/your-name
```

#### 第二步：启动中间件（Docker）

```bash
# 只启动 MySQL + Redis + MinIO，不启动应用（应用 IDE 启动方便调试）
docker compose up -d mysql redis minio minio-init
```

#### 第三步：配置环境变量

创建 `.env` 文件（从 `api.md` 对应的环境变量和 `.env.example` 复制）

#### 第四步：启动后端

**方式 A：IDE（推荐开发调试）**

```bash
# IntelliJ IDEA 打开 backend/ 目录，运行 VocalchatApplication.main()
# 设置 VM options 或 environment variables 指向 .env
```

**方式 B：命令行**

```bash
cd backend
./mvnw.cmd spring-boot:run    # Windows
./mvnw spring-boot:run        # Mac/Linux
```

#### 第五步：启动前端

```bash
cd frontend
npm install
npm run dev
# 打开 http://localhost:5173
```

前端的 Vite 开发服务器已配置代理：`/api/*` → `http://localhost:8080`，`/ws/*` → `ws://localhost:8080`。

#### 第六步：验证

1. 浏览器打开 `http://localhost:5173`
2. 注册账号 → 登录 → 创建 AI 助手
3. 进入对话页，发一条消息，确认 SSE 流式回复正常
4. 上传一个文件到知识库
5. （语音功能）进入通话页，允许麦克风权限，确认 ASR 识别 + TTS 播放

---

### 8.2 团队协作

#### 项目难度评估

| 维度           | 评级          | 说明                                 |
| ------------ | ----------- | ---------------------------------- |
| 整体难度         | ⭐⭐⭐⭐ (6/10) | 技术栈标准，Agent 设计是核心挑战                |
| CRUD 模块      | ⭐⭐ (3/10)   | Controller-Service-Mapper 模式，有模板可循 |
| Agent Loop   | ⭐⭐⭐⭐ (7/10) | 需要理解 LLM Function Calling + 多轮决策   |
| ASR/TTS 集成   | ⭐⭐⭐⭐ (7/10) | WebSocket 二进制帧 + 云服务流式对接           |
| Agent SSE 事件 | ⭐⭐⭐ (5/10)  | 自定义 SSE 事件类型，现有前端兼容                |
| 部署运维         | ⭐⭐⭐ (4/10)  | Docker Compose 编排，无 K8s            |

#### 会议节奏

| 频次    | 形式        | 时长  | 内容                                |
| ----- | --------- | --- | --------------------------------- |
| 每阶段结束 | 阶段 Review | -   | Demo 可工作功能、Code Review 回顾、下阶段任务分派 |

> 一周一次周会足够——5 人团队沟通成本低。每日异步汇报确保信息透明，不额外占用时间。

#### 四阶段开发计划（6 周 / 1.5 个月）

```
Week 1 ──────── Week 2 ──────── Week 3 ──────── Week 4 ──────── Week 5 ──────── Week 6
│               │               │               │               │               │
│◄ 阶段一 ──►│◄─────────── 阶段二 ───────────────────►│◄── 阶段三 ──►│◄─ 阶段四 ─┤
│   基础建设    │          Agent 核心 + 工具            │   语音管线    │  集成上线   │
│    1 周      │              2.5 周                  │   1.5 周     │   1 周     │
```

---

##### 阶段一：基础建设（Week 1，1 周）

**目标：每个人完成一个端到端模块，统一技术基线，为 Agent 开发扫清道路。**

> 为什么这样设计：让每个人在同一个架构模式下各自主导一个模块，Controller → Service → Mapper → DB 走通一遍。阶段结束时，所有人对三层架构的代码风格达成共识，不会出现"你写的和我不一样"。

| 成员  | 模块            | 内容                                                                        | Agent 关联                    |
| --- | ------------- | ------------------------------------------------------------------------- | --------------------------- |
| A   | 项目脚手架 + 共享层   | 项目初始化、三层包结构、BaseResult、ErrorEnum、JWT 鉴权、UserContext、全局异常处理、Docker Compose | 为 Agent 框架打好地基              |
| B   | 用户模块          | 注册/登录/登出/信息、验证码发送（Resend）、密码 BCrypt                                       | —                           |
| C   | AI 助手 + 对话    | 助手 CRUD、对话上下文管理、SSE 流式对话（通义千问）                                            | Agent 的对话入口                 |
| D   | 知识库           | 知识库 CRUD、文件上传（MinIO 本地 / COS 生产）、文件状态                                     | Agent 的知识检索工具数据源            |
| E   | LLM 集成 + 对象存储 | 通义千问 Streaming Chat Model 配置、LangChain4j 集成、MinIO/COS 存储适配器               | LLM 调用基础设施、Agent 的"大脑"和"记忆" |

**阶段一验收标准：**

- [ ] `docker compose up -d` 一键启动全栈
- [ ] 注册 → 登录 → 创建助手 → SSE 对话 → 上传文件到知识库 → 全流程通
- [ ] 所有人用同一个代码风格（Controller-Service-Mapper 模式一致）

---

##### 阶段二：Agent 核心 + 工具（Week 2 - Week 4 前半，2.5 周）

**目标：Agent 框架上线，每个人至少实现 2 个工具。**

> 这是整个项目的核心阶段。Agent Loop 和 ToolRegistry 由技术负责人设计接口，但**工具的编写分配给每个人**。Agent 过程通过 SSE 事件（agent_thinking / agent_tool_call / agent_tool_result）在现有前端对话流中观察。

**Agent 框架共建（前 3 天，A 主导，全员参与设计讨论）：**

| 产出             | 负责人         | 说明                                                                 |
| -------------- | ----------- | ------------------------------------------------------------------ |
| `ToolRegistry` | A           | 工具注册表：自动扫描 `@Tool` 注解、生成 `ToolSpecification`、执行调度                  |
| `AgentService` | A + B（pair） | Agent Loop：LLM 决策 → 工具执行 → 结果反馈 → 循环 → 最终回复                        |
| Agent SSE 事件协议 | A + E       | 定义 `agent_thinking` / `agent_tool_call` / `agent_tool_result` 事件格式 |
| 工具接口规范         | A           | `@Tool` 注解规范、参数 `@P` 描述要求、返回值格式约定                                  |

**工具分配（每人至少 2 个，确保所有人都写了 Agent 工具）：**

| 成员  | 工具 1                                      | 工具 2                                      | Agent 体验点             |
| --- | ----------------------------------------- | ----------------------------------------- | --------------------- |
| A   | （框架开发，不分配工具）                              |                                           | Agent Loop 设计、多工具链式调度 |
| B   | `AssistantConfigTool` — 修改助手配置（名称/性格/知识库） | `ConversationSearchTool` — 按关键词搜索历史对话     | LLM 决定何时修改配置、如何搜索     |
| C   | `KnowledgeBaseSearchTool` — 检索知识库文档内容     | `KnowledgeBaseSummaryTool` — 统计知识库文档数和切片数 | LLM 理解知识库结构、按需检索      |
| D   | `ConversationSaveTool` — 保存对话到知识库         | `EmailTool` — 发送邮件（对话摘要、通知）               | LLM 判断是否值得保存、邮件内容生成   |
| E   | `ThemeTool` — 浏览器深色/亮色切换                  | `TtsTool` — 将指定文本通过 TTS 语音播报              | 外部动作触发、多模态输出          |

**阶段二验收标准：**

- [ ] 用户说"帮我把助手性格改活泼"→ LLM 自动调用 `AssistantConfigTool` → 性格生效
- [ ] 用户说"帮我搜索知识库里关于 Spring 的文档，总结后发到我邮箱"→ 3 个工具链式调用成功
- [ ] SSE 事件流包含 `agent_thinking` / `agent_tool_call` / `agent_tool_result`，可在浏览器开发者工具 Network 面板观察
- [ ] Agent Loop 有 `maxIterations=10` 保护，不会死循环
- [ ] 每个工具的 `@Tool` description 经过全员 review（确保 LLM 能正确理解和使用）

---

##### 阶段三：语音管线（Week 4 后半 - Week 5 结束，1.5 周）

**目标：ASR → Agent → TTS 全链路跑通，实现语音对话。**

| 成员          | 内容                                               | Agent 关联                                       |
| ----------- | ------------------------------------------------ | ---------------------------------------------- |
| A + C（pair） | 腾讯云 ASR 集成：`TencentAsrService` 实时语音识别            | ASR 最终结果作为 Agent 输入                            |
| B + D（pair） | 腾讯云 TTS 集成：`TencentTtsService` 流式语音合成            | Agent/LM 输出作为 TTS 输入                           |
| E           | `VoiceChatWebSocketHandler` + `VoiceChatService` | 语音 WebSocket 端点：二进制帧收发、ASR → Agent → TTS 全链路编排 |

**语音 WebSocket 端点：**

| 成员  | 内容                                                                 |
| --- | ------------------------------------------------------------------ |
| E   | `VoiceChatWebSocketHandler` — WebSocket 二进制帧（音频）+ JSON 控制消息、二进制帧收发 |
| B   | `VoiceChatService` — ASR 结果 → Agent（或直接 LLM）→ TTS 整个编排             |

**阶段三验收标准：**

- [ ] 浏览器麦克风 → WebSocket → ASR 识别 → 文本正确
- [ ] 文本 → LLM/Agent → 流式 token 前端显示 + TTS 合成 → 浏览器播放语音
- [ ] 挂断通话正常清理资源
- [ ] 延迟可接受（ASR 延迟 < 500ms，TTS 首帧 < 1s）

---

##### 阶段四：集成上线（Week 6，1 周）

**目标：端到端测试、部署上线、文档完善。**

| 成员    | 内容                                             |
| ----- | ---------------------------------------------- |
| A + D | 生产环境部署：服务器配置、COS 切生产、Docker Compose 生产编排、HTTPS |
| B + C | 全链路测试：Agent 多工具链测试、语音通话压力测试、异常场景覆盖             |
| E     | API 文档 + 集成测试用例编写 + Agent 工具回归测试               |
| 全员    | Bug 修复、日志检查、`DEV_GUIDE.md` 更新、`api.md` 最终版本    |

**阶段四验收标准：**

- [ ] 生产服务器可公网访问，HTTPS 生效
- [ ] 注册 → 文字对话 → Agent 工具调用 → 语音通话全流程通过
- [ ] 异常场景有合理的错误提示（网络断开、API Key 无效、工具执行失败）
- [ ] Docker Compose 一键部署验证通过

---

#### 各成员 Agent 开发体验总结

| 成员  | Agent 相关经历                                 | 学到什么                       |
| --- | ------------------------------------------ | -------------------------- |
| A   | 设计 Agent Loop、ToolRegistry、Agent SSE 协议    | Agent 架构设计、LLM 调度、多工具编排    |
| B   | 实现 2 个工具 + pair Agent Loop                 | 工具设计、LLM 决策理解、Agent 核心逻辑   |
| C   | 实现 2 个工具 + ASR 语音识别集成                      | 工具设计、实时音频流处理、语音作为 Agent 输入 |
| D   | 实现 2 个工具 + TTS 语音合成集成                      | 工具设计、语音合成、Agent 输出多模态化     |
| E   | 实现 2 个工具 + Agent SSE 协议 + 语音 WebSocket 全链路 | 工具设计、SSE 自定义事件、实时音频流处理     |

> 每个人至少做了 2 个工具 + 至少参与了 1 个 Agent 核心环节（Loop/ASR/TTS/SSE 协议），确保全团队对 Agent 开发有完整的体感。

#### Git 工作流

```
master ─────────────────────────────────────────────
    │
    ├── feat/shared-infra         ← 阶段一：A
    ├── feat/user-auth            ← 阶段一：B
    ├── feat/ai-assistant         ← 阶段一：C
    ├── feat/knowledge-base       ← 阶段一：D
    ├── feat/llm-storage          ← 阶段一：E
    │
    ├── feat/agent-core           ← 阶段二：A+B
    ├── feat/agent-tools-b        ← 阶段二：B 的工具
    ├── feat/agent-tools-c        ← 阶段二：C 的工具
    ├── feat/agent-tools-d        ← 阶段二：D 的工具
    ├── feat/agent-tools-e        ← 阶段二：E 的工具
    │
    ├── feat/asr-integration      ← 阶段三：A+C
    ├── feat/tts-integration      ← 阶段三：B+D
    ├── feat/voice-websocket      ← 阶段三：E
    │
    └── feat/deploy-polish        ← 阶段四：全员
```

#### 每日流程

```bash
# 1. 从 master 同步
git checkout master && git pull origin master

# 2. 合并到自己的分支
git checkout feat/your-feature && git merge master

# 3. 开发
# ... 写代码 ...

# 4. 提交（一个功能点一个 commit）
git add <specific-files>
git commit -m "<type>(<scope>): <subject>"

# 5. 推送
git push origin feat/your-feature

# 6. 发起 PR → 指定 reviewer → Code Review → 合并
```

#### Commit 规范

```
feat(agent): 实现 ToolRegistry 工具注册表
feat(agent): 实现 AgentService Agent Loop 核心逻辑
feat(agent): 实现 AssistantConfigTool
feat(asr): 集成腾讯云实时语音识别
feat(tts): 集成腾讯云流式语音合成
feat(user): 实现用户注册与登录
fix(agent): 修复 Agent Loop 死循环问题
chore(config): 添加 ASR/TTS 环境变量模板
docs(guide): 更新 Agent 工具开发模板
```

#### Code Review 要点

- Controller 层：不做业务逻辑，只校验参数和调用 Service
- Service 层：核心业务逻辑，单元测试覆盖
- Mapper 层：不写自定义 SQL 除非 MyBatis-Plus 无法满足
- **工具的 `@Tool` description 必须清晰、具体**——LLM 据此判断何时使用，团队成员互相 review
- Agent Loop 必须有 `maxIterations`，防止死循环
- 所有外部 API 调用必须有超时和错误处理
- **阶段二开始，每个 PR 至少包含一个工具的实现或 Agent 相关改动**

---

### 8.3 服务器上线

#### 8.3.1 服务器要求

| 资源     | 配置                      |
| ------ | ----------------------- |
| CPU    | 2 核                     |
| 内存     | 4 GB                    |
| 磁盘     | 20 GB                   |
| 系统     | Ubuntu 22.04 / CentOS 8 |
| Docker | 24+                     |
| 开放端口   | 80, 443 (HTTPS)         |

#### 8.3.2 准备服务器

```bash
# SSH 连接服务器
ssh root@<server-ip>

# 安装 Docker
curl -fsSL https://get.docker.com | bash

# 安装 Docker Compose
apt install docker-compose-plugin

# 创建应用目录
mkdir -p /opt/vocalchat
cd /opt/vocalchat
```

#### 8.3.3 部署流程

**CI/CD 自动部署**

#### 8.3.4 上线检查清单

- [ ] `.env` 中所有占位符已填入真实值
- [ ] MySQL/Redis 密码为强密码
- [ ] JWT Secret 为强随机字符串
- [ ] 云服务 API Key 已配置且有效
- [ ] `docker compose ps` 所有容器 Running / Healthy
- [ ] `curl http://<server>/api/health` 返回 200
- [ ] 浏览器访问前端，注册→登录→对话 全流程通
- [ ] WebSocket 连接成功（语音通话）
- [ ] 日志中无异常 ERROR（`docker compose logs backend | grep ERROR`）

#### 8.3.5 常用运维命令

```bash
# 查看所有容器状态
docker compose ps

# 查看后端日志（最近 100 行）
docker compose logs --tail 100 backend

# 实时跟踪日志
docker compose logs -f backend

# 重启单个服务
docker compose restart backend

# 更新部署（拉取代码后）
git pull && docker compose build backend && docker compose up -d backend

# 数据库备份
docker exec mysql mysqldump -u root -p vocalchat > backup_$(date +%Y%m%d).sql

# 磁盘清理
docker system prune -a --volumes
```

## 9. 附录

### 9.1 接口对照

详见 `api.md`，全部保持不变。

### 9.2 Agent 接口

| 方法     | 路径                             | 说明              |
| ------ | ------------------------------ | --------------- |
| `POST` | `/api/aiAssistant/agentRun`    | Agent 同步执行      |
| `POST` | `/api/aiAssistant/agentStream` | Agent 流式 SSE    |
| `GET`  | `/api/aiAssistant/tools`       | 获取可用工具列表（前端展示用） |

### 9.3 语音 WebSocket 端点

| 端点                                  | 说明   |
| ----------------------------------- | ---- |
| `ws://<host>/ws/speech?token=<jwt>` | 语音通话 |

### 9.4 参考资源

- [LangChain4j 文档](https://docs.langchain4j.dev)
- [LangChain4j Agent 教程](https://docs.langchain4j.dev/tutorials/agents)
- [LangChain4j Tool 教程](https://docs.langchain4j.dev/tutorials/tools)
- [腾讯云实时语音识别](https://cloud.tencent.com/document/product/1093)
- [腾讯云语音合成](https://cloud.tencent.com/document/product/1073)
- [ClawRunr — 纯 Java AI Agent 参考实现](https://github.com/jobrunr/ClawRunr)

### 9.5 弹性扩展（时间充裕时）

> 以下扩展按优先级排列，均源于 ClawRunr 的核心能力。团队提前完成主线计划时可择项推进，每个扩展独立可交付。

---

#### 扩展一：MCP 协议支持（⭐⭐⭐⭐⭐ 强烈推荐）

**是什么：** Model Context Protocol，AI Agent 工具接入的标准协议。一个 MCP Server 就是一个独立的工具进程，Agent 通过标准 JSON-RPC 协议发现和调用工具。

**为什么推荐：** 这是当前 AI Agent 生态最重要的标准。团队体验 MCP 后，会理解"工具不写死在 Agent 里"的意义——任何语言写一个 MCP Server，Agent 接上就能用。

**参考实现：** ClawRunr 内置 MCP Client，支持连接外部 MCP Server。

**VocalChat 实现路径：**

```java
// 接入 LangChain4j 的 MCP 模块
// <dependency>
//     <groupId>dev.langchain4j</groupId>
//     <artifactId>langchain4j-mcp</artifactId>
//     <version>1.13.0</version>
// </dependency>

// 连接一个文件系统 MCP Server（第三方现成）
McpClient fileSystemClient = McpClient.builder()
    .transport(new StdioTransport("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp"))
    .build();

// Agent 自动获得文件读/写/列目录等工具
toolRegistry.registerMcpTools(fileSystemClient);
```

**任务量：** 2-3 人天。LangChain4j 已有现成的 MCP 适配层。

---

#### 扩展二：定时/周期 Agent 任务（⭐⭐⭐⭐）

**是什么：** 用户设定"每周一早上 9 点总结上周对话并发到邮箱"，Agent 按 cron 表达式自动执行。

**参考实现：** ClawRunr 用 JobRunr 实现任务调度（延迟、定时、重试）。

**VocalChat 实现路径：**

```java
// 方案一：轻量 — Spring @Scheduled + 自定义任务表
// 方案二：完整 — 引入 JobRunr（与 ClawRunr 同款）

// 方案一示例
@Entity
@TableName("agent_scheduled_task")
public class ScheduledTask {
    private String id;
    private String userId;
    private String cronExpression;  // "0 9 * * 1" = 每周一 9:00
    private String prompt;          // "总结上周所有对话，发到邮箱"
    private String status;          // ACTIVE / PAUSED
}

@Service
public class ScheduledAgentService {
    @Scheduled(fixedRate = 60000)  // 每分钟检查一次
    public void executePendingTasks() {
        List<ScheduledTask> tasks = taskMapper.findDue();
        for (ScheduledTask task : tasks) {
            agentService.run(task.getPrompt(), task.getUserId(), task.getId());
        }
    }
}
```

**新增接口：**

| 方法       | 路径                                     | 说明            |
| -------- | -------------------------------------- | ------------- |
| `POST`   | `/api/aiAssistant/scheduled-task`      | 创建定时 Agent 任务 |
| `GET`    | `/api/aiAssistant/scheduled-tasks`     | 任务列表          |
| `DELETE` | `/api/aiAssistant/scheduled-task/{id}` | 删除任务          |

**任务量：** 3-4 人天（方案一）/ 5-6 人天（方案二 + JobRunr 集成）。

---

#### 扩展三：动态技能系统（⭐⭐⭐⭐）

**是什么：** 在 `workspace/skills/` 目录放一个 `SKILL.md` 文件，Agent 自动获得新能力，无需重启，无需写 Java 代码。

**参考实现：** ClawRunr 的文件级技能系统——`SKILL.md` 包含系统提示词 + 工具声明，Agent 加载目录即获得能力。

**VocalChat 实现路径：**

```markdown
# workspace/skills/code-review/SKILL.md  ← 新建文件即可
---
name: code-review
description: 检查代码片段中的安全漏洞和性能问题
tools:
  - check_sql_injection
  - check_xss
---

## 角色
你是一个资深代码审查专家。

## 审查规则
1. 检查是否使用了参数化查询
2. 检查是否有未转义的用户输入
3. ...
```

```java
@Service
public class SkillLoader {
    // 监听 workspace/skills/ 目录变化
    // 解析 SKILL.md frontmatter
    // 动态注入 SystemPrompt + 工具到 Agent
    public List<Skill> loadSkills() { ... }
}
```

**任务量：** 3-4 人天（YAML 解析 + 动态注入）。

---

#### 扩展四：Agent 工具调用审批（⭐⭐⭐）

**是什么：** 在执行敏感操作（发邮件、删除数据、修改配置）前，Agent 暂停执行，等待用户在网页上点"批准"或"拒绝"。

**参考实现：** ClawRunr 的 Human-in-the-Loop 机制。

**VocalChat 实现路径：**

```
Agent 决定调用 EmailTool
    → 暂停，SSE 推送: {"event":"approval_request", "tool":"EmailTool", "args":{...}}
    → 前端弹窗: "Agent 想要发送邮件，批准？"
    → 用户点"批准" → 继续执行
    → 用户点"拒绝" → Agent 收到"用户拒绝了该操作"，重新决策
```

```java
@Service
public class AgentService {
    public void runWithApproval(String userMessage, WebSocketSession session) {
        // Agent Loop 中增加拦截点
        for (ToolExecutionRequest req : response.toolExecutionRequests()) {
            if (toolRegistry.isSensitive(req.name())) {
                // 暂停，等待用户确认
                ApprovalRequest approval = new ApprovalRequest(req);
                session.sendMessage(approval.toJson());
                ApprovalResponse resp = approval.await(30000);  // 30s 超时
                if (!resp.isApproved()) {
                    memory.add("用户拒绝了该操作，请尝试其他方案");
                    continue;
                }
            }
            // 执行工具
        }
    }
}
```

**任务量：** 2-3 人天。

---

#### 扩展五：多助手协作对话（⭐⭐⭐）

**是什么：** 两个 AI 助手互相对话——一个扮演提问者，一个扮演解答者。用于头脑风暴、方案辩论。

**参考实现：** ClawRunr 的 A2A（Agent-to-Agent）通信。

**VocalChat 实现路径：**

```java
// 助手 A（角色："质疑者"）← 可以看到助手 B 的回复
// 助手 B（角色："方案提出者"）
// 对话循环：
//   用户提出问题 → B 回答 → A 质疑 → B 修正 → A 判断通过 → 最终方案输出
public String debate(String topic, AIAssistant proposer, AIAssistant critic) {
    // 多轮交替对话
}
```

**新增接口：**

| 方法     | 路径                        | 说明        |
| ------ | ------------------------- | --------- |
| `POST` | `/api/aiAssistant/debate` | 发起助手辩论/协作 |

**任务量：** 3-4 人天。

---

#### 扩展选择建议

| 剩余时间  | 推荐组合                           |
| ----- | ------------------------------ |
| 多 1 周 | MCP 协议支持（扩展一）                  |
| 多 2 周 | MCP + 定时任务（扩展一 + 二）            |
| 多 3 周 | MCP + 定时任务 + 技能系统（扩展一 + 二 + 三） |
| 多 4 周 | 全上                             |

> 优先级依据：MCP 是行业标准，团队必学；定时任务让 Agent 从"被动响应"变成"主动服务"，体验差异最大；技能系统展示可扩展架构；审批和协作是锦上添花。

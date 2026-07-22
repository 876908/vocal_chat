# VocalChat API 文档

---

## 1. 概述

| 项目     | 说明                                                        |
| ------ | --------------------------------------------------------- |
| 基础 URL | `http://<host>:<port>`，REST 接口均位于 `/api/**`               |
| 鉴权方式   | JWT，HTTP 通过 `Token` Header 传递；WebSocket 通过 `token` 查询参数传递 |
| 请求格式   | `application/json`（文件上传为 `multipart/form-data`）           |
| 流式响应   | SSE（`text/event-stream`）                                  |
| 实时通信   | WebSocket `/ws/speech`（语音通话）、`/ws/chat`（文本聊天）             |

### 拦截器链

所有 HTTP 请求经过两层拦截器：

| 顺序  | 拦截器                  | 职责                                        |
| --- | -------------------- | ----------------------------------------- |
| 0   | `RequestInterceptor` | 注入 `TRACE_ID` 到 MDC，记录请求耗时                |
| 1   | `UserInterceptor`    | 验证 `Token` Header 中的 JWT，写入 `UserContext` |

- `@SkipToken` 注解跳过鉴权
- 鉴权失败返回 HTTP `401`

---

## 2. 认证机制

### 2.1 Token 获取

注册或登录成功，响应的 `data` 字段即为 JWT。

### 2.2 Token 使用

所有需鉴权的 HTTP 请求在 Header 中携带：

```
Token: <jwt>
```

WebSocket 握手时通过查询参数传递：

```
ws://<host>:<port>/ws/speech?token=<jwt>
ws://<host>:<port>/ws/chat?token=<jwt>
```

### 2.3 Token 生命周期

- 签发时存入 Redis，key 为 `user:<userId>`，TTL 86400 秒
- 登出时删除 Redis 记录

---

## 3. 通用响应格式

标注 `@AutoResult` 的方法返回值自动包装为：

```json
{
  "code": null,
  "message": null,
  "success": true,
  "data": "<实际载荷>"
}
```

| 字段        | 类型        | 说明                 |
| --------- | --------- | ------------------ |
| `code`    | `Integer` | 成功为 `null`，失败为错误码  |
| `message` | `String`  | 成功为 `null`，失败为错误描述 |
| `success` | `boolean` | `true` / `false`   |
| `data`    | `T`       | 实际载荷               |

> SSE 流式接口不经过 `@AutoResult` 包装，直接返回 `text/event-stream`。

---

### 4. REST API

### 4.1 用户模块 `/api/public/user`

#### `POST /register` — 注册

- **鉴权**：`@SkipToken`
- **响应**：`BaseResult<String>`，data 为 JWT

请求体：

```json
{
  "nickName": "string",
  "password": "string",
  "email": "string",
  "verificationCode": "string"
}
```

---

#### `POST /login` — 登录

- **鉴权**：`@SkipToken`
- **响应**：`BaseResult<String>`，data 为 JWT

请求体：

```json
{
  "email": "string",
  "password": "string"
}
```

---

#### `POST /getVerificationCode` — 发送验证码

- **鉴权**：`@SkipToken`
- **响应**：`BaseResult<null>`

| 参数      | 类型       | 必填  | 说明       |
| ------- | -------- | --- | -------- |
| `email` | `String` | 是   | Query 参数 |

验证码为 6 位随机数字，Redis 缓存 5 分钟。

---

#### `POST /logout` — 登出

- **鉴权**：需要 Token
- **响应**：`BaseResult<null>`

---

#### `GET /info` — 获取当前用户信息

- **鉴权**：需要 Token
- **响应**：`BaseResult<UserInfoVO>`

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "nickName": "string",
    "email": "string"
  }
}
```

---

### 4.2 AI 助手模块 `/api/aiAssistant`

#### `POST /createNewAssistant` — 创建助手

- **鉴权**：需要 Token
- **响应**：`BaseResult<String>`，data 为助手 ID

```json
{
  "name": "string",
  "description": "string",
  "character": "string",
  "knowledgeBaseId": "string // 可选"
}
```

---

#### `POST /modifyAssistantConfig` — 修改助手配置

- **鉴权**：需要 Token
- **响应**：`BaseResult<String>`

| 参数              | 类型       | 必填  |
| --------------- | -------- | --- |
| `aiAssistantId` | `String` | 是   |

请求体同创建接口。

---

#### `DELETE /deleteAssistant` — 删除助手

- **鉴权**：需要 Token
- **响应**：`BaseResult<String>`

| 参数              | 类型       | 必填  |
| --------------- | -------- | --- |
| `aiAssistantId` | `String` | 是   |

---

#### `GET /aiAssistants` — 助手列表

- **鉴权**：需要 Token
- **响应**：`BaseResult<List<AIAssistantVO>>`

```json
{
  "success": true,
  "data": [{
    "id": "uuid",
    "name": "string",
    "description": "string",
    "character": "string",
    "knowledgeBaseId": "string | null"
  }]
}
```

---

#### `GET /{aiAssistantId}/conversation-log` — 会话记录

- **鉴权**：需要 Token
- **响应**：`BaseResult<List<[String, String]>>`

```json
{
  "success": true,
  "data": [
    ["USER", "你好"],
    ["ASSISTANT", "你好！"]
  ]
}
```

---

#### `POST /streamGenerateReply` — 流式生成回复（SSE）

- **鉴权**：需要 Token
- **Content-Type**：请求 `application/json`，响应 `text/event-stream`

```json
{
  "question": "string",
  "aiAssistantId": "string",
  "enableOnlineSearch": false,
  "enableDeepThinking": false
}
```

---

#### `DELETE /{aiAssistantId}/conversation-log` — 重置会话

- **鉴权**：需要 Token
- **响应**：`BaseResult<null>`

---

### 4.3 Agent 模块

#### `POST /agentRun` — Agent 同步执行

- **鉴权**：需要 Token
- **响应**：`BaseResult<AgentResultVO>`

```json
// 请求体
{
  "question": "string",
  "aiAssistantId": "string",
  "enableOnlineSearch": false,
  "enableDeepThinking": false
}

// 响应
{
  "success": true,
  "data": {
    "finalAnswer": "string",
    "success": true,
    "steps": [{
      "type": "thinking | tool_call | tool_result",
      "content": "string",
      "tool": "string",
      "args": {},
      "result": "string",
      "success": true,
      "timestamp": 1234567890
    }]
  }
}
```

---

#### `POST /agentStream` — Agent 流式执行（SSE）

- **鉴权**：需要 Token
- **Content-Type**：请求 `application/json`，响应 `text/event-stream`

请求体同 `agentRun`。SSE 事件包括 `agent_thinking`、`agent_tool_call`、`agent_tool_result`，详见 [第 6 节](#6-sse-流式协议)。

---

#### `GET /tools` — 可用工具列表

- **鉴权**：需要 Token
- **响应**：`BaseResult<List<ToolVO>>`

```json
{
  "success": true,
  "data": [{
    "type": "tool",
    "tool": "set_browser_theme",
    "content": "切换浏览器主题色"
  }, {
    "type": "tool",
    "tool": "search_knowledge_base",
    "content": "搜索知识库文档"
  }]
}
```

#### Agent 工具一览

| 工具名                        | 功能           |
| -------------------------- | ------------ |
| `set_browser_theme`        | 切换浏览器深色/亮色主题 |
| `search_conversation`      | 按关键词搜索历史对话   |
| `save_conversation`        | 保存对话到知识库     |
| `search_knowledge_base`    | 搜索知识库文档      |
| `get_knowledge_base_stats` | 统计知识库信息      |
| `send_email`               | 发送邮件         |
| `modify_assistant_config`  | 修改助手配置       |
| `speak_text`               | TTS 语音播报     |

---

### 4.4 知识库模块 `/api/knowledge-base`

#### `POST /api/knowledge-base` — 创建知识库

- **鉴权**：需要 Token
- **响应**：`BaseResult<null>`

```json
{
  "name": "string",
  "description": "string // 可选，最长 200 字符"
}
```

---

#### `GET /api/knowledge-base` — 知识库列表

- **鉴权**：需要 Token
- **响应**：`BaseResult<List<KnowledgeBaseVO>>`

```json
{
  "success": true,
  "data": [{
    "id": "uuid",
    "name": "string",
    "description": "string | null",
    "status": "ACTIVE",
    "documentCount": 5,
    "chunkCount": 128,
    "createdAt": "2026-07-10T12:00:00",
    "updatedAt": "2026-07-10T12:00:00"
  }]
}
```

---

#### `GET /api/knowledge-base/{id}` — 知识库详情

- **鉴权**：需要 Token
- **响应**：`BaseResult<KnowledgeBaseVO>`

---

#### `PUT /api/knowledge-base/{id}` — 修改知识库

- **鉴权**：需要 Token
- **响应**：`BaseResult<null>`

请求体同创建接口。

---

#### `DELETE /api/knowledge-base/{id}` — 删除知识库

- **鉴权**：需要 Token
- **响应**：`BaseResult<null>`

---

#### 文件管理 `/api/knowledge-base/{id}/file`

##### `POST /{id}/file` — 上传文件

- **鉴权**：需要 Token
- **Content-Type**：`multipart/form-data`
- **响应**：`BaseResult<KnowledgeBaseFileVO>`

| 字段     | 类型     | 说明                       |
| ------ | ------ | ------------------------ |
| `file` | `File` | 支持 PDF / TXT / MD / DOCX |

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "knowledgeBaseId": "uuid",
    "fileName": "doc.pdf",
    "fileType": "pdf",
    "fileSize": 102400,
    "status": "COMPLETED",
    "chunkCount": 0,
    "createdAt": "2026-07-10T12:00:00",
    "updatedAt": "2026-07-10T12:00:00"
  }
}
```

`status`：`UPLOADING` → `UPLOADED` / `COMPLETED` / `FAILED`

##### `GET /{id}/file` — 文件列表

- **鉴权**：需要 Token
- **响应**：`BaseResult<List<KnowledgeBaseFileVO>>`

##### `DELETE /{id}/file/{fileId}` — 删除文件

- **鉴权**：需要 Token
- **响应**：`BaseResult<null>`

##### `GET /{id}/file/{fileId}/status` — 文件状态

- **鉴权**：需要 Token
- **响应**：`BaseResult<KnowledgeBaseFileVO>`

---

## 6. SSE 流式协议

### 6.1 端点

```
POST /api/aiAssistant/streamGenerateReply
POST /api/aiAssistant/agentStream
```

### 6.2 标准对话事件（`streamGenerateReply`）

| 事件          | Data         | 说明                                |
| ----------- | ------------ | --------------------------------- |
| `started`   | `"invoked"`  | 请求已接收                             |
| `heartbeat` | `"ping"`     | 每 15 秒心跳                          |
| `thinking`  | 文本           | 深度思考（`enableDeepThinking=true` 时） |
| `token`     | 文本           | LLM 流式输出                          |
| `fallback`  | `"true"`     | 主模型失败，切换备用                        |
| `done`      | `"complete"` | 回复完成并持久化                          |
| `error`     | 异常信息         | 异常（`"timeout"` = 30 分钟超时）         |

### 6.3 Agent 扩展事件（`agentStream`）

| 事件                  | Data                               | 说明           |
| ------------------- | ---------------------------------- | ------------ |
| `agent_thinking`    | `"思考内容"`                           | Agent 决策推理   |
| `agent_tool_call`   | `{"tool":"...","args":{...}}`      | 调用工具（JSON）   |
| `agent_tool_result` | `{"summary":"...","success":true}` | 工具执行结果（JSON） |
| `token`             | 文本                                 | 最终回复全文（一次性）  |
| `done`              | `"complete"`                       | Agent 执行完成   |

> `streamGenerateReply` 也可以选择性地发送 `agent_*` 事件（当后端决定启用 Agent 时）。

### 6.4 Agent 事件流示例

```
event:started
data:invoked

event:agent_thinking
data:用户想搜索知识库中的 Spring 文档，先搜索...

event:agent_tool_call
data:{"tool":"search_knowledge_base","args":{"keyword":"Spring"}}

event:agent_tool_result
data:{"tool":"search_knowledge_base","success":true,"summary":"找到3篇文档"}

event:token
data:根据知识库中的三篇文档，Spring Boot 核心要点为...

event:done
data:complete
```

### 6.5 连接参数

| 参数  | 值     |
| --- | ----- |
| 超时  | 30 分钟 |
| 心跳  | 15 秒  |

---

## 7. WebSocket — 语音聊天

### 7.1 端点

```
ws://<host>:<port>/ws/speech?token=<jwt>
```

### 7.2 消息格式

**客户端 → 服务端（Command）**

```json
{
  "command": "<command_name>",
  "seq": 1,
  "payload": { }
}
```

**服务端 → 客户端（Event）**

```json
{
  "event": "<event_name>",
  "seq": 1,
  "payload": { }
}
```

### 7.3 二进制帧（音频）

每帧前 1 字节为类型标记，后续为音频数据：

| 偏移  | 大小      | 说明                             |
| --- | ------- | ------------------------------ |
| 0   | 1 byte  | `0x01` = 麦克风输入，`0x02` = TTS 输出 |
| 1   | N bytes | PCM Int16 音频载荷                 |

| 参数  | 上行（Mic）  | 下行（TTS）  |
| --- | -------- | -------- |
| 编码  | PCM      | PCM      |
| 采样率 | 16000 Hz | 16000 Hz |
| 位深  | 16 bit   | 16 bit   |
| 声道  | mono     | mono     |

### 7.4 Command 注册表

| command               | 说明       | payload                                              |
| --------------------- | -------- | ---------------------------------------------------- |
| `voice.session.join`  | 加入语音通道   | `{ aiAssistantId, tts: { speaker, speed, volume } }` |
| `voice.session.leave` | 离开语音通道   | `{}`                                                 |
| `voice.interrupt`     | 打断当前 TTS | `{}`                                                 |
| `voice.mute`          | 静音麦克风    | `{}`                                                 |
| `voice.unmute`        | 取消静音     | `{}`                                                 |

### 7.5 Event 注册表

#### 会话生命周期

| event                   | 说明     | payload                 |
| ----------------------- | ------ | ----------------------- |
| `voice.session.started` | 语音通道建立 | `{ sessionId }`         |
| `voice.session.ended`   | 语音通道关闭 | `{ sessionId, reason }` |

#### ASR 识别

| event               | 说明     | payload                         |
| ------------------- | ------ | ------------------------------- |
| `voice.asr.interim` | 中间识别结果 | `{ text, timestamp }`           |
| `voice.asr.final`   | 最终识别结果 | `{ text, timestamp, duration }` |

#### LLM 回复

| event                | 说明         | payload                            |
| -------------------- | ---------- | ---------------------------------- |
| `voice.llm.token`    | 流式 token   | `{ token, index }`                 |
| `voice.llm.thinking` | 深度思考 token | `{ token, index }`                 |
| `voice.llm.complete` | LLM 回复完成   | `{ fullText, chatId, dialogueId }` |

#### TTS 播放

| event                   | 说明    | payload              |
| ----------------------- | ----- | -------------------- |
| `voice.tts.started`     | 开始播放  | `{ chatId }`         |
| `voice.tts.ended`       | 播放完毕  | `{ chatId }`         |
| `voice.tts.interrupted` | 播放被打断 | `{ chatId, reason }` |

#### Pipeline 状态

| event                  | 说明   | payload     |
| ---------------------- | ---- | ----------- |
| `voice.pipeline.state` | 状态变更 | `{ state }` |

`state` 取值：`IDLE` → `LISTENING` → `THINKING` → `SPEAKING`

#### Agent 事件

| event               | 说明       | payload                |
| ------------------- | -------- | ---------------------- |
| `agent_thinking`    | Agent 推理 | `{ text }`             |
| `agent_tool_call`   | 工具调用     | `{ tool, args }`       |
| `agent_tool_result` | 工具结果     | `{ summary, success }` |
| `agent_complete`    | Agent 完成 | `{}`                   |

#### 响应事件（工具触发的 UI 变更）

| event            | 说明   | payload                          |
| ---------------- | ---- | -------------------------------- |
| `reactive_event` | 工具触发 | `{ event: "browser_color_change" |

#### 错误

| event         | 说明  | payload                          |
| ------------- | --- | -------------------------------- |
| `voice.error` | 错误  | `{ code, message, recoverable }` |

### 7.6 Pipeline 状态机

```
                  voice.session.join
                         │
                  ┌──────▼──────┐
                  │    IDLE     │◄──────────────────────────┐
                  └──────┬──────┘                           │
                         │ VAD 检测到语音                     │
                  ┌──────▼──────┐                           │
            ┌────►│  LISTENING  │                           │
            │     └──────┬──────┘                           │
            │            │ VAD 检测静默                       │
            │     ┌──────▼──────┐    LLM 完成                │
            │     │  THINKING   │──────────┐                │
            │     └──────┬──────┘          │                │
            │            │ TTS 开始       ┌▼──────────┐     │
            │            └──────────────►│  SPEAKING  │     │
            │                           └─────┬──────┘     │
            │                 播放完成 / 打断    │           │
            └──────────────────────────────────┘───────────┘
```

### 7.7 典型交互流程

```
Client                                     Server

    │-- voice.session.join ──────────────►│
    │   { aiAssistantId, tts }             │
    │◄── voice.session.started ───────────│
    │◄── voice.pipeline.state: IDLE ──────│
    │                                      │
    │-- Binary [0x01 PCM] ───────────────►│  (用户说话)
    │◄── voice.pipeline.state: LISTENING ─│
    │◄── voice.asr.interim ───────────────│  "今天"
    │◄── voice.asr.final ────────────────│  "今天天气怎么样"
    │◄── voice.pipeline.state: THINKING ──│
    │◄── voice.llm.token ────────────────│  "今天天气"
    │◄── voice.llm.complete ─────────────│
    │◄── voice.pipeline.state: SPEAKING ─│
    │◄── voice.tts.started ──────────────│
    │◄── Binary [0x02 PCM] ──────────────│  (TTS 音频)
    │◄── voice.tts.ended ────────────────│
    │◄── voice.pipeline.state: IDLE ─────│
    │                                      │
    │-- voice.session.leave ────────────►│
    │◄── voice.session.ended ────────────│
```

---

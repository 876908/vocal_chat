# VocalChat 设计指南

---

## 1. 项目定位

VocalChat 是一个 AI Agent 语音助手 Web 应用，核心能力：

- 文字/语音与 AI 对话（流式 SSE）
- 多 AI 助手管理（不同性格、不同知识库）
- AI Agent 自主调用工具链完成任务
- 实时语音通话（ASR + LLM + TTS 全链路）

架构原则：Controller-Service-Mapper 三层，LangChain4j AI Services + Agentic 模块实现 Agent。

---

## 2. 架构

```
前端 (Vue 3)  ←→  REST / SSE / WebSocket  ←→  Controller 层
                                                   ↓
                                              Service 层（业务逻辑 + Agent 编排 + 工具注册）
                                                   ↓
                                              Mapper 层（MyBatis-Plus CRUD）
                                                   ↓
                                              MySQL / Redis / COS
```

### 各层职责

| 层              | 职责                      | 禁止       |
| -------------- | ----------------------- | -------- |
| Controller     | 接收请求、参数校验、调用 Service    | 不写业务逻辑   |
| Service        | 业务逻辑、Agent 编排、事务管理、工具注册 | 不直接操作数据库 |
| Mapper         | MyBatis-Plus CRUD       | 不写业务逻辑   |
| Infrastructure | 配置、拦截器、外部服务适配、横切工具      | 不侵入业务    |

### 关键设计决策

- **实体直接映射数据库**：字段直接对应表列，不包装 Value Object
- **Service 内聚**：不拆分"领域服务"和"应用服务"
- **Spring 事件解耦**：跨模块通知用事件（如回答完成 → TTS 合成）
- **统一响应格式**：`@AutoResult` 注解包装 `BaseResult<T>`
- **统一鉴权**：JWT + `UserInterceptor` + ThreadLocal `UserContext`

## 3. 包结构

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

## 技术栈与成本

### 选型

| 层        | 技术                 | 选型理由                           |
| -------- | ------------------ | ------------------------------ |
| 语言       | Java 25            | 虚拟线程，无需额外线程池                   |
| 框架       | Spring Boot 3.5.4  | 生态成熟                           |
| ORM      | MyBatis-Plus 3.5.7 | 轻量，无外键约束                       |
| LLM 框架   | LangChain4j 1.13.0 | AI Services模块                  |
| LLM      | 通义千问 DashScope     | 流式对话 + Function Calling，国内访问稳定 |
| 邮件       | Resend             | 免费 100 封/天，生产级送达率              |
| ASR      | 腾讯云实时语音识别          | 免费 5 小时/月，WebSocket 流式接入       |
| TTS      | 腾讯云语音合成            | 免费 100 万字符/月，流式合成              |
| 数据库      | MySQL 8.0          | Docker 自部署                     |
| 缓存       | Redis 7            | JWT 存储 + 验证码缓存                 |
| 对象存储（本地） | MinIO              | Docker 自部署，体验部署流程              |
| 容器化      | Docker Compose     | 全栈一键部署                         |

## 4. 数据库设计

### 核心表

| 表                     | 关键字段                                                                                  | 说明                                       |
| --------------------- | ------------------------------------------------------------------------------------- | ---------------------------------------- |
| `user`                | id, name, email, password                                                             | BCrypt 加密                                |
| `ai_assistant`        | id, user_id, name, description, assistant_character, knowledge_base_id                | 助手配置，System Prompt 存 assistant_character |
| `dialogue`            | id, ai_assistant_id, contexts(JSON)                                                   | 对话上下文，JSON 数组存储                          |
| `knowledge_base`      | id, user_id, name, description, status, document_count, chunk_count                   | 知识库元数据                                   |
| `knowledge_base_file` | id, knowledge_base_id, file_name, storage_key, status, chunk_count                    | 上传文件                                     |
| `voice_call_session`  | id, user_id, ai_assistant_id, status, duration_seconds                                | 语音通话记录                                   |
| `agent_tool_call_log` | id, user_id, session_id, tool_name, input_params, output_result, duration_ms, success | Agent 工具调用审计                             |

### 设计要点

- 无外键约束（MyBatis-Plus 惯例）
- UUID 主键
- JSON 列存对话上下文（dialogue.contexts）
- 知识库文件状态机：UPLOADING → UPLOADED → PROCESSING → COMPLETED / FAILED

---

## 5. 功能模块设计思路

### 5.1 用户与认证

**接口：** 注册、登录、登出、用户信息、验证码发送。

**认证链路：**

1. 所有请求经过 `UserInterceptor`（`/**` 路径）
2. 从 HTTP `Token` Header 或 WebSocket `?token=` 提取 JWT
3. 验证 JWT 有效性，解析 userId
4. 从 Redis 校验 Token 是否在有效会话列表中
5. 写入 `UserContext`（ThreadLocal），后续 Service 通过 `UserContext.require()` 获取

**设计要点：**

- `@SkipToken` 注解跳过鉴权（注册/登录/验证码接口）
- 密码 BCrypt 加密，永不存明文
- 验证码 6 位随机数字，Redis 5 分钟 TTL，异步邮件发送
- 登出时清除 Redis 中用户的 Token 记录

### 5.2 AI 助手与对话

**接口：** 助手 CRUD、对话记录获取/追加/重置、SSE 流式对话。

**对话流：**

1. 前端 POST 问题 + 助手 ID + 是否联网搜索 + 是否深度思考
2. 后端构建 `ChatMemory`：SystemMessage（助手角色设定）+ 历史对话消息
3. 选择 Qwen Model Bean（普通 / 联网搜索 / 深度思考）
4. 调用 `QwenStreamingChatModel.chat(messages, handler)`
5. handler 回调 `onPartialResponse`（token）→ SSE 推给前端
6. handler 回调 `onCompleteResponse`（完整回复）→ 持久化到 dialogue 表

**SSE 事件设计：**

- `started`：请求已接收
- `heartbeat`：每 15 秒保活
- `thinking`：深度思考中间过程（可选）
- `token`：流式 token
- `done`：完成并持久化
- `error`：异常

**设计要点：**

- 超时 30 分钟，心跳 15 秒
- 创建助手时自动生成空对话
- 对话上下文存在 dialogue.contexts JSON 列中

### 5.3 知识库管理

**接口：** 知识库 CRUD、文件上传/列表/状态/删除。

**文件处理链路：**

1. 前端 multipart 上传文件
2. 后端校验文件类型（PDF/TXT/MD/DOCX）
3. 上传到对象存储（本地 MinIO / 生产 COS）
4. 入库记录（状态：UPLOADING → UPLOADED/COMPLETED）
5. 后续对接文档切片服务（Easy RAG）

**设计要点：**

- 删除知识库时级联删除所有文件
- 文件状态可轮询查询

### 5.4 Agent 工具调用

**设计目标：** 用户用自然语言描述需求，LLM 自动从工具列表中选择合适的工具，按需链式调用。

**入口：**

- `POST /api/aiAssistant/agentStream` — Agent 流式 SSE

**核心思路：** LLM 不是"被调用一次就回答"，而是"每次回答可能是文本也可能是工具调用请求"——如果是工具调用，框架执行工具后把结果回传给 LLM，LLM 再决定下一步。循环直到 LLM 输出最终文本回复。

**SSE 扩展事件：**

- `agent_thinking`：Agent 内部思考
- `agent_tool_call`：开始调用工具（工具名 + 参数）
- `agent_tool_result`：工具返回结果

**设计要点：**

- Agent Loop 上限 10 轮，防止死循环
- `@Tool` 的 description 决定了 LLM 是否能正确判断何时调用——这是工具设计最重要的环节
- 工具注册表自动扫描 `@Component` 中带 `@Tool` 注解的方法，无需手动注册
- 如果 LLM 不支持 Function Calling 或工具执行失败，Agent 应优雅降级为普通对话

### 5.5 ASR 语音识别

**链路：** 浏览器麦克风 → WebSocket 二进制帧（PCM 16kHz 16bit mono）→ 后端 → 腾讯云 ASR WebSocket → 识别结果回传

**设计要点：**

- 每 40ms 一块音频帧推流
- 中间结果实时推给前端（用于 UI 显示"正在识别..."）
- 最终结果触发后续 LLM/Agent 流程
- ASR 连接需要管理生命周期：开始识别、结束识别、异常断开

### 5.6 TTS 语音合成

**链路：** LLM 回复文本 → TTS 服务流式合成 → WebSocket 二进制帧（PCM）→ 浏览器播放

**设计要点：**

- 流式合成：文本分句 → 逐句合成 → 逐块推送音频帧
- 合成参数可配置：音色、语速、音量、采样率
- 与 ASR 共享 WebSocket 连接（`/ws/speech`），二进制帧和 JSON 控制消息混合传输

---

## 6. Agent 核心设计

### 6.1 整体思路

Agent 的本质是：**让 LLM 在"回复文本"和"调用工具"之间自主决策，多轮循环直到完成任务。**

与普通对话的区别：

|        | 普通对话        | Agent              |
| ------ | ----------- | ------------------ |
| LLM 响应 | 始终返回文本      | 可能返回文本，也可能返回工具调用请求 |
| 执行轮次   | 1 轮         | 不确定多轮              |
| 终止条件   | LLM 回复即结束   | LLM 判断"任务完成"才结束    |
| 上下文    | 用户消息 + 历史对话 | 再加上工具调用和结果的完整链路    |
| 工具     | 无           | 由 LLM 按需选择和组合      |

### 6.2 工具注册机制

**设计思路：**

1. 工具类标注 `@Component`，方法标注 `@Tool(name="xxx", value="工具描述")`，参数标注 `@P("参数含义")`
2. Spring 启动时自动发现所有带 `@Tool` 的 Bean
3. 提取方法签名 + 注解信息，生成 LangChain4j 的 `ToolSpecification`
4. LLM 调用时根据 `ToolSpecification` 理解可用工具及其参数
5. LLM 返回 `ToolExecutionRequest` → `ToolRegistry` 反射调用对应方法 → 结果回传 LLM

**设计要点：**

- `@Tool` 的 `value`（工具描述）是 LLM 判断何时使用的唯一依据，必须清晰、具体
- `@P`（参数描述）同样关键——描述模糊会导致 LLM 传错参数
- 工具返回值应为 LLM 可理解的文本摘要，不需要返回完整对象
- 工具内部异常应捕获并返回错误描述，不要上抛

### 6.3 Agent Loop 控制流

**核心循环：**

1. 构建 `ChatMemory`：System Prompt（列出所有可用工具 + 使用规则）+ 对话历史
2. 调用 `ChatModel.chat(messages, toolSpecifications)`
3. 判断 LLM 响应：
   - 如果是 `ToolExecutionRequest` → 执行工具 → 将结果加入 memory → 回到步骤 2
   - 如果是普通文本 → 循环结束，返回最终回复
4. 如果循环超过 `maxIterations`（默认 10）→ 强制终止

**设计要点：**

- System Prompt 中明确告知 LLM："你可以调用工具来完成任务，一次最多调用 N 个工具，完成后必须回复用户"
- `maxIterations` 是安全阀——某些 LLM 可能陷入重复调用同一个工具的死循环
- 每次工具调用的结果需要格式化为 LLM 可理解的文本后再加入 memory
- 工具执行耗时可在 `agent_tool_call_log` 表中记录，用于后续调优

### 6.4 SSE 事件扩展

在现有 SSE 流式事件体系上增加三个 Agent 专用事件：

| 事件                  | 触发时机            | 前端效果               |
| ------------------- | --------------- | ------------------ |
| `agent_thinking`    | Agent 收到消息，开始思考 | 展示"思考中..."         |
| `agent_tool_call`   | Agent 决定调用工具    | 展示"🔧 正在调用 XXX 工具" |
| `agent_tool_result` | 工具执行完成          | 展示工具返回摘要，继续等待下一步   |

这三个事件穿插在 `token` 事件流中，形成完整的 Agent 执行链路可视化。

---

## 7. 语音管线设计

**整体链路：**

```
麦克风 → PCM 音频帧 → WebSocket → TencentAsrService → 腾讯云 ASR → 识别文本
    ↓
VoiceChatService 编排：
    ├─ 判断是否 Agent 请求
    │   ├─ 是 → AgentService.run() → 工具链调用
    │   └─ 否 → QwenChatService → 流式 token
    ↓
回复文本 → TencentTtsService → PCM 音频帧 → WebSocket → 浏览器播放
```

**WebSocket 消息协议（`/ws/speech`）：**

- 客户端 → 服务端：二进制帧（音频数据）+ JSON 文本帧（config / hangup 控制消息）
- 服务端 → 客户端：二进制帧（TTS 音频）+ JSON 文本帧（asr_interim / asr_final / llm_token / agent_thinking / agent_tool_call / agent_tool_result / hangup）

**设计要点：**

- ASR 和 TTS 共享同一个 WebSocket 连接，通过消息类型区分
- 挂断时需要清理 ASR 连接、TTS 缓冲、WebSocket Session
- 语音延迟敏感：ASR 中间结果 < 200ms，TTS 首帧 < 1s
- 通话结束后写入 `voice_call_session` 表记录

---

## 8. 阶段开发计划

总周期 6 周，5 人全后端。

```
Week 1      Week 2      Week 3      Week 4      Week 5      Week 6
│◄ 阶段一 ──►│◄──────────── 阶段二 ──────────────────►│◄── 阶段三 ──►│◄─ 阶段四 ─┤
│   1 周     │                2.5 周                  │    1.5 周    │   1 周     │
│  基础建设   │           Agent 核心 + 工具            │   语音管线    │  集成上线  │
```

### 阶段一：基础建设（Week 1）

**目标：** 每人完成一个端到端模块，统一技术基线。

| A | 项目脚手架 + 共享层（JWT、BaseResult、ErrorEnum、拦截器、Docker Compose） |
| B | 用户模块（注册/登录/登出/信息、Resend 邮件） |
| C | AI 助手 + 对话（CRUD、SSE 流式对话、对话管理） |
| D | 知识库（CRUD、文件上传 MinIO/COS） |
| E | LLM 集成 + 对象存储（Qwen 配置、LangChain4j 集成、MinIO/COS 适配器） |

### 阶段二：Agent 核心 + 工具（Week 2 - Week 4 前半）

**目标：** Agent 框架上线，每个人至少实现 2 个工具。

**Agent 框架共建（前 3 天，A 主导，全员设计讨论）：**

- `ToolRegistry`：工具注册表，自动扫描 `@Tool` 注解、生成 `ToolSpecification`、执行调度
- `AgentService`：Agent Loop 核心，LLM 决策 → 工具执行 → 结果反馈 → 循环
- Agent SSE 事件协议：agent_thinking / agent_tool_call / agent_tool_result

**工具分配：**

|     | 工具 1                    | 工具 2                     | Agent 核心环节      |
| --- | ----------------------- | ------------------------ | --------------- |
| A   | （框架设计，不分配）              |                          | Agent Loop 设计   |
| B   | AssistantConfigTool     | ConversationSearchTool   | pair Agent Loop |
| C   | KnowledgeBaseSearchTool | KnowledgeBaseSummaryTool | ASR 集成          |
| D   | ConversationSaveTool    | EmailTool                | TTS 集成          |
| E   | ThemeTool               | TtsTool                  | Agent SSE 协议    |

### 阶段三：语音管线（Week 4 后半 - Week 5）

**目标：** ASR → Agent → TTS 全链路。

| A + C | 腾讯云 ASR 集成：实时语音识别 WebSocket 客户端 |
| B + D | 腾讯云 TTS 集成：流式语音合成 |
| E | VoiceChatWebSocketHandler + VoiceChatService 全链路编排 |

### 阶段四：集成上线（Week 6）

| A + D | 生产部署：COS 切生产、Docker Compose 编排、HTTPS |
| B + C | 全链路测试：Agent 多工具链、语音通话压力测试 |
| E | API 文档 + 集成测试用例 |
| 全员 | Bug 修复、日志检查、文档更新 |

### 会议节奏

- 周一上午 30-45min：周对齐会，每人 5min 汇报 + 技术负责人同步目标
- 每阶段结束 1h：阶段 Review，Demo + Code Review 回顾 + 下阶段分派
- 每日异步：微信群一条"今天做了什么、明天计划、有无求助"

---

## 9. 弹性扩展思路

时间充裕时按优先级挑选，每个扩展独立可交付。

### 扩展一：MCP 协议支持（最推荐）

**思路：** 接入 Model Context Protocol——AI Agent 工具接入的标准协议。MCP Server 是独立进程，Agent 通过 JSON-RPC 发现和调用工具。LangChain4j 已有 MCP 适配层。

**价值：** 理解"工具不写死在 Agent 里"的行业标准做法。任何语言写一个 MCP Server，Agent 接上就能用。

**任务量：** 2-3 人天。

### 扩展二：定时/周期 Agent 任务

**思路：** 用户设定 cron 表达式（如"每周一 9:00"），Agent 按时自动执行预设 prompt。可以用 Spring `@Scheduled` + 自定义任务表（轻量方案），或引入 JobRunr（完整方案）。

**价值：** Agent 从"被动响应"升级为"主动服务"，体验差异最大。

**任务量：** 3-4 人天（轻量方案）。

### 扩展三：动态技能系统

**思路：** 在 `workspace/skills/` 目录放一个 `SKILL.md` 文件（YAML frontmatter + 提示词），Agent 自动获得新能力，无需重启。文件监听器扫描目录 → 解析 Markdown → 动态注入 System Prompt 和工具。

**价值：** 展示可扩展架构，技能即文件。

**任务量：** 3-4 人天。

### 扩展四：Agent 工具调用审批

**思路：** 敏感工具（发邮件、删除数据、修改配置）执行前，Agent 暂停，SSE 推送审批请求到前端，用户确认后继续。在 Agent Loop 中增加拦截点，敏感工具调用时 `await` 用户响应。

**价值：** 理解 Human-in-the-Loop 模式。

**任务量：** 2-3 人天。

### 扩展五：多助手协作对话

**思路：** 两个 AI 助手交替对话——一个提问一个回答，用于方案辩论。本质是多轮交替调用两个不同 System Prompt 的 LLM。

**价值：** 体验 Agent-to-Agent 通信。

**任务量：** 3-4 人天。

### 选择建议

| 剩余时间 | 组合                |
| ---- | ----------------- |
| +1 周 | MCP               |
| +2 周 | MCP + 定时任务        |
| +3 周 | MCP + 定时任务 + 技能系统 |
| +4 周 | 全部                |

---

## 附录：参考资源

- LangChain4j 文档、Agent 教程、Tool 教程
- 腾讯云实时语音识别 / 语音合成文档
- ClawRunr — 纯 Java AI Agent 参考实现

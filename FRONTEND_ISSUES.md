## 前端联调发现的问题

前端为打包后的产物（`vocalchat-frontend`），无源码。

### 问题 1：助手卡片点击无响应
- **现象**：首页助手卡片有三个按钮（切换音色、设置、删除），但点击卡片主体无反应，无法进入对话界面
- **原因**：前端 JS 中卡片主体未绑定 `Re(id)` 点击事件（对话入口），或 Matter.js 物理画布拦截了点击
- **预期行为**：点击助手卡片 → 调用 `setSelectedId(assistantId)` → 显示拨号/对话界面

### 问题 2：知识库页面上传文件 URL 错误
- **现象**：`POST /api/knowledge-base/[object%20Object]/file` → 400
- **原因**：前端 JS 将 JavaScript 对象拼入 URL，而非字符串 ID（`[object Object]` 是对象的 toString() 结果）

### 问题 3：后端缺少部分 API
- 前端调用了 `/api/knowledge-base`、`/ws/speech` 等接口，后端尚未实现

### 验证
- 后端 AI 助手 + SSE 流式对话功能通过 `test-chat.html` 验证正常
- 测试账号：`test@vocalchat.com` / `Test123456`

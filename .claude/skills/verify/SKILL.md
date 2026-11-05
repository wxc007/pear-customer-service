---
name: verify
description: 使用真实 Spring 应用、MySQL 和本机 OpenAI 兼容替身验证 REST/SSE 外部行为，不调用真实模型。
---

# 运行时验证

不要以重新运行测试代替运行时观察。使用 JDK 17，启动 MySQL、模型替身和真实应用，再驱动 HTTP 接口。

## 启动

1. `docker compose up -d mysql qdrant`。
2. 后台运行 `python -u .claude/skills/verify/fake_openai.py`，只监听 `127.0.0.1:18081`。
3. 使用演示配置启动应用，注入以下非机密验证值：`AI_API_KEY=verification-only`、`AI_BASE_URL=http://127.0.0.1:18081`、`AI_MODEL=deepseek-chat`、`SPRING_AI_MODEL_EMBEDDING=openai`、`AI_EMBEDDING_MODEL=verification-embedding`。命令为 `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo -Dspring-boot.run.arguments=--app.agent.model-timeout=500ms`。
4. 等待 `GET http://localhost:8080/` 返回 200。首次模型调用可能需要预热；若冷启动触发超时，观察到诚实错误后重试正常场景。

完成后停止应用和模型替身后台任务。MySQL 可保留给本地开发。

## 驱动

- `POST /api/v1/conversations/guest` 创建会话。原始 `guestToken` 只保存在当前 shell 变量中，不写入文件、不输出到日志；后续请求使用 `X-Guest-Token`。
- 发送 `hello`：预期两个 `delta` 后一个 `complete`，历史包含 COMPLETED 用户与助手消息。
- 发送 `price`：替身请求 `getSku(1)`，预期 SSE 包含实时 `price=39.90` 与 `onSale=true`。
- 发送 `fault` 或 `timeout`：预期 `error`，历史中的助手消息为 FAILED。
- 使用 UTF-8 JSON 发送“鲜梨售后怎么处理”：预期答案末尾包含《鲜梨品质售后政策》v2026.1；发送“鲜梨如何储存”应引用储存指南。
- 发送“鲜梨储存能用微波炉处理吗”：可控嵌入与所有资料均低相关，预期直接返回资料不足，不调用聊天模型。
- 使用错误 token：预期 401 ProblemDetail。发送 `{}`：预期 400，且不产生新消息。
- 连续发送 11 次普通消息，再发送 `context`：预期第 12 次回答 `contextMessages=21;hasSummary=True`，历史共 24 条。
- 对 `disconnect` 请求使用短客户端超时主动断开，稍后读取历史：助手消息不能停留在 STREAMING。

Windows 终端直接发送中文 JSON 可能出现编码问题；验证场景使用上述 ASCII 关键字，页面与 SSE 响应仍应显示正确中文。

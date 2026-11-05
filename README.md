# 梨产品智能客服

基于 JDK 17、Spring Boot、Spring AI 和 MySQL 的梨产品智能客服项目。当前完成：

- ticket 01：商品分类、SPU、SKU 的公开目录查询与联调页面。
- ticket 02：24 小时游客会话、SSE 流式回答、消息持久化、有限上下文与白名单商品工具。
- ticket 03：已发布政策、储存和食用资料的 Qdrant 检索、版本来源、低相关拒答与故障降级。

认证、交易、知识库和人工接管由后续 ticket 实现。

## 本地启动

前置条件：JDK 17、Docker Desktop。

1. 启动本地 MySQL 8.4 和 Qdrant：

   ```bash
   docker compose up -d mysql qdrant
   ```

2. 在本机环境变量中设置 `AI_API_KEY`。默认使用 DeepSeek OpenAI 兼容接口，可通过 `AI_BASE_URL` 和 `AI_MODEL` 覆盖地址与模型。密钥不要写入项目配置或发送到对话中。

3. 使用演示配置启动应用：

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
   ```

   Windows CMD 可使用：

   ```bat
   mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
   ```

4. 打开 `http://localhost:8080`。页面会创建游客会话，可以连续发送消息、查看 SSE 增量与失败状态，也可以浏览 SPU 和 SKU。新建会话会获得独立凭证；刷新当前标签页会从历史接口恢复消息。

Compose 与 `demo` profile 中的数据库账号仅用于本机演示。非演示环境需要自行提供 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和 `AI_API_KEY`，不要把真实凭据写入项目文件。

模型流默认 30 秒无增量响应后超时，可通过 `app.agent.model-timeout` 调整。每个环境只配置一个模型提供方，不自动跨供应商降级。

知识问答还需要同一提供方支持嵌入模型。默认 `spring.ai.model.embedding=none`，此时应用可以启动，但知识问题会明确降级。需要启用时设置 `SPRING_AI_MODEL_EMBEDDING=openai`，并通过 `AI_EMBEDDING_MODEL` 配置该提供方实际支持的嵌入模型；不要用不受支持的嵌入地址反复请求。Qdrant 默认连接本机 `localhost:6334`，可通过 `QDRANT_HOST`、`QDRANT_PORT` 调整。

## 当前 API

### 商品目录

- `GET /api/v1/catalog/categories`：查询商品分类。
- `GET /api/v1/catalog/spus?categoryId=1`：查询全部或指定分类下的 SPU，`categoryId` 可省略。
- `GET /api/v1/catalog/spus/{spuId}`：查询 SPU 及其全部 SKU。
- `GET /api/v1/catalog/skus/{skuId}`：查询 SKU 实时价格、差异属性和组合可售状态。

SKU 只有自身和所属 SPU 均为 `ON_SALE` 时，`onSale` 才为 `true`。

### 游客会话

- `POST /api/v1/conversations/guest`：创建匿名会话并返回原始 `guestToken`。原始凭证仅出现在该响应中，数据库只保存 SHA-256 摘要，有效期 24 小时。
- `GET /api/v1/conversations/{id}/messages`：通过请求头 `X-Guest-Token` 查询本人会话的完整消息历史。
- `POST /api/v1/conversations/{id}/messages/stream`：通过同一请求头发送 JSON `{"content":"..."}`，响应为 UTF-8 SSE。

SSE 事件均携带 `conversationId`、`messageId` 和 `content`：

- `delta`：文本增量；对应回复消息已先以 `STREAMING` 状态持久化。
- `complete`：完整回复；发送前已将消息更新为 `COMPLETED`。
- `error`：可理解的失败结果；发送前已将消息更新为 `FAILED`，不会伪造商品事实或业务成功。

无效、跨会话或过期游客凭证返回 401 ProblemDetail。单条消息最多 4000 个字符。全量消息入库，模型只接收最近 20 条消息与较早内容的滚动摘要。价格、规格和可售状态只能由 `listSpus`、`getSku` 白名单工具实时读取。

### 知识问答

- MySQL 保存文档、版本、原文、分段、发布状态与索引状态；Qdrant 只保存可重建向量索引。
- 政策、储存和食用问题默认检索最相关的 5 个片段，`app.knowledge.similarity-threshold` 控制相似度阈值。
- 只有 `PUBLISHED` 且 `INDEXED` 的版本参与回答。服务端在完整回复中附加《文档标题》和版本号，并随消息持久化。
- 检索不足时明确不确定，Qdrant 或嵌入模型不可用时返回知识暂不可靠的错误事件。
- 价格、库存和订单不进入静态知识检索链路，知识资料中的恶意指令不会增加工具权限。

## 演示数据

仅 `demo` profile 在空数据库中初始化演示数据，商品响应中的 `demoData=true` 用于明确标识。非演示环境不会自动插入这些分类、SPU 或 SKU。

演示数据包括鲜梨、梨膏、下架 SPU、下架 SKU，以及所属 SPU 已下架但自身可售的 SKU，便于验证组合状态。演示知识包括鲜梨品质售后、储存与食用、梨制品说明，均为明确标记的 `2026.1` 演示版本。

## 测试

启动 Docker Desktop 后使用 JDK 17 运行：

```bash
./mvnw test
```

测试通过 Testcontainers 启动真实 MySQL 8.4 和 Qdrant，从 `/api/v1` REST 与 SSE 接缝验证公开商品、游客归属、流式事件、历史恢复、有限上下文、真实商品工具、知识检索、topK 与阈值、版本来源、低相关拒答、提示词注入隔离、向量库故障、模型异常和超时。聊天与嵌入模型均由可控替身提供，不调用真实模型或业务服务。

数据库结构由 Flyway 版本化管理，JPA 使用 `validate`，不会自动建表或修改表结构。

## 架构决策

- [ADR 0001：单 Agent 与确定性业务事务边界](docs/adr/0001-single-agent-and-deterministic-transactions.md)
- [ADR 0002：MySQL 业务事实与 Qdrant 可重建向量索引分离](docs/adr/0002-mysql-facts-and-qdrant-index.md)

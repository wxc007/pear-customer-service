# 01: 商品目录与项目纵向骨架

**What to build:** 游客可以浏览鲜梨及梨制品的分类、SPU 和具体 SKU，查看实时价格与商品状态；建立可运行的 Spring Boot 工程、MySQL 持久化、演示商品、REST API、简易页面和首个接口测试。

**Blocked by:** None (can start immediately)

**Status:** done

- [x] JDK 17 应用可以启动，本地 MySQL 可以通过 Docker Compose 提供，全部表结构由 Flyway 管理且 JPA 只校验结构。
- [x] 游客无需登录即可浏览分类、SPU 列表和 SKU 详情，鲜梨与梨制品展示各自正确的差异属性。
- [x] 商品价格使用定点金额，SPU 与 SKU 的上下架状态在查询结果中表现一致。
- [x] 演示商品明确标记为演示数据，非演示环境不会自动插入。
- [x] 简易页面可以完成商品列表到 SKU 详情的浏览，接口具有 ProblemDetail 错误语义。
- [x] 通过真实 MySQL 的 REST 接口测试验证公开商品行为，不断言内部实现结构。
- [x] 本地启动和首个演示流程具有简明说明。

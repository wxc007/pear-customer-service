## Agent skills

### Issue tracker

Issues and specs are tracked as local Markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context: root `GLOSSARY.md` and `docs/adr/`. See `docs/agents/domain.md`.

## 代码组织

- 每个业务领域包内按职责固定分包：`controller`、`service`、`dao`、`entity`、`dto`、`bootstrap`。
- 后续新增类必须直接创建到对应职责包中，不在领域根包平铺。
- 当前领域内的实体和领域枚举放在 `entity` 包；数据初始化类放在 `bootstrap` 包。

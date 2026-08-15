# Hexcessible Pinyin Addon

为 [Hexcessible](https://github.com/tizu69/hexcessible) 制作的附属插件（Fabric，Minecraft 1.20.1），
将原来魔改版里的**中文拼音搜索**能力独立出来，以 Mixin 方式注入原版模组，**不修改原版任何代码**。

> 灵感与算法来源：[KJH50/hexcessible-PinIn](https://github.com/KJH50/hexcessible-PinIn)（原魔改分支）
> 拼音引擎：[Towdium/PinIn](https://github.com/Towdium/PinIn)（MIT License）

## 功能

- **拼音搜索**：在 Hexcessible 的法术搜索框里输入拼音（如 `huo`、`huoyan`）即可匹配中文法术名（如「火焰」）
- **中文搜索**：直接输入中文片段也能模糊命中（CJK 名称 contains 回退）
- **语言感知权重**：自动检测游戏语言——
  - 中文环境：PinIn 拼音匹配优先（+5000），英文名/ID 辅助
  - 其他语言：英文名/ID 匹配优先（保持原版手感），拼音匹配辅助（+2000）

## 原理

原版 `dev.tizu.hexcessible.entries.PatternEntries#get(String)` 的评分逻辑是写死的，没有扩展点。
本插件用 **单个 Mixin** 在其方法头接管搜索：

- `@Inject(method = "get(Ljava/lang/String;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)`
- `@Shadow` 复用原版私有字段 `entries` 与 `fuzzySearchCache`
  - 搜索缓存继续由原版 `invalidateCaches()` 统一管理，无需额外清理逻辑
- 评分完全复刻原版逻辑（`z 索引 * 10000` + `Utils.fluffySearch` × 权重），
  再叠加 contains 回退与 PinIn 拼音加分
- 空查询、缓存命中仍走原版路径，行为完全兼容

## 目录结构

```
src/main/java/dev/kjh50/hexcessible/pinyin/
├── PinyinAddonClient.java        # Client 入口（打印加载日志）
├── mixin/
│   └── PatternEntriesSearchMixin.java  # 注入 PatternEntries#get(String)
└── search/
    └── PinyinSearcher.java       # PinIn 引擎 + 语言检测 + 评分逻辑
```

## 构建

前置：JDK 17。

```bash
# libs/ 需包含原版模组 jar（已通过 .gitignore 排除，请自行放置）
#   hexcessible-0.3.1.jar  ← https://github.com/tizu69/hexcessible/releases/tag/v0.3.1
#   serialization-hooks-0.4.99999.jar（可选，仅开发环境运行需要）

./gradlew build
```

产物：`build/libs/hexcessible-pinyin-addon-1.0.0.jar`

## 使用

把构建出的 jar 放进 `mods/` 即可（与下列模组一同使用）：

| 模组 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Fabric Loader | ≥ 0.16.14 |
| Fabric API | 0.92.2+1.20.1 |
| Hex Casting | 0.11.2-pre-702 |
| Hexcessible | 0.3.1 |

## License

MIT

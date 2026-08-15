# Hexcessible Pinyin Addon

为 [Hexcessible](https://github.com/tizu69/hexcessible) 制作的附属插件（Fabric，Minecraft 1.20.1），
将原来魔改版里的**中文拼音搜索**能力独立出来，以 Mixin 方式注入原版模组，**不修改原版任何代码**。

> 灵感与算法来源：[KJH50/hexcessible-PinIn](https://github.com/KJH50/hexcessible-PinIn)（原魔改分支）
> 拼音引擎：[Towdium/PinIn](https://github.com/Towdium/PinIn)（MIT License）

## 功能

- **拼音搜索**：在 Hexcessible 的法术搜索框里输入拼音（如 `huo`、`huoyan`）即可匹配中文法术名（如「火焰」）
- **中文搜索**：直接输入中文片段也能模糊命中（CJK 名称 contains 回退）
- **大小写不敏感**：拼音/英文查询统一按小写匹配（与原版英文搜索行为一致）
- **输入即搜**：施法界面空闲时直接输入字母/数字/汉字即可弹出法术搜索框——不必点击，
  也不依赖 Ctrl+Space（Windows 微软拼音默认把 Ctrl+Space 注册为「输入法开/关」系统热键，
  会在系统层拦截，游戏收不到该按键；本插件的「输入即搜」完全绕开这个问题）
- **语言感知权重**：自动检测游戏语言——
  - 中文环境：PinIn 拼音匹配优先（+5000），英文名/ID 辅助（name ×3、id ×1，与原版权重一致）
  - 其他语言：英文名/ID 匹配优先（name ×5、id ×3，高于原版权重以强化英文匹配），拼音匹配辅助（+2000）

## 原理

原版 `dev.tizu.hexcessible.entries.PatternEntries#get(String)` 的评分逻辑是写死的，没有扩展点。
本插件用 **单个 Mixin** 在其方法头接管搜索：

- `@Inject(method = "get(Ljava/lang/String;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)`
- `@Shadow` 复用原版私有字段 `entries` 与 `fuzzySearchCache`
  - 搜索缓存继续由原版 `invalidateCaches()` 统一管理，无需额外清理逻辑
- 评分沿用原版结构（`z 索引 * 10000` + `Utils.fluffySearch` × 权重），并叠加两个增强：
  - **contains 回退**：顺序模糊匹配失败但名称整体包含查询串时给基础分 10（中英文分支均生效），
    让仅含查询子串的 CJK 名称也能进入结果；代价是可能带入少量「仅包含」的噪声条目
  - **PinIn 拼音加分**：查询统一转为小写后做拼音匹配（中文环境 +5000，其他语言 +2000）
- 权重说明：中文分支 name ×3 / id ×1（与原版一致）；其他语言分支 name ×5 / id ×3
  （高于原版以强化英文匹配，因此该语言下结果排序与原版略有差异）
- 空查询、缓存命中仍走原版路径，行为完全兼容

## 目录结构

```
src/main/java/dev/kjh50/hexcessible/pinyin/
├── PinyinAddonClient.java        # Client 入口（打印加载日志）
├── mixin/
│   ├── PatternEntriesSearchMixin.java  # 注入 PatternEntries#get(String)
│   └── IdlingSearchOpenMixin.java      # 空闲状态输入字符即打开搜索框（模拟 Ctrl+Space）
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

> **关于 Ctrl+Space**：原版 hexcessible 用「施法界面内按 Ctrl+Space」打开搜索框。
> 但微软拼音等中文输入法默认把 Ctrl+Space 用作「输入法开/关」系统热键（OS 层直接拦截，
> 游戏收不到该按键），所以原版方式在中文系统上常常无效。本插件的「输入即搜」
> （空闲状态直接输入字符即打开搜索框）可完全替代；若仍想恢复 Ctrl+Space 原版行为，
> 可在 Windows「设置 → 时间和语言 → 语言和区域 → 中文 → 微软拼音 → 键盘选项 → 按键」中
> 更改或禁用「输入法开/关」热键，让游戏重新收到 Ctrl+Space。

| 模组 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Fabric Loader | ≥ 0.16.14 |
| Fabric API | 0.92.2+1.20.1 |
| Hex Casting | 0.11.2-pre-702 |
| Hexcessible | 0.3.1 |

## License

MIT License，详见 [LICENSE](LICENSE)。

随包分发的 PinIn（Towdium）等第三方组件版权声明见 [NOTICE.md](NOTICE.md)。

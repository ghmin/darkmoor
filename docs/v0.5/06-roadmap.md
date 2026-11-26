# 06 - 路线图

## 当前状态（v0.5）

### 已完成

- **依赖升级**：Gradle 2.2 → 8.7，libGDX 1.5.3 → 1.14.2，JDK 1.6 → 21
- **桌面后端迁移**：LWJGL2 → LWJGL3（`LwjglApplication` → `Lwjgl3Application`）
- **API 破坏修复**：
  - `ScissorStack.calculateScissors` 4-arg → 8-arg
  - `BitmapFont.TextBounds` → `GlyphLayout`
  - `font.draw(batch, str, x, y, w, h)` → 7-arg overload with `Align`
  - `Dialog.setTitle` → `getTitleLabel().setText`
- **构建脚本现代化**：AGP 1.0 → 8.5.2，`compile` → `implementation`/`api`，`classesDir` → `classesDirs`
- **仓库镜像**：阿里云 + 华为云 Maven 镜像，国内下载顺畅
- **Android 子项目禁用**：暂时不构建（代码保留）
- **iOS / HTML 子项目禁用**：RoboVM 死了，GWT 插件版本不兼容

### 编译状态

桌面端可以编译通过（v0.5 阶段的核心目标已达成）。运行时还有未验证的部分，详见下方"运行时风险"。

### 已知的代码遗留问题（v0.5 不修）

| 问题 | 位置 | 影响 |
|---|---|---|
| `StretchViewport(DISPLAY_WIDTH, DISPLAY_WIDTH)` 用了两次 width | `GameScreenBase.java:119` | 视口变形，但能跑 |
| 大量 `TODO` 注释（从 C# XNA 移植残留） | 全项目 | 不影响编译 |
| `stub` 包里多数类是空实现 | `com.ntk.darkmoor.stub` | 部分功能不可用 |
| `CharGen` 角色生成流程未走通 | `CharGen.java` | 不能新建角色 |
| 战斗系统未实现 | `Monster`/`Attack`/`Hero` | 怪物不会攻击 |
| 怪物 AI 状态机骨架已有，行为未实现 | `monsterstate/*` | 怪物不会动 |
| 法术施放未实现 | `SpellBook`/`SpellWindow` | 不能施法 |
| 存档功能未完成 | `SaveGameWindow`/`SaveGame` | 不能保存进度 |
| 自动地图是占位 | `AutoMap.java` | 只有 "TODO..." 文字 |

## 运行时风险（升级后没在游戏里实际测过的部分）

升级后这些 API 调用可能行为有变，**第一次跑通后要重点验证**：

### 1. LWJGL3 输入模型

LWJGL2 和 LWJGL3 的鼠标/键盘事件机制不完全一致。如果出现：
- 鼠标点击没响应 → 检查 `Gdx.input.setInputProcessor(stage)` 是否调到
- 多点触摸行为异常 → 桌面端通常用不到，但 Android 端要注意
- 光标位置不对 → LWJGL3 的坐标系可能跟 LWJGL2 不同

### 2. ScissorStack 行为

`MazeGroup.drawChildren` 里用 `ScissorStack` 剪裁地牢视图区域。新版 libGDX 的剪裁算法在边缘情况下可能略有差异。如果地牢视图区域显示异常，先注释掉剪裁代码看完整渲染。

### 3. AssetManager 错误处理

libGDX 1.10+ 改了默认错误处理：以前加载失败是静默记日志，现在会抛异常。如果某个贴图缺失，游戏会直接崩溃而不是继续跑。

可以临时设置回老行为：

```java
assetManager.setErrorHandler(new AssetErrorHandling() { ... });
```

### 4. FreeTypeFontGenerator 字符覆盖

新版 FreeTypeFontGenerator 默认字符集可能有变化。如果某些中文字符或符号显示成方块，需要显式指定字符集：

```java
FreeTypeFontParameter param = new FreeTypeFontParameter();
param.characters = "ABCabc123你好...";   // 显式列出需要的字符
```

### 5. BitmapFont draw 时坐标原点

libGDX 1.5 → 1.14 之间，`BitmapFont.draw(batch, str, x, y)` 的 `y` 坐标含义没变（基线位置），但如果你看到文字位置偏移，可能是 viewport 配置变了导致坐标系不一致。

## 下一阶段（v0.6）建议

按优先级从高到低：

### 优先级 1：跑通 + 修运行时 bug

1. **跑通主菜单 → 加载存档 → 进入地牢 → 移动 → 退出** 的完整流程
2. 修复上面"运行时风险"列出的问题
3. 验证 `StretchViewport(DISPLAY_WIDTH, DISPLAY_WIDTH)` 的 bug 是否影响视觉

### 优先级 2：补全基础战斗

1. 实现 `Monster.update()`：让怪物能看见玩家、追击
2. 实现 `monsterstate.Attack`：怪物攻击玩家
3. 实现 `Hero.attack()`：玩家攻击怪物
4. 实现 HP 结算和死亡处理

### 优先级 3：补全角色生成

1. 修通 `CharGen` 的状态机
2. 让用户能选种族、职业、属性
3. 把生成的角色保存到 `savegame.xml`

### 优先级 4：补全 UI 系统

1. 修通 `SaveGameWindow` 让用户能存档
2. 修通 `SpellWindow` 让用户能施法
3. 修通 `InventoryGroup` 让用户能管理物品

### 优先级 5：重新启用子项目

1. **Android**：取消 `settings.gradle` 里 `android` 的注释，恢复 `build.gradle` 里 AGP classpath
2. **HTML**：迁移到 `org.gwtproject.gradle:gwt-gradle-plugin:1.1.0` + GWT 2.10
3. **iOS**：迁移到 MobiVM（RoboVM 的维护 fork）

### 优先级 6：代码现代化

1. 把 `org.ntk.commons.StringUtils` 替换为 Apache Commons Lang 或 `java.util.Optional`
2. 删除空实现的 `stub` 包，用 libGDX 原生 API 替代
3. 把 `Gdx.app.debug` 改成 SLF4J + Logback（可选）
4. 引入 JUnit 5 替换 JUnit 4（`core/test` 里的测试）

## 文档维护

每次合并到主干后，对应模块文档需同步更新：

- 修改了某个类 → 更新对应的 `modules/XX-*.md`
- 修改了构建脚本 → 更新 `04-build-and-run.md`
- 修改了架构 → 更新 `02-architecture.md` 和 `03-module-map.md`
- 完成路线图里的一项 → 更新本文档

## 给接手维护者的建议

1. **先读** [02-architecture.md](02-architecture.md) 理解三层架构
2. **再读** [03-module-map.md](03-module-map.md) 找到关心的代码
3. **改代码前** 先读对应的 `modules/XX-*.md` 看类的设计意图
4. **改完后** 跑一次 `./gradlew desktop:run` 验证没破坏现有功能
5. **每次提交** 至少要保证编译通过（哪怕功能没完成）

## 结语

Darkmoor 是个有年头的项目，从 C# XNA 移植到 Java libGDX，原作者留下了大量 TODO 和未完成的代码。v0.5 阶段只是把它从"用旧工具链勉强能编译"提升到"用现代工具链能编译"，真正让游戏"好玩"还有很长的路要走。

如果你想贡献代码，建议从 [优先级 1：跑通 + 修运行时 bug](#优先级-1跑通--修运行时-bug) 开始 —— 这是最容易上手也最容易看到效果的工作。

# Darkmoor v0.5 文档索引

> **当前阶段：v0.5（半成品）**
> 目标：在升级到 Gradle 8.7 + libGDX 1.14.2 + JDK 21 之后，让项目能**编译通过并启动运行**。
> 此时游戏功能仍不完整（无战斗、无怪物 AI、新建角色流程未完成），但可以作为后续维护的基础。

## 文档清单

### 主文档

| 文件 | 内容 | 适合谁看 |
|---|---|---|
| [01-overview.md](01-overview.md) | 项目是什么、为什么存在、当前能做什么 | 第一次接触本项目的人 |
| [02-architecture.md](02-architecture.md) | 三层架构总览（平台层 / 屏幕层 / 引擎层），数据流 | 想理解整体设计的人 |
| [03-module-map.md](03-module-map.md) | 全部 12 个包的一句话说明 + 关键类清单 | 想快速定位代码的人 |
| [04-build-and-run.md](04-build-and-run.md) | 环境要求、构建命令、常见错误排查 | 准备本地跑起来的人 |
| [05-libgdx-crash-course.md](05-libgdx-crash-course.md) | libGDX 入门速览（给 Java 新手） | 没用过 libGDX 的人 |
| [06-roadmap.md](06-roadmap.md) | 已完成 / 未完成 / 下一步建议 | 决定接下来做什么的人 |
| [07-i18n-system.md](07-i18n-system.md) | 多语言系统详解 + 中文化改造记录 | 改翻译 / 加新语言的人 |
| [08-minimap-design.md](08-minimap-design.md) | 小地图模块设计与实现 | 想加地图功能的人 |
| [09-input-system.md](09-input-system.md) | 键盘输入与操控系统（按键绑定 / 配置 UI） | 想改按键 / 加键盘操控的人 |
| [10-camp-system.md](10-camp-system.md) | CAMP 按钮与营地对话框系统 | 想修 CAMP / 加营地功能的人 |
| [11-ui-research.md](11-ui-research.md) | 项目界面系统调研报告 + 候选 UI 库评估 | 决定 UI 迁移方案的人 |
| [12-visui-migration-log.md](12-visui-migration-log.md) | 引入 VisUI 1.5.9 的工作日志 | 接手 UI 迁移的人 |
| [13-random-event-system.md](13-random-event-system.md) | 随机事件系统解构 + RandomEventBus 统一总线 | 维护事件/脚本系统的人 |
| [14-move-blocked-feedback.md](14-move-blocked-feedback.md) | 撞墙（移动受阻）玩家反馈系统设计与实现 | 调整手感反馈的人 |

### 子模块详细文档（位于 `modules/` 目录）

| 文件 | 覆盖包 | 关键类 |
|---|---|---|
| [modules/01-top-level.md](modules/01-top-level.md) | `com.ntk.darkmoor` | `DarkmoorGame`、`GameScreen`、`MainMenu`、`CharGen` |
| [modules/02-config.md](modules/02-config.md) | `com.ntk.darkmoor.config` | `Settings`、`SaveGame`、`LanguagesManager`、`Log` |
| [modules/03-resource.md](modules/03-resource.md) | `com.ntk.darkmoor.resource` | `Resources`、`TextureSet`、`GraphicAssets`、`AssetManager` |
| [modules/04-engine-core.md](modules/04-engine-core.md) | `com.ntk.darkmoor.engine`（根包） | `Dungeon`、`Maze`、`Square`、`Team`、`Hero`、`Monster`、`Item`、`Spell` |
| [modules/05-engine-actor.md](modules/05-engine-actor.md) | `com.ntk.darkmoor.engine.actor` | `Door`、`Stair`、`PressurePlate`、`Teleporter`、`WallSwitch` |
| [modules/06-engine-graphics.md](modules/06-engine-graphics.md) | `com.ntk.darkmoor.engine.graphics` | `MazeGroup`、`HeroInterfaceGroup`、`NavigationControlsGroup` |
| [modules/07-engine-gui.md](modules/07-engine-gui.md) | `com.ntk.darkmoor.engine.gui` | `GUI`、`BaseWindow`、`CampDialog`、`MessageBox`、`ScreenButton` |
| [modules/08-engine-campwindows.md](modules/08-engine-campwindows.md) | `com.ntk.darkmoor.engine.gui.campwindows` | `MainWindow`、`SpellWindow`、`RestPartyWindow`、`SaveGameWindow` |
| [modules/09-engine-script.md](modules/09-engine-script.md) | `com.ntk.darkmoor.engine.script` (+ `actions` + `gui`) | `ScriptBase`、`ActionBase`、20+ 个 action |
| [modules/10-engine-monsterstate.md](modules/10-engine-monsterstate.md) | `com.ntk.darkmoor.engine.monsterstate` | `MonsterState`、`Attack`、`Hurt`、`Idle` |
| [modules/11-exception-stub-util.md](modules/11-exception-stub-util.md) | `exception` + `stub` + `util` | `BaseException`、`Display`、`Mouse`、`RandomEnum` |
| [modules/12-maze-system.md](modules/12-maze-system.md) | 地图系统（`Dungeon` / `Maze` / `Square` / `DungeonLocation` / `Team` 位置） | 想加地图 / 小地图 / 自动化地图的人 |

## 阅读建议

**完全的新人**：按编号 01 → 02 → 03 → 05 → 04 顺序看，再挑一个感兴趣的子模块深入。

**有 libGDX 经验的人**：直接看 `03-module-map.md` 找到关心的包，跳到对应子模块文档。

**只想跑起来的人**：直接看 `04-build-and-run.md`。

**接手维护的人**：所有文档都要看，特别是 `06-roadmap.md`。

## 文档约定

- 所有类名用 `Mono 字体` 包裹，例如 `DarkmoorGame`。
- 所有文件路径用 `反引号 + 斜杠` 表示，例如 `core/src/com/ntk/darkmoor/`。
- 文档中提到的"v0.5 阶段"特指 2026-11 当前的依赖更新与语法修正阶段。
- 文档与代码同步更新原则：每次合并到主干后，相关文档需对应修订。

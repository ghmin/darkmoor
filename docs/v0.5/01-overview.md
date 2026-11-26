# 01 - 项目概述

## 一句话介绍

**Darkmoor** 是经典 CRPG 游戏 *Eye of the Beholder II: The Legend of Darkmoon*（《魔眼暗黑月传说 II》，1991 年 Westwood Studios 出品）的 Java 开源复刻版，基于 libGDX 游戏框架开发。

## 这个项目从哪儿来

- **原作**：*Eye of the Beholder II*（1991），D&D 第二版规则的"地牢爬行"（dungeon crawler）/ "Blobber" 类 CRPG。
- **C# 起源**：原作者 `ntk4`（Nick）最初用 C# + XNA 框架写过一个版本，后来移植到 Java + libGDX，命名为 `darkmoor`。
- **资源兼容**：项目设计上与另一个开源复刻 [Dungeon Eye](http://www.dungeoneye.net/)（C# 版）的资源格式兼容，因此可以借用 Dungeon Eye 的资源编辑器。
- **现状**：原仓库 `ntk4/darkmoor` 自 2015 年后基本停滞；2026 年由 `ghmin` fork 后开始进行依赖更新与维护，本系列文档对应 fork 后的 v0.5 阶段。

## 项目目标（按阶段）

| 阶段 | 目标 | 状态 |
|---|---|---|
| **v0.4** | 原作者 2015 年停止维护时的状态：能编译、能加载预定义存档、能在地牢里走动 | 历史 |
| **v0.5（当前）** | 把 Gradle 2.2 / libGDX 1.5.3 / JDK 1.6 升级到 Gradle 8.7 / libGDX 1.14.2 / JDK 21，让项目在现代工具链上能继续编译运行 | **进行中** |
| **v0.6+** | 修复运行时问题、补全战斗系统、补全怪物 AI、补全新建角色流程 | 未开始 |

## 当前能做什么（v0.5 阶段）

启动 `desktop:run` 后，预期可以看到：

- 主菜单（MainMenu）— 显示标题图，有 Load Game / Options / Exit 三个按钮
- 加载预定义存档（`android/assets/data/savegame.xml`）
- 进入地牢后可以**第一人称视角、网格化移动**（标准 Blobber 操作）
- 移动方向有 4 个罗盘方位（北/东/南/西），可前进、后退、左转、右转
- 界面下方显示小队头像（最多 6 人）
- 可以挥武器（但只能挥空气，没有敌人）

## 当前**不能**做什么

- 不能新建角色（CharGen 屏幕部分实现，但流程未走通）
- 不能战斗（怪物会显示，但没有 AI、没有伤害结算）
- 不能施法（SpellBook 类已存在，但 SpellWindow UI 未完整）
- 不能保存游戏（SaveGameWindow UI 存在，但保存逻辑未完成）
- iOS / HTML5 子项目已禁用（RoboVM 死了、GWT 插件版本不兼容）

## 技术栈一览

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 21（构建），8（core 字节码） | core 保持 Java 8 字节码以便 Android 兼容 |
| Gradle | 8.7 | 通过 wrapper 提供 |
| libGDX | 1.14.2 | 最新稳定版，LWJGL3 后端 |
| LWJGL | 3（随 libGDX 1.14 一起） | 桌面端底层（OpenGL、输入、音频） |
| 本地工具 jar | `libs/ntk-commons-android-0.1.jar` | 仅含 `StringUtils` 等工具类 |
| Android（已禁用） | AGP 8.5.2 / minSdk 21 / targetSdk 34 | 代码保留但不构建 |

## 项目目录结构

```
darkmoor/
├── build.gradle                # 根构建脚本（所有子项目共享配置）
├── settings.gradle             # 当前只 include 'core', 'desktop'
├── gradle.properties           # JVM 参数 + AndroidX 开关
├── gradle/wrapper/             # Gradle wrapper（指向本地 gradle-8.7-bin.zip）
├── libs/
│   └── ntk-commons-android-0.1.jar   # 原作者自带的工具 jar
├── core/                       # 跨平台共享代码（游戏逻辑、引擎、资源加载）
│   ├── build.gradle
│   └── src/com/ntk/darkmoor/
│       ├── DarkmoorGame.java   # 程序入口（ApplicationListener 实现）
│       ├── GameScreen.java     # 主游戏屏幕
│       ├── MainMenu.java       # 主菜单屏幕
│       ├── CharGen.java        # 角色生成屏幕（未完成）
│       ├── config/             # 配置、存档、i18n、日志
│       ├── engine/             # 核心游戏引擎（地牢、小队、英雄、怪物、物品、法术）
│       ├── exception/          # 自定义异常
│       ├── resource/           # 资源加载（贴图、字体、动画）
│       ├── stub/               # 从 C# XNA 移植时留下的占位类
│       └── util/               # 通用工具
├── desktop/                    # 桌面端启动器
│   ├── build.gradle
│   └── src/com/ntk/darkmoor/desktop/DesktopLauncher.java
├── android/                    # 安卓端（已禁用，代码保留）
│   ├── build.gradle
│   ├── AndroidManifest.xml
│   ├── assets/data/            # ★ 所有游戏资源（XML 配置 + PNG 贴图 + 字体 + 音效）
│   └── src/com/ntk/darkmoor/android/AndroidLauncher.java
├── html/                       # GWT Web 版（已禁用，代码保留）
├── ios/                        # RoboVM iOS 版（已禁用，代码保留）
└── docs/v0.5/                  # 本文档
```

## 接下来读什么

- 想理解整体架构 → [02-architecture.md](02-architecture.md)
- 想找具体某个类的代码 → [03-module-map.md](03-module-map.md)
- 想立刻把项目跑起来 → [04-build-and-run.md](04-build-and-run.md)
- 没用过 libGDX → [05-libgdx-crash-course.md](05-libgdx-crash-course.md)
- 想知道接下来该做什么 → [06-roadmap.md](06-roadmap.md)

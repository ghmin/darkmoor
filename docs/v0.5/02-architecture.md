# 02 - 架构总览

## 三层架构

Darkmoor 代码可以分为三层，从上到下越来越底层：

```
┌──────────────────────────────────────────────────────────────────┐
│  平台层（Platform Layer）                                          │
│  desktop / android / ios / html                                   │
│  每个 platform 只有一个 Launcher 类，负责创建 libGDX Application    │
└──────────────────────────────────────────────────────────────────┘
                              ↓ 启动
┌──────────────────────────────────────────────────────────────────┐
│  屏幕层（Screen Layer）                                            │
│  com.ntk.darkmoor 顶层包                                          │
│  DarkmoorGame → ScreenManager → 各 Screen                          │
│  负责"现在该显示哪个屏幕"（主菜单 / 角色生成 / 游戏 / 选项）         │
└──────────────────────────────────────────────────────────────────┘
                              ↓ 调用
┌──────────────────────────────────────────────────────────────────┐
│  引擎层（Engine Layer）                                            │
│  com.ntk.darkmoor.engine.*                                        │
│  - 核心数据：Dungeon / Maze / Square / Team / Hero / Monster       │
│  - 渲染：graphics 子包（scene2d Group 实现）                       │
│  - UI：gui 子包（BaseWindow + CampDialog + 各 camp window）        │
│  - 脚本：script 子包（XML 驱动的事件系统）                          │
│  - 怪物 AI：monsterstate 子包（状态机）                             │
└──────────────────────────────────────────────────────────────────┘
                              ↓ 加载
┌──────────────────────────────────────────────────────────────────┐
│  资源层（Resource Layer，横切关注点）                              │
│  com.ntk.darkmoor.resource                                        │
│  Resources（AssetManager 单例）+ TextureSet + GraphicAssets        │
└──────────────────────────────────────────────────────────────────┘
```

## 平台层

每个平台只有一个入口类，非常薄：

| 平台 | 入口类 | 入口方法 | 作用 |
|---|---|---|---|
| Desktop | `desktop/.../DesktopLauncher.java` | `main(String[] arg)` | 创建 `Lwjgl3Application`，传入 `DarkmoorGame` 实例 |
| Android | `android/.../AndroidLauncher.java` | `onCreate(Bundle)` | 继承 `AndroidApplication`，调用 `initialize(new DarkmoorGame(), config)` |
| iOS（禁用） | `ios/.../IOSLauncher.java` | `main(String[])` | RoboVM，原计划用 `IOSApplication` |
| HTML（禁用） | `html/.../HtmlLauncher.java` | `getApplicationListener()` | GWT，原计划用 `GwtApplication` |

**关键点**：所有平台最终都会创建一个 `DarkmoorGame` 实例，由 libGDX 框架回调它的 `create()` / `render()` / `pause()` / `resume()` / `dispose()` 等生命周期方法。所以**所有游戏逻辑都写在 `core` 模块里，平台层只负责"启动"**。

## 屏幕层

### DarkmoorGame - 程序主入口

`DarkmoorGame` 继承自 libGDX 的 `Game` 类，实现 `ApplicationListener`。它是整个游戏的"门面"：

```java
public class DarkmoorGame extends Game {
    public void create() {
        instance = this;
        executor = new AsyncExecutor(MAX_THREADS);
        loadStartupData();              // 加载 settings + savegame
        camera = new OrthographicCamera();
        batch = new SpriteBatch();
        setScreen(getMainMenu());       // 进入主菜单
    }
    // ... pause / resume / dispose ...
}
```

关键职责：
- **单例**：通过 `DarkmoorGame.getInstance()` 全局访问（典型的 libGDX 模式）
- **资源加载**：调用 `Resources.loadGameStartupResources()` 启动 `AssetManager` 加载贴图/字体
- **屏幕切换**：通过 `setScreen()` 切到不同的 `Screen`
- **任务异步**：通过 `AsyncExecutor` 后台加载地牢（避免阻塞主线程）

### GameScreenBase - 屏幕基类

所有具体屏幕都继承 `GameScreenBase`，它提供：
- 一个 libGDX `Stage`（场景图）
- 一个 `Skin`（UI 皮肤）
- `setupStage()` / `setupButtons()` 模板方法
- `resize()` / `dispose()` 标准实现

### 具体屏幕

| 屏幕 | 文件 | 作用 | v0.5 状态 |
|---|---|---|---|
| 主菜单 | `MainMenu.java` | 显示标题图 + Load/Options/Exit 三个按钮 | 可用 |
| 游戏 | `GameScreen.java` | 地牢探索主屏幕 | 可用（无战斗） |
| 角色生成 | `CharGen.java` | 创建角色流程 | 部分实现 |
| 介绍 | `IntroScreen.java` | 开场剧情 | 部分实现 |
| 选项 | `OptionsWindow.java` | 设置面板（作为弹窗） | 可用 |

### ScreenManager - 屏幕管理器

`ScreenManager` 是个静态工具类，负责屏幕间切换：

```java
ScreenManager.init(darkmoorGame);
ScreenManager.showIntroScreen();
ScreenManager.exitScreen();
```

注意：实际项目里很多地方直接用 `game.setScreen(...)` 而不走 `ScreenManager`，这是历史遗留，未来可以统一。

## 引擎层

引擎层是项目最重的一块，占了 ~60% 的代码量。按子包分：

### `engine` 根包 - 核心数据模型

| 类 | 角色 |
|---|---|
| `Dungeon` | 整个地牢，包含多个 `Maze`（楼层） |
| `Maze` | 一个楼层（二维网格），由 `Square[][]` 组成 |
| `Square` | 一个网格格子，可包含门、楼梯、陷阱、怪物等 actor |
| `DungeonLocation` | "在哪个 maze 的哪个坐标、面朝哪个方向" |
| `Team` | 玩家小队（最多 6 个 `Hero`），有当前 `DungeonLocation` |
| `Hero` | 一个英雄角色，有属性、职业、物品栏、法术书 |
| `Monster` | 一只怪物，有 HP、AI 状态机、攻击力 |
| `Item` | 一件物品（武器、护甲、消耗品、杂物） |
| `Spell` | 一个法术定义 |
| `ViewField` | 当前视野里 15 个格子（A-O）的可见性计算 |

### `engine.actor` 子包 - 格子内的"演员"

每个 `Square` 可以包含若干 `SquareActor`：

| Actor | 含义 |
|---|---|
| `Door` | 门（可开关、可上锁） |
| `Stair` | 楼梯（连接不同 maze） |
| `PressurePlate` | 踩踏板（玩家踩上去触发） |
| `WallSwitch` | 墙上按钮 |
| `Teleporter` | 传送点 |
| `Pit` | 陷阱坑 |
| `ForceField` | 力场（阻挡通行） |
| `AlcoveActor` | 壁龛（可放物品） |
| `EventSquare` | 事件格（踏入触发脚本） |
| `Generator` | 怪物发生器 |

### `engine.graphics` 子包 - 场景图渲染

每个屏幕的视觉部分由若干 libGDX `Group` 组成：

| Group | 渲染内容 |
|---|---|
| `MazeGroup` | 第一人称地牢视图（最复杂，处理 15 格视野、深度排序、剪裁） |
| `HeroInterfaceGroup` | 屏幕下方的小队头像栏 |
| `InventoryGroup` | 物品栏（角色身上的装备格） |
| `NavigationControlsGroup` | 移动控制按钮（前进/后退/左转/右转） |
| `StatisticsGroup` | 数值面板（HP/MP 等） |

### `engine.gui` 子包 - 游戏内 UI

- `GUI` - 工具类，封装画矩形、画斜面边框、获取共享字体等
- `BaseWindow` - 所有弹窗的基类，继承 libGDX `Dialog`
- `CampDialog` - "营地"对话框，包含多个子窗口
- `MessageBox` / `MessageBox2` - 消息提示框
- `ScreenButton` - 自定义按钮（基于 libGDX `Button`）

### `engine.gui.campwindows` 子包 - 营地子窗口

`CampDialog` 内部用栈管理多个 `BaseWindow`：

- `MainWindow` - 营地主菜单
- `SpellWindow` - 法术列表
- `RestPartyWindow` - 休息
- `SaveGameWindow` / `LoadGameWindow` - 存档/读档
- `GameOptionsWindow` / `PreferencesWindow` - 选项
- `DropNPCWindow` - 离队 NPC
- `ScribeScrollsWindow` - 抄写卷轴

### `engine.script` 子包 - XML 驱动的事件系统

这是项目里**最容易被新手忽略但最重要**的一块。游戏里所有"触发 X 后发生 Y"的逻辑（按下墙上的按钮、踩到踏板、踏入事件格、施法、对话）都由脚本系统驱动。

- `ScriptBase` - 脚本基类，每个子类对应一种触发源（如 `WallSwitchScript`、`PressurePlateScript`）
- `ActionBase` - 动作基类，每个子类对应一种"动作"
- `actions/` 包 - 20+ 个具体动作：
  - `ActivateTarget` / `DeactivateTarget` / `ToggleTarget` - 启停游戏对象
  - `DisplayMessage` - 显示消息
  - `GiveItem` / `GiveExperience` / `Healing` - 给玩家东西
  - `Teleport` - 传送
  - `SpawnMonster` - 召唤怪物
  - `EndDialog` / `EndChoice` - 结束对话
  - 等等
- `gui/GUIScriptButton` / `ScriptChoice` - 脚本对话框里的按钮和选项

数据从 `android/assets/data/Script.xml` 加载，结构如下：

```xml
<script script="Spells" interface="CureMinorWounds">
    <action name="Healing" target="..." />
    <action name="DisplayMessage" target="..." />
</script>
```

### `engine.monsterstate` 子包 - 怪物 AI 状态机

每个 `Monster` 持有一个 `MonsterState`，根据当前情境切换：

- `Idle` - 待机
- `Attack` - 攻击
- `Hurt` - 受伤
- `StateAdapter` - 状态适配器（空实现，方便子类只重写关心的方法）

**v0.5 状态**：状态机骨架已搭好，但具体行为逻辑大部分未实现。

## 资源层

### Resources - 全局资源管理器

`Resources` 是一个静态工具类，包装了 libGDX 的 `AssetManager`：

```java
Resources.loadGameStartupResources();   // 启动时加载主菜单资源
Resources.loadGameResources();          // 进入游戏时加载地牢资源
Resources.unloadResources();            // 退出时卸载
Resources.getAssetManager();            // 获取底层 AssetManager
```

### TextureSet - 贴图集

`TextureSet` 类代表"一张大图 + 元数据"，把一张 PNG 切成多个 Sprite：

```java
TextureSet set = Resources.loadSharedTextureSetAsset("wall-forest");
Sprite wall = set.getSprite(5);   // 取第 5 个 sprite（一面墙）
```

`TextureSet.xml` 描述每个 tile 在大图里的位置、尺寸、翻转效果。

### 其他资源类

- `GraphicAssets` - 通用图形资产
- `AnimationAssets` - 动画资产
- `DecorationAssets` - 装饰物资产
- `ItemAssets` - 物品图标资产
- `DungeonLoadingTask` - 后台异步加载地牢的任务

## 数据流总览

下面以"玩家按下前进键，移动一格"为例，画出调用链：

```
用户按 W 键
    ↓
NavigationControlsGroup (libGDX InputListener)
    ↓ button.touchDown()
Team.moveForward()
    ↓
DungeonLocation.coordinate.y += 1   (根据当前朝向)
    ↓
Square newSquare = Maze.getSquare(coordinate)
    ↓
触发 Square 的 OnTeamEnter 事件
    ↓
EventSquare.run() → ScriptBase.run() → ActionBase.run()
    ↓ 例如 SpawnMonster / DisplayMessage / Teleport
MazeGroup.update()   (下一帧渲染时重新计算 ViewField)
    ↓
绘制新的 15 格视野 → 屏幕
```

## 关键设计模式

- **单例**：`DarkmoorGame.getInstance()`、`LanguagesManager.getInstance()`、`Settings.getLastLoadedInstance()`
- **模板方法**：`GameScreenBase.setupStage()` / `setupButtons()` 由子类实现
- **状态机**：`Monster` ↔ `MonsterState` 子类
- **命令模式**：`ScriptBase` 持有一串 `ActionBase`，按顺序 `run()`
- **栈式导航**：`CampDialog.windows` 是 `ArrayDeque<BaseWindow>`，类似浏览器历史
- **AssetManager 异步加载**：所有贴图/字体通过 `Resources` 统一加载，避免内存泄漏

## 接下来读什么

- [03-module-map.md](03-module-map.md) - 看每个包里有哪些类
- [04-build-and-run.md](04-build-and-run.md) - 跑起来再说
- 或直接跳到 [modules/](modules/) 看具体模块的代码细节

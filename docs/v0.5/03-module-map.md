# 03 - 模块地图

每个 Java 包一句话说明 + 关键类清单。点击类名可以跳到对应源码文件（在 IDE 里）。

## 顶层包 `com.ntk.darkmoor`

**职责**：libGDX 应用入口 + 屏幕管理。

| 类 | 一句话说明 |
|---|---|
| `DarkmoorGame` | 程序主入口，继承 `Game`，单例，管理生命周期 |
| `ScreenManager` | 屏幕切换的静态工具类 |
| `GameScreenBase` | 所有屏幕的抽象基类，封装 Stage/Skin |
| `MainMenu` | 主菜单屏幕 |
| `GameScreen` | 地牢探索主屏幕 |
| `CharGen` | 角色生成屏幕（未完成） |
| `IntroScreen` | 开场剧情屏幕 |
| `OptionsWindow` | 选项窗口（弹窗形式） |

详细文档：[modules/01-top-level.md](modules/01-top-level.md)

## `com.ntk.darkmoor.config`

**职责**：游戏配置、存档、多语言、日志。

| 类 | 一句话说明 |
|---|---|
| `Settings` | 用户设置（语言/音效/输入方案等），读写 `settings.properties` |
| `SaveGame` | 存档文件管理，最多 6 个 slot |
| `SaveGameSlot` | 单个存档槽 |
| `LanguagesManager` | 多语言管理，单例，从 `StringTable.xml` 加载 |
| `Log` | 日志工具，包装 `Gdx.app.debug/error` |
| `GameLanguage` | 语言枚举 |
| `GameMechanics` | 游戏规则常量 |
| `InputType` | 输入方式枚举 |

详细文档：[modules/02-config.md](modules/02-config.md)

## `com.ntk.darkmoor.resource`

**职责**：贴图、字体、动画等资源的加载与缓存。

| 类 | 一句话说明 |
|---|---|
| `Resources` | 全局资源门面，包装 libGDX `AssetManager` |
| `TextureSet` | 贴图集（一张大图切多个 Sprite） |
| `GraphicAssets` | 通用图形资产 |
| `AnimationAssets` | 动画资产 |
| `DecorationAssets` | 装饰物资产 |
| `ItemAssets` | 物品图标资产 |
| `TextureMetadata` | 单个 tile 的元数据 |
| `ResizableRectangle` | 可缩放矩形（UI 用） |
| `ResourceUtility` | XML 读取工具 |
| `DungeonLoadingTask` | 后台加载地牢的异步任务 |

详细文档：[modules/03-resource.md](modules/03-resource.md)

## `com.ntk.darkmoor.engine`（根包）

**职责**：核心数据模型与游戏逻辑。

| 类 | 一句话说明 |
|---|---|
| `Dungeon` | 整个地牢，包含多个 `Maze` |
| `Maze` | 一个楼层（二维 `Square` 网格） |
| `Square` | 一个格子，可包含 actor |
| `MazeZone` | maze 内的分区 |
| `DungeonLocation` | 位置（maze + 坐标 + 朝向） |
| `ViewField` | 当前视野里 15 个格子的可见性 |
| `Team` | 玩家小队（最多 6 个 `Hero`） |
| `Hero` | 英雄角色（属性/职业/物品/法术） |
| `Monster` | 怪物 |
| `Item` | 物品 |
| `Spell` | 法术 |
| `SpellBook` | 法术书 UI |
| `Compass` | 罗盘（4 方位枚举） |
| `Decoration` / `DecorationSet` | 装饰物 |
| `Entity` | 生物基类（Hero/Monster 共用） |
| `Ability` | D&D 属性（力量/敏捷等） |
| `Attack` / `Defense` / `CriticalHit` | 战斗数据 |
| `Dice` | 骰子（D&D 的 d20 等） |
| `Profession` | 职业 |
| `HitPoint` | HP 数据 |
| `ThrownItem` | 投掷物 |
| `WallButton` | 墙按钮 |
| `SwitchCount` | 开关次数记录 |
| `AutoMap` | 自动地图（占位） |
| `CampDialog` | 营地对话框 |
| `DialogBase` | 对话框基类 |
| `ScriptedDialog` | 脚本对话 |
| `GameMessage` / `ScreenMessage` | 游戏内消息 |
| `DisplayCoordinates` / `InterfaceCoordinates` | 屏幕坐标常量 |
| `GameColors` | 颜色常量 |
| `HandAction` | 手部动作（用物品时） |
| `HeroAllowedClasses` | 允许的职业列表 |
| `HeroExperienceLevels` | 经验等级 |
| `HeroHPBonus` | HP 加成 |
| `SavingThrowType` | D&D 豁免检定类型 |
| `State` | 状态枚举 |
| `MazeElements` | maze 元素枚举 |

详细文档：[modules/04-engine-core.md](modules/04-engine-core.md)

## `com.ntk.darkmoor.engine.actor`

**职责**：格子内的"演员"——可触发互动的对象。

| 类 | 一句话说明 |
|---|---|
| `SquareActor` | actor 基类 |
| `Door` | 门（开关/上锁） |
| `Stair` | 楼梯 |
| `PressurePlate` | 踩踏板 |
| `WallSwitch` | 墙上开关 |
| `Teleporter` | 传送点 |
| `Pit` | 陷阱 |
| `ForceField` | 力场 |
| `AlcoveActor` | 壁龛 |
| `EventSquare` | 事件格 |
| `Generator` | 怪物生成器 |
| `Launcher` | 投射物发射器 |

详细文档：[modules/05-engine-actor.md](modules/05-engine-actor.md)

## `com.ntk.darkmoor.engine.graphics`

**职责**：把数据模型画到屏幕上。

| 类 | 一句话说明 |
|---|---|
| `GameScreenGroup` | 所有 game screen Group 的基类 |
| `MazeGroup` | 第一人称地牢视图（最复杂） |
| `HeroInterfaceGroup` | 小队头像栏 |
| `InventoryGroup` | 物品栏 |
| `NavigationControlsGroup` | 移动控制按钮 |
| `StatisticsGroup` | 数值面板 |
| `TileDrawing` | 单个 tile 的绘制数据 |
| `SpriteEffects` | 翻转效果枚举 |

详细文档：[modules/06-engine-graphics.md](modules/06-engine-graphics.md)

## `com.ntk.darkmoor.engine.gui`

**职责**：游戏内 UI 组件。

| 类 | 一句话说明 |
|---|---|
| `GUI` | UI 工具类（画矩形/边框/取字体） |
| `BaseWindow` | 弹窗基类（继承 libGDX `Dialog`） |
| `CampDialog` | 营地对话框（含多个子窗口的栈） |
| `MessageBox` / `MessageBox2` | 消息提示框 |
| `ScreenButton` | 自定义按钮 |

详细文档：[modules/07-engine-gui.md](modules/07-engine-gui.md)

## `com.ntk.darkmoor.engine.gui.campwindows`

**职责**：营地对话框里的各个子窗口。

| 类 | 一句话说明 |
|---|---|
| `MainWindow` | 营地主菜单 |
| `SpellWindow` | 法术选择 |
| `RestPartyWindow` | 休息 |
| `SaveGameWindow` | 存档 |
| `LoadGameWindow` | 读档 |
| `GameOptionsWindow` | 游戏选项 |
| `PreferencesWindow` | 偏好设置 |
| `DropNPCWindow` | NPC 离队 |
| `ScribeScrollsWindow` | 抄写卷轴 |

详细文档：[modules/08-engine-campwindows.md](modules/08-engine-campwindows.md)

## `com.ntk.darkmoor.engine.script` + `script.actions` + `script.gui`

**职责**：XML 驱动的事件/脚本系统。

| 类 | 一句话说明 |
|---|---|
| `ScriptBase` | 脚本基类 |
| `AlcoveScript` / `PressurePlateScript` / `WallSwitchScript` | 触发源脚本 |
| `ActionBase` | 动作基类 |
| `ActivateTarget` / `DeactivateTarget` / `ToggleTarget` | 启停对象 |
| `DisplayMessage` | 显示消息 |
| `GiveItem` / `GiveExperience` / `Healing` | 给玩家东西 |
| `Teleport` | 传送 |
| `SpawnMonster` | 召唤怪物 |
| `JoinCharacter` | NPC 加入队伍 |
| `PlaySound` | 播放音效 |
| `ChangePicture` / `ChangeText` | 改 UI 内容 |
| `EnableChoice` / `DisableChoice` / `EndChoice` | 对话选项控制 |
| `EnableTarget` / `DisableTarget` | 启停触发目标 |
| `EndDialog` | 结束对话 |
| `SetTo` | 设置状态 |
| `GUIScriptButton` | 脚本对话框按钮 |
| `ScriptChoice` | 脚本对话框选项 |

详细文档：[modules/09-engine-script.md](modules/09-engine-script.md)

## `com.ntk.darkmoor.engine.monsterstate`

**职责**：怪物 AI 状态机。

| 类 | 一句话说明 |
|---|---|
| `MonsterState` | 状态基类（持有 `Monster` 引用） |
| `StateAdapter` | 空实现适配器 |
| `Idle` | 待机 |
| `Attack` | 攻击 |
| `Hurt` | 受伤 |

详细文档：[modules/10-engine-monsterstate.md](modules/10-engine-monsterstate.md)

## `com.ntk.darkmoor.exception`

**职责**：项目自定义异常，全部继承 `BaseException`（RuntimeException）。

| 类 | 一句话说明 |
|---|---|
| `BaseException` | 基类 |
| `InitializationException` | 初始化失败 |
| `LoadException` | 资源加载失败 |
| `ResourceException` | 资源错误 |
| `SaveException` | 存档错误 |
| `StateException` | 状态机错误 |

## `com.ntk.darkmoor.stub`

**职责**：从 C# XNA 移植时留下的占位类，多数已废弃。

| 类 | 一句话说明 |
|---|---|
| `Display` | 显示工具（GL 清屏等） |
| `Mouse` / `MouseButtons` | 鼠标占位 |
| `GameTime` | 游戏时间占位 |
| `Scene` | 场景占位 |
| `AudioSample` | 音效占位 |
| `ScriptInterface` | 脚本接口占位 |

## `com.ntk.darkmoor.util`

**职责**：通用工具。

| 类 | 一句话说明 |
|---|---|
| `RandomEnum` | 从枚举里随机取一个值的工具 |

## `com.ntk.darkmoor.engine.interfaces`

**职责**：抽象接口（标记用）。

| 接口 | 一句话说明 |
|---|---|
| `IItem` | 物品接口 |
| `IMonster` | 怪物接口 |
| `ISpell` | 法术接口 |

详细文档：[modules/11-exception-stub-util.md](modules/11-exception-stub-util.md)

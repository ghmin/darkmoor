# 13 · 随机事件系统解构与 RandomEventBus 统一总线

> 轮次：v0.5 第十轮（2026-11-27）
> 关联代码：`engine/event/RandomEventBus.java`、`engine/event/RandomEvent.java`、
> `engine/ScriptedDialog.java`、`engine/actor/EventSquare.java`、`engine/script/**`
> 关联文档：`11-ui-research.md`（弹窗选型）、`12-visui-migration-log.md`（VisUI 迁移）、`14-move-blocked-feedback.md`

---

## 1. 结论速览

**用户判断正确：随机事件系统在本项目里是一个"数据完备、执行断裂"的半成品。**
10 轮排查后，我们把它的真实完成度量化如下：

| 层 | 组件 | 完成度 | 说明 |
| --- | --- | --- | --- |
| 数据层 | dungeon.xml 事件节点 | **≈95%** | 24 个 eventsquare + 8 类机关 actor 全部有数据定义 |
| 触发层 | SquareActor.onTeamEnter() | **≈70%** | 8 类 actor 已实现触发分发；saving throw 判定可用 |
| 动作层 | ScriptChoice → ActionBase | **≈45%** | 21 个动作类只有 9 个真正可执行，其余 run() 为空壳 |
| 展示层 | ScriptedDialog 弹窗 | **0% → 已修复** | 旧渲染链路根本不会执行，弹窗从未显示过（§3.1） |
| 消息层 | GameMessage | **0% → 旁路** | draw()/update() 无人调用，是死代码（§3.2） |
| 区域脚本 | MazeZone.onTeamEnterScript | **0%** | 字段加载/保存了，但没有任何执行点（§3.4） |

本轮交付 **RandomEventBus 统一随机事件总线**，把断裂的链路重新接通：
触发源 → 总线排队 → VisUI 弹窗 → 选项执行 → 未完成动作弹"功能尚在制作中"。

---

## 2. 系统全链路解剖

### 2.1 数据层：事件定义在哪里

全部游戏内容打包在 `android/assets/data/dungeon.zip` → `dungeon.xml`（单文件）。
事件相关节点统计（全库）：

| 节点 | 数量 | 说明 |
| --- | --- | --- |
| `door` | 349 | 门（开/关/锁定状态机） |
| `stair` | 112 | 楼梯（大部分是可见楼梯，直接 onTeamEnter 换层） |
| `wallswitch` | 76 | 墙上机关（拉杆/按钮，联动 target） |
| `alcove` | 50 | 壁龛（放置物品触发） |
| `pit` | 42 | 陷阱坑 |
| `eventsquare` | 24 | **随机事件方块（本文主角）** |
| `teleporter` | 18 | 传送格 |
| `forcefield` | 16 | 力场 |
| `pressureplate` | 14 | 压力板（6 种条件脚本） |

其中带配图、会弹窗的 eventsquare 只有 **2 个**：

1. **发现隐藏楼梯**（Forest 迷宫，Illusion 类型格子）——就是用户截图里触发的那一个：

```xml
<square>
    <type value="Illusion" />
    <eventsquare>
        <remaining value="-1" />          <!-- -1 = 无限次 -->
        <intelligence value="0" />        <!-- 感知难度：意志豁免 > 0 即触发 -->
        <displayborder value="True" />
        <text>Behind the brush you discover a hidden stairway.&lt;br /&gt;
Do you wish to go down the stairs ?</text>
        <picturename value="forest-hidden_stairs.png" />
        <choice name="Yes">
            <actions>
                <Teleport>
                    <target maze="Catacomb - 01" x="8" y="2" direction="West" ... />
                </Teleport>
                <EndDialog />
            </actions>
        </choice>
        <choice name="No">
            <actions>
                <Teleport> <target maze="Forest" x="10" y="12" .../> </Teleport>
                <EndDialog />
            </actions>
        </choice>
    </eventsquare>
</square>
```

2. **Darkmoon 入口**（"Do you wish to enter Darkmoon ?" + `darkmoon.png`）。

其余 22 个 eventsquare 只配了 `message`（英雄喊话）或空 text，不弹窗。

> 注意：`text` 节点用 `getText()` 读取（节点内文本），`message` 是 `value` 属性——
> 这是两套读取方式，解析代码 `EventSquare.load()` 已正确区分。
> 正文里 `&lt;br /&gt;` 经 XmlReader 反转义后变成 `<br />`，展示层需转回 `\n`（已在 ScriptedDialog 处理）。

### 2.2 触发层：事件如何被踩到

```
玩家输入（键盘 W / 屏幕方向钮 / 撞墙反馈链路见 doc-14）
  └─ Team.walk(front, strafe)                 [Team.java]
       └─ Team.move(offset)
            ├─ canMove()                       移动冷却 150ms，冷却中直接 false（不反馈）
            ├─ dst.isBlocking() / getMonsterCount()>0
            │    └─ 阻挡 → GameMessage.buildMessage(1) + Feedback.moveBlocked() → return false
            ├─ square.onTeamLeave()            离开旧格
            ├─ 坐标更新、lastMove/hasMoved
            └─ square.onTeamEnter()            进入新格
                 └─ Square.onTeamEnter() → actor.onTeamEnter()   [Square.java:462]
```

`SquareActor.onTeamEnter()` 的 8 个实现（每个都是一种"事件源"）：

| Actor | onTeamEnter 行为 | 状态 |
| --- | --- | --- |
| `EventSquare` | 意志豁免判定 → 消息/弹窗 | **本轮接入总线** |
| `Stair` | 换层 teleport | 可用 |
| `Teleporter` | 传送 | 可用 |
| `PressurePlate` | 6 种条件跑 PressurePlateScript | 脚本可跑，动作受动作层完成度限制 |
| `ForceField` | 挡路/穿越判定 | 可用 |
| `Door` | 开门/关门 | 可用 |
| `Pit` | 掉坑伤害 | 可用（伤害走 Hero.damage） |
| `WallSwitch` / `AlcoveActor` / `Launcher` / `Generator` | 交互/发射/生成 | 依赖动作层 |

EventSquare 的触发判定保留了原版规则：**队伍中任一英雄意志豁免 roll > intelligence 才感知到事件**；
没人感知则 return false，`remaining` 不消耗，下次进格再判。

### 2.3 动作层：21 个动作的真实完成度

选项（`ScriptChoice`）点击后依次执行 `ActionBase.run()`。逐个核对源码的结论：

| 动作 | run() 状态 | 说明 |
| --- | --- | --- |
| `Teleport` | **可用** | 队伍 teleport 到 target，支持 changeDirection |
| `SpawnMonster` | **可用*** | 依赖 Monster 资源加载（Resources.createAsset）；怪物 AI/战斗未完成 |
| `GiveExperience` | **可用** | 队伍平分经验 |
| `DisplayMessage` | **可用**（旁路） | 消息写入 GameMessage（死代码）；总线拦截后走 toast 显示 |
| `ActivateTarget` / `DeactivateTarget` | **可用** | 激活/停用目标格 actor |
| `EnableTarget` / `DisableTarget` / `ToggleTarget` | **可用** | 启用/禁用/翻转目标 actor |
| `EndDialog` | 可用（**已修 bug**） | 关闭事件弹窗；旧代码 target==null 直接 return false，而数据里 EndDialog 从不带 target → 弹窗永不关闭（§3.3） |
| `EndChoice` | 空壳 | 总线特判：语义为结束分支 → 关闭当前弹窗 |
| `GiveItem` | 空壳 | 物品系统未完成 → 弹"功能尚在制作中" |
| `Healing` | 空壳 | 同上 |
| `PlaySound` | 空壳 | 音频未接入 → 同上 |
| `JoinCharacter` | 空壳 | NPC 加入队伍未实现 → 同上 |
| `ChangePicture` / `ChangeText` | 空壳 | 修改弹窗内容（脚本对话用）→ 同上 |
| `EnableChoice` / `DisableChoice` | 空壳 | 动态启停选项 → 同上 |
| `SetTo` | 仅注册 | ScriptBase 可加载但 run 未验证 |

\* SpawnMonster 标记"可用"指代码完整，但生成出来的怪物没有战斗 AI（见 doc-06 路线图），
实际效果等于"凭空站着一只怪物"。

### 2.4 展示层：为什么弹窗从未显示过（本次修复的核心）

旧版链路在两个环节同时断裂：

1. **GameScreen 只存不画**。`EventSquare.onTeamEnter()` 里
   `getGameScreen().setDialog(new ScriptedDialog(...))` 只把弹窗存进字段；
   GameScreen 的 update()/draw()（继承 GameScreenBase，走 scene2d
   `stage.act()+stage.draw()`）**没有任何一行代码调用 dialog.update()/dialog.draw()**。
2. **ScriptedDialog 的绘制方法是错误签名**。`DialogBase extends Dialog`（scene2d Actor），
   但 override 写的是 `draw(SpriteBatch batch, float parentAlpha)`——scene2d 的绘制入口是
   `draw(Batch batch, float parentAlpha)`（`Batch` 是 `SpriteBatch` 的父接口）。
   **参数类型不同 = 重载而非覆盖**，即便有人调 stage 渲染也不会走到这段代码；
   `update(float)` 同样从未被调用。按钮用的 `GUIScriptButton` 是自绘伪按钮
   （非 Actor，`addListener` 收不到任何输入事件）。

这是原版 C#（XNA 即时渲染管线）移植到 scene2d 时遗留的架构断层：
C# 版 GameScreen.Draw() 每帧手绘 dialog，Java 版渲染改成 stage 之后这段被丢了。

**修复方案**：ScriptedDialog 整体重写为 **VisDialog 子类**（不保留旧渲染代码）：
内容（配图 + 正文）进 `getContentTable()`，选项转成 `button(name, choice)`，
点选走 scene2d Dialog 的 `result(Object)` 回调 → `RandomEventBus.runChoice()`。
GameScreen 的 `dialog` 字段与 `getDialog().exit()` 链路保留（EndDialog 动作依赖）。

---

## 3. 排查中发现的全部问题清单

| # | 问题 | 严重度 | 状态 |
| --- | --- | --- | --- |
| 3.1 | ScriptedDialog 从不被渲染（§2.4） | 致命 | ✅ 本轮修复（VisDialog 重写） |
| 3.2 | GameMessage.draw()/update() 零调用，消息系统是死代码 | 致命 | ⚠️ 旁路（DisplayMessage/事件 message 改走 toast；GameMessage 保留原样） |
| 3.3 | EndDialog.run() 的 `target==null` 检查使弹窗永不退出 | 致命 | ✅ 本轮修复（防御式获取 dialog） |
| 3.4 | MazeZone 的 7 个区域脚本字段（onteamenter 等）从未被执行 | 高 | 📌 未修（见 §6 路线） |
| 3.5 | 12 个脚本动作 run() 空壳 | 高 | ✅ 总线统一弹"功能尚在制作中" |
| 3.6 | EventSquare 配图路径缺少 `data/` 前缀（用户上轮已修） | 高 | ✅ 已修（8c189b3） |
| 3.7 | GUIScriptButton 非 Actor、监听器永远收不到点击 | 高 | ✅ 随 VisDialog 重写一并消亡 |
| 3.8 | GameTime stub 的 getElapsedGameTime() 恒为 0，即使接入 GameMessage.update 消息也永不过期 | 中 | 📌 未修（旁路已绕开） |

---

## 4. RandomEventBus 统一随机事件总线

### 4.1 架构

```
触发源（任意多个）                     展示层（唯一）
┌──────────────────────┐    post()   ┌─────────────────────────────────┐
│ EventSquare          │──────────▶  │        RandomEventBus           │
│ MazeZone 脚本(未来)   │             │  queue: Deque<RandomEvent>      │
│ 怪物触发器(未来)      │             │  stage: GameScreen 主 stage     │
│ PressurePlate(可选)  │             │  showing: 一次只弹一个          │
└──────────────────────┘             └───────┬─────────────────────────┘
                                             │ showNext()
                                             ▼
                                  ┌─────────────────────────┐
                                  │ ScriptedDialog(VisDialog)│
                                  │  配图 + 正文 + 选项按钮   │
                                  └───────┬─────────────────┘
                              result(choice)│
                                            ▼
                                  RandomEventBus.runChoice()
                                  ├─ 白名单动作 → action.run()
                                  ├─ DisplayMessage → Feedback.message() (toast)
                                  ├─ EndChoice → 关闭当前弹窗
                                  └─ 其余 → notImplemented() 弹窗
                                            「该功能尚在制作中」
```

### 4.2 公开 API

```java
RandomEventBus.bind(Stage)          // GameScreen.initScreenObjects() 时绑定
RandomEventBus.unbind()             // unloadContent() 时解绑并清队列
RandomEventBus.post(RandomEvent)    // 唯一提交入口；无弹窗在展示时立即弹出
RandomEventBus.runChoice(ScriptChoice)  // 执行选项动作（白名单 + 未实现提示）
RandomEventBus.notImplemented()     // 弹「该功能尚在制作中」VisDialog
RandomEventBus.onDialogClosed()     // ScriptedDialog.hide() 回调，驱动队列
```

提交侧只需要构造一个 `RandomEvent`（source/text/pictureName/choices 四个字段）并 post，
**不再需要知道弹窗怎么画、stage 是谁、选项怎么执行**——这就是"统一总线"的意义。

### 4.3 关键时序：选项执行与弹窗切换不冲突

以"发现隐藏楼梯 → 点 Yes"为例：

1. 玩家点 `Yes` → scene2d Dialog 触发 `result(choice)` → `runChoice()`
2. `Teleport.run()` → `Team.teleport()` → 新格 `onTeamEnter()` →（若新格也有事件）
   `post(新事件)`——此时旧弹窗尚未 hide，`showing==true`，新事件**只入队不弹窗** ✅
3. `EndDialog.run()` → `getDialog().exit()` → `hide()`
4. `hide(Action)` 回调 `onDialogClosed()` → `showing=false` → `showNext()` 弹出队列中的新事件 ✅

递归触发、连环事件、传送落点事件全部自然有序，不会弹窗互相覆盖。

### 4.4 白名单机制（"已完成拿过来用，没完成的弹提示"）

`IMPLEMENTED_ACTIONS`（字符串集合，按类 simpleName 匹配）：

```
Teleport / SpawnMonster / GiveExperience / DisplayMessage /
ActivateTarget / DeactivateTarget / EnableTarget / DisableTarget /
ToggleTarget / EndDialog
```

- 在名单内 → 直接 `action.run()`（异常吞掉记日志，不让单个动作卡死弹窗链）；
- `EndChoice` 特判：关闭当前弹窗（原 run() 是空壳，按语义补齐）；
- 名单外 → `notImplemented()` 弹 **VisUI 模态提示窗**「该功能尚在制作中，敬请期待后续版本。」，
  玩家点"确定"后事件弹窗仍在，可继续选择其他选项。

字符串表新增 `events` 段（三语）：id1=制作中提示、id2=弹窗标题"事件"、id3="确定"。

---

## 5. 本轮改动清单与测试指南

### 5.1 改动文件

| 文件 | 改动 |
| --- | --- |
| `engine/event/RandomEvent.java` | **新增**：事件封装（source/text/pictureName/choices） |
| `engine/event/RandomEventBus.java` | **新增**：统一总线（队列/白名单/未实现提示） |
| `engine/feedback/Feedback.java` | **新增**：非模态反馈静态入口（撞墙/短消息） |
| `engine/feedback/NotificationToast.java` | **新增**：toast 横幅（淡入-停留-淡出） |
| `engine/ScriptedDialog.java` | **重写**：DialogBase 手绘版 → VisDialog 版 |
| `engine/actor/EventSquare.java` | onTeamEnter 改走总线；message 走 toast；新增 getEventSource() |
| `engine/Team.java` | move() 阻挡分支增加 `Feedback.moveBlocked()`（见 doc-14） |
| `engine/script/actions/EndDialog.java` | 修复 target==null 提前返回 bug |
| `GameScreen.java` | 绑定/解绑总线；挂 toast；注册 Feedback.Listener（含迷宫震动） |
| `android+desktop StringTable.xml` | 新增 events 三语字符串段 |

### 5.2 编译验证

ECJ 3.37.0 + gdx-1.14.2 + vis-ui-1.5.9 + gdx-freetype-1.14.2 全量编译：
错误总数 175，**全部为预存在的 `org.ntk.commons.StringUtils` 缺失**
（用户机器 libs/ntk-commons-*.jar 提供，Gradle 构建不受影响）；
本轮新增/修改文件 0 错误。

### 5.3 手工测试步骤

1. `desktop` 启动，载入存档或新开局（Forest）。
2. 走到 Forest (10,13) 附近的 Illusion 格（触发"发现隐藏楼梯"）：
   - 预期：弹出 VisUI 事件窗，标题"事件"，配图 forest-hidden_stairs.png，
     正文两行英文（"Behind the brush..."），下方 `Yes` / `No` 两个按钮。
   - 点 `Yes`：传送到 Catacomb - 01 (8,2)。
   - 点 `No`：传送到 Forest (10,12)（原版行为：No 也传送，把队伍从幻墙格挪出来）。
3. 撞墙（对着墙按 W）：红色横幅"那个方向走不通！"显示约 1.5 秒 + 迷宫画面轻微震动。
4. 任何未完成动作的事件（后续补充内容）会弹「该功能尚在制作中……」。

### 5.4 回归风险

- `ScriptedDialog` 构造签名从 `(Square, EventSquare)` 改为 `(RandomEvent)`——
  全库检索确认无外部调用者（只有 EventSquare 与新总线使用）。
- `DialogBase` 成为无子类的遗留类，保留未删（避免牵动 DialogBase(String, Skin) 二参构造的潜在引用）。
- `GameScreen.getDialog().exit()` 链路不变，EndDialog 动作照常工作。

---

## 6. 后续路线（把随机事件系统做完）

1. **MazeZone 区域脚本接线**：Team.move/onTeamEnter 时检查所在 zone 的
   onTeamEnterScript 等字段，经 RandomEventBus 触发（总线已就位，只差触发点）。
2. **GameMessage 正式接入**：GameScreen.update() 调 update(真实 delta)，
   GameScreen.draw() 调 draw(batch)（坐标需从 640x400 旧坐标系映射到 1280x720 stage）。
   完成后 DisplayMessage 可切回原链路。
3. **空壳动作逐个补齐**：优先级 GiveItem > Healing > PlaySound > EnableChoice/DisableChoice
   > ChangePicture/ChangeText > JoinCharacter。每补一个就把它加入总线白名单。
4. **SpawnMonster 联动战斗系统**：战斗完成后事件才能完整（怪物遭遇、伏击）。
5. **随机遭遇表**：原版 EOB2 的"随机遇敌"可建模为 MazeZone onUpdate 脚本 + Dice roll，
   走同一个总线弹出遭遇事件。

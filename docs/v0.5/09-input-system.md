# 09. 输入与操控系统模块（Input & Control System）

> **覆盖**：键盘 / 鼠标输入处理、按键绑定、UI 按钮事件、操控配置
> **关键类**：`GameScreenBase`、`NavigationControlsGroup`、`KeyboardInputProcessor`（新）、`KeyBindings`（新）、`KeyConfigWindow`（新）、`Settings`、`InputType`、`InputScheme.xml`（数据文件，未使用）
> **适合谁看**：想理解"按下按键发生了什么"、"怎么改键位"、"怎么加新按键功能"的人

---

## 9.1 历史与现状（v0.5 之前）

darkmoor 项目的输入系统**长期处于残缺状态**：

| 设施 | v0.5 之前的状态 |
|---|---|
| 鼠标点击 scene2d 按钮 | ✅ 工作正常（`MainMenu`、`OptionsWindow`、`NavigationControlsGroup`、`HeroInterfaceGroup`） |
| 鼠标点击 `ScreenButton`（camp 子窗口） | ❌ 不工作（`ScreenButton` 不是 scene2d actor，需手动 `onSelectEntry()` 调用） |
| 键盘事件处理 | ❌ **完全没有**（无 `InputProcessor`、无 `keyDown/keyUp`） |
| 键盘轮询（`Gdx.input.isKeyPressed`） | ⚠️ 仅 `IntroScreen`（ESC/SPACE/LEFT）和 `CharGen`（ESC）用 |
| 按键配置文件 | ⚠️ 有 `InputScheme.xml`（完整 QWERTY/AZERTY 绑定），**但无 Java 解析器** |
| `Settings.inputScheme` 字段 | ❌ 死代码（加载了但 `getInputScheme()` 只被测试调用） |
| 按键配置 UI | ❌ 没有 |

**结论**：v0.5 之前玩家**只能用鼠标点击屏幕上的方向按钮**移动，无法用键盘玩。这是地牢爬行游戏的核心可用性缺陷。

---

## 9.2 v0.5 输入系统架构

### 9.2.1 总体设计

```
                        ┌─────────────────────────────────┐
                        │  Gdx.input (libGDX 输入源)       │
                        └─────────────────────────────────┘
                                       │
                                       ▼
                        ┌─────────────────────────────────┐
                        │  InputMultiplexer                │
                        │  (GameScreenBase.setupStage)     │
                        └─────────────────────────────────┘
                                       │
                  ┌────────────────────┼────────────────────┐
                  │                    │                    │
                  ▼                    ▼                    ▼
    ┌─────────────────────┐  ┌──────────────────┐  ┌──────────────────┐
    │ KeyboardInputProc.  │  │ Stage (scene2d)  │  │ (其他 Processor) │
    │ - keyDown → 动作    │  │ - 按钮点击       │  │ (未来扩展)       │
    │ - 查 KeyBindings    │  │ - ScreenButton   │  │                  │
    └─────────────────────┘  └──────────────────┘  └──────────────────┘
              │
              ▼
    ┌─────────────────────┐
    │   KeyBindings       │
    │   - action → keycode│
    │   - 默认值 (QWERTY) │
    │   - 从 Settings 加载│
    └─────────────────────┘
              │
              ▼
    ┌─────────────────────┐
    │   Team              │
    │   - walk(front,strafe)│
    │   - setDirection()  │
    └─────────────────────┘
```

### 9.2.2 关键设计决策

| 决策 | 选择 | 理由 |
|---|---|---|
| 输入处理模式 | **事件驱动**（`InputProcessor.keyDown`） | 而非轮询（`isKeyPressed`）— 响应及时，不浪费 CPU |
| 多输入源 | **`InputMultiplexer`** | 键盘和 scene2d 按钮都能接收事件 |
| 按键存储 | **`Settings` + `settings.properties`** | 与现有 settings 体系一致；可持久化 |
| 默认布局 | **QWERTY + 方向键** | 兼顾桌面用户习惯；可切换 AZERTY |
| 转向键 | **Q/E（L1/R1 风格）** | 用户要求；与主流 CRPG 一致 |
| 移动键 | **方向键 + WASD** | 双套并存，玩家可选 |

### 9.2.3 默认按键绑定（QWERTY）

| 动作 | 默认键 | 说明 |
|---|---|---|
| `MoveForward` | `W` 或 `↑` | 向前移动一格 |
| `MoveBackward` | `S` 或 `↓` | 向后退一格 |
| `StrafeLeft` | `A` 或 `←` | 向左侧移 |
| `StrafeRight` | `D` 或 `→` | 向右侧移 |
| `TurnLeft` | `Q` | 左转 90°（L1 风格） |
| `TurnRight` | `E` | 右转 90°（R1 风格） |
| `Camp` | `C` | 打开营地对话框 |
| `Inventory` | `I` | 切换到物品栏视图 |
| `Statistics` | `P` | 切换到属性视图（P for Profile） |
| `SelectHero1`~`6` | `1`~`6` | 选中第 N 个角色 |
| `MainMenu` | `ESC` | 打开游戏内设置菜单 |
| `QuickSave` | `F5` | 快速保存到当前槽位 |
| `QuickLoad` | `F9` | 快速读取当前槽位 |
| `ToggleMinimap` | `M` | 切换小地图显示 |
| `ToggleNavButtons` | `N` | 切换方向按钮显示 |

> **设计要点**：同一个动作可以绑定**多个键**（如 `MoveForward` 同时绑 `W` 和 `↑`），`KeyBindings` 内部用 `Map<Integer, String>`（keycode → action）查询，O(1) 复杂度。

---

## 9.3 类与文件清单

### 9.3.1 新增类（v0.5）

#### `core/src/com/ntk/darkmoor/config/KeyBindings.java`

```java
public class KeyBindings {
    public enum Action {
        MoveForward, MoveBackward, StrafeLeft, StrafeRight,
        TurnLeft, TurnRight, Camp, Inventory, Statistics,
        SelectHero1, SelectHero2, SelectHero3, SelectHero4, SelectHero5, SelectHero6,
        MainMenu, QuickSave, QuickLoad, ToggleMinimap, ToggleNavButtons
    }

    private Map<Integer, Action> keyToAction;     // keycode → action
    private Map<Action, List<Integer>> actionToKeys; // action → keycodes（反向查询用）

    public static KeyBindings defaults();           // QWERTY 默认
    public static KeyBindings azerty();             // AZERTY 默认

    public void bind(int keycode, Action action);   // 加绑定（一个 action 可多键）
    public void unbind(int keycode);                // 解绑某键
    public void unbindAction(Action action);        // 解绑某动作所有键
    public Action lookup(int keycode);              // keyDown 时调用
    public List<Integer> keysFor(Action action);    // UI 显示用
    public String keyName(int keycode);             // 把 keycode 转成可读名（"W"、"↑"）
    public void saveTo(Properties props);           // 持久化
    public static KeyBindings loadFrom(Properties props); // 从 settings.properties 加载
}
```

#### `core/src/com/ntk/darkmoor/engine/KeyboardInputProcessor.java`

```java
public class KeyboardInputProcessor implements InputProcessor {
    private final Team team;
    private final KeyBindings bindings;
    private final GameScreen gameScreen;

    public KeyboardInputProcessor(Team team, KeyBindings bindings, GameScreen gameScreen);

    @Override public boolean keyDown(int keycode);  // 主入口：查 bindings → 执行动作
    @Override public boolean keyUp(int keycode);    // 通常 no-op
    @Override public boolean keyTyped(char ch);     // no-op
    // touchDown/moved/scrolled 都返回 false（让 Stage 处理）
}
```

`keyDown` 的工作流：

```
keycode → bindings.lookup(keycode) → switch(action):
  MoveForward → team.walk(0, -1)
  MoveBackward → team.walk(0, 1)
  StrafeLeft → team.walk(-1, 0)
  StrafeRight → team.walk(1, 0)
  TurnLeft → team.getLocation().setDirection(Compass.rotate(dir, Rotate270))
  TurnRight → team.getLocation().setDirection(Compass.rotate(dir, Rotate90))
  Camp → gameScreen.openCampDialog()
  Inventory → gameScreen.setInterfaceType(Inventory)
  Statistics → gameScreen.setInterfaceType(Statistic)
  SelectHeroN → gameScreen.selectHero(N)
  MainMenu → gameScreen.openInGameMenu()
  QuickSave → gameScreen.saveGameSlot(currentSlot)
  QuickLoad → gameScreen.loadGameSlot(currentSlot)
  ToggleMinimap → Settings.setShowMinimap(!showMinimap)
  ToggleNavButtons → Settings.setShowNavButtons(!showNavButtons)
```

#### `core/src/com/ntk/darkmoor/KeyConfigWindow.java`

继承 `BaseWindow`，UI 让玩家重新绑定按键：

```
┌──────────────── 操控设置 ────────────────┐
│                                          │
│  [向前移动]        W / ↑      [修改]     │
│  [向后移动]        S / ↓      [修改]     │
│  [左侧移动]        A / ←      [修改]     │
│  [右侧移动]        D / →      [修改]     │
│  [左转]            Q          [修改]     │
│  [右转]            E          [修改]     │
│  [营地]            C          [修改]     │
│  ...                                     │
│                                          │
│       [恢复默认]      [返回]             │
└──────────────────────────────────────────┘
```

点"修改"后，窗口进入"按下任意键捕获"模式，下一个 `keyDown` 即绑定到该动作。

### 9.3.2 修改的现有类

#### `GameScreenBase.java`

```java
// 新增字段
protected InputProcessor keyboardProcessor;

// setupStage() 修改
protected void setupStage() {
    stage = new Stage(new StretchViewport(...));
    // ... 添加 background 和 buttons ...

    InputMultiplexer mux = new InputMultiplexer();
    if (keyboardProcessor != null) {
        mux.addProcessor(keyboardProcessor);  // 键盘先吃
    }
    mux.addProcessor(stage);                   // Stage 后吃（按钮点击）
    Gdx.input.setInputProcessor(mux);
}
```

#### `GameScreen.java`

```java
// 在 initScreenObjects() 末尾，注册完所有 group 之后：
KeyBindings bindings = KeyBindings.loadFrom(/* Settings.getProperties() */);
if (bindings == null) bindings = KeyBindings.defaults();
keyboardProcessor = new KeyboardInputProcessor(team, bindings, this);
// 因为 setupStage() 已经在父类构造时跑过，需要重新设置 input processor
InputMultiplexer mux = new InputMultiplexer();
mux.addProcessor(keyboardProcessor);
mux.addProcessor(stage);
Gdx.input.setInputProcessor(mux);
```

#### `Settings.java`

新增字段：

```java
private boolean showNavButtons = true;   // 是否显示方向按钮（默认 true，老玩家无感）
// showMinimap 已在 v0.5 minimap 补丁中添加
```

新增 settings.properties 键：

```
ShowNavButtons=true
# 按键绑定（每行一个动作，值为 keycode 列表，逗号分隔）
KeyBindings.MoveForward=51,19      # W, UP
KeyBindings.MoveBackward=47,22     # S, DOWN
KeyBindings.StrafeLeft=29,21       # A, LEFT
KeyBindings.StrafeRight=32,22      # D, RIGHT
KeyBindings.TurnLeft=45            # Q
KeyBindings.TurnRight=33           # E
# ... 其他动作
```

#### `NavigationControlsGroup.java`

- 加 `setVisible(Settings.isShowNavButtons())` 在 `update()` 中
- **新增 CAMP TextButton**（替代背景图里的 CAMP 像素）：使用 `LanguagesManager.getString("titles", 1)` 作为文本
- **新增 Settings TextButton**：点击后弹出游戏内菜单

#### `OptionsWindow.java`

- 加第 7 个按钮"操控"（`titles` id=18）→ 弹出 `KeyConfigWindow`

#### `StringTable.xml`

新增 `controls` stringtable：

```xml
<stringtable name="controls">
    <default name="English" />
    <language name="English">
        <string id="1">Controls</string>
        <string id="2">Move Forward</string>
        <string id="3">Move Backward</string>
        <string id="4">Strafe Left</string>
        <string id="5">Strafe Right</string>
        <string id="6">Turn Left</string>
        <string id="7">Turn Right</string>
        <string id="8">Camp</string>
        <string id="9">Inventory</string>
        <string id="10">Statistics</string>
        <string id="11">Select Hero %d</string>
        <string id="12">Main Menu</string>
        <string id="13">Quick Save</string>
        <string id="14">Quick Load</string>
        <string id="15">Toggle Minimap</string>
        <string id="16">Toggle Nav Buttons</string>
        <string id="17">Press any key to bind...</string>
        <string id="18">Reset to Defaults</string>
        <string id="19">Back</string>
        <string id="20">Change</string>
    </language>
    <language name="Francais">
        <!-- ... 法语 ... -->
    </language>
    <language name="Chinese">
        <string id="1">操控</string>
        <string id="2">向前移动</string>
        <string id="3">向后移动</string>
        <string id="4">左侧移动</string>
        <string id="5">右侧移动</string>
        <string id="6">左转</string>
        <string id="7">右转</string>
        <string id="8">营地</string>
        <string id="9">物品栏</string>
        <string id="10">属性</string>
        <string id="11">选择角色 %d</string>
        <string id="12">主菜单</string>
        <string id="13">快速保存</string>
        <string id="14">快速读取</string>
        <string id="15">切换小地图</string>
        <string id="16">切换方向按钮</string>
        <string id="17">按下任意键以绑定...</string>
        <string id="18">恢复默认</string>
        <string id="19">返回</string>
        <string id="20">修改</string>
    </language>
</stringtable>
```

并在 `titles` stringtable 加 id=18：

```xml
<string id="18">Controls</string>           <!-- English -->
<string id="18">Contrôles</string>          <!-- Francais -->
<string id="18">操控</string>                <!-- Chinese -->
```

---

## 9.4 输入事件流（详细）

### 9.4.1 玩家按 W 键向前移动

```
1. libGDX 检测到 W 按下 → 调用 InputMultiplexer.keyDown(51)  // Keys.W = 51
2. multiplexer 第一个 processor 是 KeyboardInputProcessor
   → keyDown(51)
   → bindings.lookup(51) → Action.MoveForward
   → switch: case MoveForward → team.walk(0, -1)
3. team.walk(0, -1) 内部：
   a. 检查 canMove()（冷却 + 战斗状态）
   b. 计算 destCoord = location.coordinate + direction 偏移
   c. maze.getSquare(destCoord) → 撞墙检查
   d. 通过 → location.coordinate.add(offset)
   e. 触发 onTeamLeave / onTeamEnter
4. 下一帧 GameScreen.update() → mazeGroup.update() → 重画视图
5. KeyboardInputProcessor.keyDown 返回 true → 事件被消费
   → multiplexer 不会传给 stage → 不会误触发按钮点击
```

### 9.4.2 玩家点击屏幕上的"前进"按钮

```
1. libGDX 检测到鼠标按下 → 调用 InputMultiplexer.touchDown(x, y, pointer, button)
2. multiplexer 第一个 processor 是 KeyboardInputProcessor
   → touchDown 返回 false（不消费）
3. multiplexer 把事件传给 stage
4. scene2d 命中测试 → forward Image 的 InputListener
   → touchDown → team.walk(0, -1)
5. 同样的 team.walk 调用，效果一致
```

### 9.4.3 玩家按 ESC 打开主菜单

```
1. libGDX 检测到 ESC → keyDown(111)  // Keys.ESCAPE = 111
2. KeyboardInputProcessor.keyDown(111)
   → bindings.lookup(111) → Action.MainMenu
   → gameScreen.openInGameMenu()
3. openInGameMenu():
   - 创建 InGameMenuWindow（新 BaseWindow 子类）
   - stage.addActor(menuWindow)
   - menuWindow 包含"继续游戏 / 操控 / 选项 / 保存 / 读取 / 退出到主菜单"按钮
4. 返回 true → 事件消费
```

---

## 9.5 按键配置的持久化

### 9.5.1 存储

按键绑定保存在 `settings.properties`，每个动作一行：

```properties
# KeyBindings: action = comma-separated keycodes
# Keycodes are libGDX Input.Keys constants (see https://libgdx.com/wiki/input/input)
KeyBindings.MoveForward=51,19
KeyBindings.MoveBackward=47,22
KeyBindings.StrafeLeft=29,21
KeyBindings.StrafeRight=32,22
KeyBindings.TurnLeft=45
KeyBindings.TurnRight=33
KeyBindings.Camp=35
KeyBindings.Inventory=37
KeyBindings.Statistics=44
KeyBindings.SelectHero1=8
KeyBindings.SelectHero2=9
KeyBindings.SelectHero3=10
KeyBindings.SelectHero4=11
KeyBindings.SelectHero5=12
KeyBindings.SelectHero6=13
KeyBindings.MainMenu=111
KeyBindings.QuickSave=63    # F5
KeyBindings.QuickLoad=67    # F9
KeyBindings.ToggleMinimap=41 # M
KeyBindings.ToggleNavButtons=42 # N
```

### 9.5.2 加载

```java
public static KeyBindings loadFrom(Properties props) {
    KeyBindings kb = new KeyBindings();
    for (Action action : Action.values()) {
        String key = "KeyBindings." + action.name();
        String value = props.getProperty(key);
        if (value == null || value.isEmpty()) continue;
        for (String code : value.split(",")) {
            try {
                int keycode = Integer.parseInt(code.trim());
                kb.bind(keycode, action);
            } catch (NumberFormatException e) { /* skip */ }
        }
    }
    // 如果完全空，返回 defaults
    if (kb.isEmpty()) return defaults();
    return kb;
}
```

### 9.5.3 保存

```java
public void saveTo(Properties props) {
    for (Action action : Action.values()) {
        List<Integer> keys = actionToKeys.get(action);
        if (keys == null || keys.isEmpty()) {
            props.remove("KeyBindings." + action.name());
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(keys.get(i));
            }
            props.setProperty("KeyBindings." + action.name(), sb.toString());
        }
    }
}
```

---

## 9.6 已知遗留问题与未来扩展

| 问题 | 影响 | 解决思路 |
|---|---|---|
| `InputScheme.xml` 仍未被使用 | 文件存在但无 Java 解析器 | 本补丁用 `settings.properties` 替代，未来可写 `InputScheme.java` 解析 XML |
| `ScreenButton` 不响应点击 | 营地子窗口按钮不可点 | 需把 `ScreenButton` 改为继承 `TextButton` 或在 `BaseWindow.update()` 加轮询 |
| 键盘重复触发 | 长按 W 会持续触发 `keyDown` | libGDX 默认每帧检查，可在 `keyDown` 加 `lastTriggerTime` 节流（如 200ms） |
| 手柄支持 | 无 | libGDX 有 `Controllers` API，未来可加 |
| 鼠标拖拽 | 无 | 物品拖拽需自定义 `DragListener` |
| 输入方案切换 UI | `KeyConfigWindow` 只能逐键改 | 可加"切换到 AZERTY 默认"按钮（已有 `KeyBindings.azerty()`） |

---

## 9.7 测试清单

| 测试项 | 期望结果 |
|---|---|
| 进入游戏后按 W | 队伍向前移动一格 |
| 按 ↑ | 同上（双键绑定） |
| 按 Q | 队伍左转 90° |
| 按 E | 队伍右转 90° |
| 按 C | 打开营地对话框 |
| 按 ESC | 打开游戏内菜单 |
| 按 M | 小地图切换显示/隐藏 |
| 按 N | 方向按钮切换显示/隐藏 |
| 在 OptionsWindow 点"操控" | 弹出 KeyConfigWindow |
| 在 KeyConfigWindow 点某动作的"修改" | 显示"按下任意键..."提示 |
| 按下任意键 | 该动作绑定到此键，UI 更新 |
| 点"恢复默认" | 所有绑定回到 QWERTY 默认 |
| 退出游戏重启 | 自定义绑定保留 |
| 同时按 W 和 D | 队伍向前移动（不侧移）— 因为 `walk()` 是顺序执行的 |
| 鼠标点击方向按钮 | 仍然可用（multiplexer 同时支持） |

---

## 9.8 相关文档

- [10-camp-system.md](10-camp-system.md) — CAMP 按钮与营地对话框系统
- [modules/06-engine-graphics.md](modules/06-engine-graphics.md) — `NavigationControlsGroup` 等图形组
- [modules/02-config.md](modules/02-config.md) — `Settings` / `InputType` 配置类

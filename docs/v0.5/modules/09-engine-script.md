# 模块 09 - `com.ntk.darkmoor.engine.script`（脚本系统）

## 概览

XML 驱动的事件系统——所有"触发 X 后发生 Y"的游戏逻辑都通过它实现。这是项目里**最容易被新手忽略但最重要**的一块。

按子包分：

| 子包 | 类 | 角色 |
|---|---|---|
| `script` | `ScriptBase` | 脚本基类 |
| `script` | `AlcoveScript` / `PressurePlateScript` / `WallSwitchScript` | 触发源脚本 |
| `script.actions` | `ActionBase` + 20+ 个子类 | 具体动作 |
| `script.gui` | `GUIScriptButton` / `ScriptChoice` | 脚本对话框 UI |

## 设计思路

游戏里有大量"触发器 → 动作"的逻辑，例如：
- 按下墙上的按钮（触发器）→ 打开门（动作）
- 踏入事件格（触发器）→ 显示对话（动作）→ 给玩家物品（动作）→ 召唤怪物（动作）
- 施法（触发器）→ 治疗（动作）→ 显示消息（动作）

如果硬编码，每种触发器和动作的组合都需要专门代码。脚本系统的设计是：
- **触发器**（`WallSwitch`、`PressurePlate` 等）只负责"检测到自己被触发"
- **脚本**（`ScriptBase` 子类）维护一个动作列表，被触发时按顺序 `run()`
- **动作**（`ActionBase` 子类）执行具体逻辑
- **数据**（`Script.xml`）描述"哪个触发器对应哪个脚本，脚本里有哪些动作"

## ScriptBase

**位置**：`core/src/com/ntk/darkmoor/engine/script/ScriptBase.java`

**职责**：脚本基类，维护一个动作列表。

**字段**：
```java
private ActionBase action;        // 动作链表头
private Dungeon dungeon;
```

**主要方法**：
```java
public boolean run() {
    if (action != null) {
        return action.run();   // 执行第一个动作
    }
    return false;
}

public boolean load(Element xml) {
    // 从 XML 加载动作链
    // 每个 <action> 子元素实例化对应 ActionBase 子类
}

public void dispose()
```

**子类**：

| 类 | 触发源 |
|---|---|
| `AlcoveScript` | 玩家在壁龛里放/取物品 |
| `PressurePlateScript` | 玩家踩到踏板 |
| `WallSwitchScript` | 玩家点击墙上开关 |

## ActionBase

**位置**：`core/src/com/ntk/darkmoor/engine/script/actions/ActionBase.java`

**职责**：动作基类。

**字段**：
```java
protected DungeonLocation target;    // 动作目标位置
protected String name;
```

**主要方法**：
```java
public boolean run() { return false; }   // 子类实现
public boolean load(Element node)
public void dispose()
```

## 20+ 个具体动作

按功能分类：

### 启停对象

| 类 | 作用 |
|---|---|
| `ActivateTarget` | 激活目标（如开门、开启传送点） |
| `DeactivateTarget` | 关闭目标 |
| `ToggleTarget` | 切换目标状态 |
| `EnableTarget` | 启用目标 |
| `DisableTarget` | 禁用目标 |

### 显示信息

| 类 | 作用 |
|---|---|
| `DisplayMessage` | 显示游戏消息 |
| `ChangePicture` | 改变对话框图片 |
| `ChangeText` | 改变对话框文本 |

### 给玩家东西

| 类 | 作用 |
|---|---|
| `GiveItem` | 给玩家物品 |
| `GiveExperience` | 给玩家经验值 |
| `Healing` | 治疗玩家 |
| `JoinCharacter` | NPC 加入队伍 |

### 移动 / 召唤

| 类 | 作用 |
|---|---|
| `Teleport` | 传送玩家到指定位置 |
| `SpawnMonster` | 召唤怪物 |

### 对话控制

| 类 | 作用 |
|---|---|
| `EnableChoice` | 启用对话选项 |
| `DisableChoice` | 禁用对话选项 |
| `EndChoice` | 结束选项 |
| `EndDialog` | 结束整个对话 |

### 其他

| 类 | 作用 |
|---|---|
| `PlaySound` | 播放音效 |
| `SetTo` | 设置状态（通用 setter） |

## XML 数据格式

**位置**：`android/assets/data/Script.xml`

**示例**：
```xml
<scripts>
    <!-- 法术：Cure Minor Wounds -->
    <script script="Spells" interface="CureMinorWounds">
        <action name="Healing" target="...">
            <param name="amount" value="1d4+1" />
        </action>
        <action name="DisplayMessage" target="...">
            <param name="text" value="You feel slightly better." />
        </action>
    </script>
    
    <!-- 墙按钮：开秘密门 -->
    <script script="WallSwitch" interface="secretDoorButton">
        <action name="ActivateTarget" target="Temple/maze1/5,3/NORTH" />
        <action name="DisplayMessage" target="...">
            <param name="text" value="A door opens somewhere." />
        </action>
    </script>
    
    <!-- 对话：NPC 加入 -->
    <script script="Dialog" interface="joinNPC">
        <action name="JoinCharacter" target="Hero:Valanau" />
        <action name="EndDialog" />
    </script>
</scripts>
```

**`script` 属性**对应 `ScriptBase` 子类名，决定触发源类型。
**`interface` 属性**是脚本的唯一标识，触发器通过它引用脚本。
**`<action>` 元素**按顺序执行，`name` 属性对应 `ActionBase` 子类名。

## ScriptBase 如何找到 ActionBase 子类

`ScriptBase.load(Element)` 用反射或 if-else 链把 XML 里的 `name` 属性映射到具体类。看 `ScriptBase.java` 顶部的 import 列表，可以看到所有 20+ 个 action 子类都被 import 进来了，load 时按 name 字符串实例化对应类。

## script.gui 子包

### GUIScriptButton

**位置**：`core/src/com/ntk/darkmoor/engine/script/gui/GUIScriptButton.java`

**职责**：脚本对话框里的按钮。

**v0.5 修复点**：原来用 `BitmapFont.TextBounds` + `getBounds()` 计算文本尺寸，已改成 `GlyphLayout`。

**修复前**：
```java
TextBounds textsize = GUI.getDialogFont().getBounds(text);
Vector2 pos = new Vector2(
    rectangle.x + (rectangle.width - textsize.width) / 2,
    rectangle.y + (rectangle.height - textsize.height) / 2
);
```

**修复后**：
```java
GlyphLayout layout = new GlyphLayout();
layout.setText(GUI.getDialogFont(), text);
Vector2 pos = new Vector2(
    rectangle.x + (rectangle.width - layout.width) / 2,
    rectangle.y + (rectangle.height - layout.height) / 2
);
```

### ScriptChoice

**职责**：脚本对话框里的选项（一个选项对应一个 `GUIScriptButton` + 后续动作链）。

## 数据流示例：玩家踩踏板触发陷阱

```
1. 玩家移动到 (5, 3) 格子
2. Square.OnTeamEnter 事件触发
3. Square 检查自己的 actors，发现 PressurePlate
4. PressurePlate.run() → 调用 PressurePlateScript.run()
5. PressurePlateScript 持有动作链：
   [DisplayMessage("Trap!"), SpawnMonster("Goblin"), ActivateTarget(door)]
6. 按顺序执行：
   a. DisplayMessage.run() → 在屏幕显示"Trap!"
   b. SpawnMonster.run() → 在 (5, 4) 创建 Goblin
   c. ActivateTarget.run() → 打开 (5, 3) 北边的门
7. 玩家看到消息、遇到怪物、门打开
```

## v0.5 状态

- **数据加载**：完整可用（Script.xml 能正确解析）
- **ScriptBase / ActionBase 骨架**：完整
- **具体动作实现**：
  - `DisplayMessage` / `PlaySound`：✅ 完成
  - `ActivateTarget` / `DeactivateTarget` / `ToggleTarget`：✅ 完成
  - `Teleport`：✅ 完成
  - `SpawnMonster`：⚠️ 部分（怪物能生成，但 AI 未实现）
  - `GiveItem` / `GiveExperience` / `Healing`：⚠️ 部分
  - `JoinCharacter`：⚠️ 部分
  - `EndDialog` / `EndChoice` / 对话相关：⚠️ 部分
- **GUIScriptButton**：✅ 完成（v0.5 已修复 TextBounds → GlyphLayout）

## 学习建议

1. 先读 `ActionBase.java`（基类，最短）
2. 再读几个简单 action：`DisplayMessage.java`、`PlaySound.java`、`Teleport.java`
3. 再读 `ScriptBase.java`（理解加载和 run 流程）
4. 再读一个具体 ScriptBase 子类：`WallSwitchScript.java`
5. 再读 `GUIScriptButton.java`（理解脚本对话框 UI，注意 v0.5 修复）
6. 想加新动作？继承 `ActionBase`，实现 `run()` 和 `load()`，然后在 `ScriptBase.load()` 里注册

## 想加新动作？

以加一个"Give Gold"动作为例：

1. 创建 `core/src/com/ntk/darkmoor/engine/script/actions/GiveGold.java`：
```java
public class GiveGold extends ActionBase {
    private int amount;
    
    @Override
    public boolean run() {
        Team team = GameScreen.getTeam();
        // team.addGold(amount);
        return true;
    }
    
    @Override
    public boolean load(Element node) {
        if (!super.load(node)) return false;
        amount = Integer.parseInt(node.getAttribute("amount", "0"));
        return true;
    }
}
```

2. 在 `ScriptBase.java` 的 `load` 方法里加：
```java
if ("GiveGold".equalsIgnoreCase(node.getName())) {
    action = new GiveGold();
}
```

3. 在 `Script.xml` 里使用：
```xml
<script script="WallSwitch" interface="goldButton">
    <action name="GiveGold" amount="100" />
    <action name="DisplayMessage" text="You found 100 gold!" />
</script>
```

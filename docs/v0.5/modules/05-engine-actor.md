# 模块 05 - `com.ntk.darkmoor.engine.actor`

## 概览

每个 `Square` 格子里可以包含若干 `SquareActor`——可触发互动的对象。这是"地牢里能被点击/踩到/触碰的东西"的统一抽象。

| 类 | 文件 | 角色 |
|---|---|---|
| `SquareActor` | `SquareActor.java` | actor 基类 |
| `Door` | `Door.java` | 门 |
| `Stair` | `Stair.java` | 楼梯 |
| `PressurePlate` | `PressurePlate.java` | 踩踏板 |
| `WallSwitch` | `WallSwitch.java` | 墙上开关 |
| `Teleporter` | `Teleporter.java` | 传送点 |
| `Pit` | `Pit.java` | 陷阱坑 |
| `ForceField` | `ForceField.java` | 力场 |
| `AlcoveActor` | `AlcoveActor.java` | 壁龛 |
| `EventSquare` | `EventSquare.java` | 事件格 |
| `Generator` | `Generator.java` | 怪物生成器 |
| `Launcher` | `Launcher.java` | 投射物发射器 |

## SquareActor（基类）

**位置**：`core/src/com/ntk/darkmoor/engine/actor/SquareActor.java`

**职责**：所有 actor 的基类。

**字段**：
```java
protected Square square;             // 所在格子
protected boolean activated;          // 是否激活
protected boolean visible;            // 是否可见
```

**主要方法**：
```java
public abstract boolean update(float delta);  // 子类实现
public void activate()
public void deactivate()
public boolean isActivated()
public void setVisible(boolean v)
```

**子类要做的**：
1. 实现 `update(float delta)` 处理状态变化
2. 实现 `draw(Batch, ViewField, Vector2)` 绘制自己
3. 实现 `load(Element)` / `save(XmlWriter)` 序列化

## 各 actor 详解

### Door

**职责**：门，可开关、可上锁。

**状态**：
```java
private boolean open;
private boolean locked;
private int keyId;   // 开锁所需的钥匙 id（0 = 不需要钥匙）
```

**触发**：玩家点击门 → 尝试打开（如果上锁且没钥匙则失败）。

### Stair

**职责**：楼梯，连接不同 maze。

**字段**：
```java
private String targetMaze;            // 目标楼层
private Vector2 targetCoordinate;     // 目标坐标
private CardinalPoint targetDirection; // 目标朝向
```

**触发**：玩家点击楼梯 → `team.setLocation(new DungeonLocation(target...))`。

### PressurePlate

**职责**：踩踏板。玩家踏入格子时触发。

**触发**：`Square.OnTeamEnter` 事件 → 调用关联脚本。

### WallSwitch

**职责**：墙上的按钮。玩家点击时触发。

**触发**：`Square.OnClick` → 调用关联脚本。

### Teleporter

**职责**：传送点。玩家踏入时传送到另一位置。

**字段**：跟 `Stair` 类似（目标 maze + 坐标 + 朝向）。

### Pit

**职责**：陷阱坑。玩家踏入时受伤。

**字段**：
```java
private int damage;       // 伤害值
private boolean visible;   // 是否可见（隐藏的 pit 才是真正的陷阱）
```

### ForceField

**职责**：力场，阻挡通行但可被关闭。

**典型用途**：被脚本控制开/关。例如"按下墙按钮 → ForceField 关闭 → 玩家可通过"。

### AlcoveActor

**职责**：壁龛，可以放物品。

**字段**：
```java
private Item item;   // 当前放的物品（可为 null）
```

**触发**：玩家点击 → 拿走或放回物品。

### EventSquare

**职责**：事件格，玩家踏入时触发脚本（无可见外观）。

**典型用途**：进入大房间时触发剧情对话、踩到暗格触发陷阱。

### Generator

**职责**：怪物生成器，按时间间隔召唤怪物。

**字段**：
```java
private String monsterName;     // 要召唤的怪物
private float interval;          // 间隔（秒）
private int maxMonsters;         // 最多同时存在几个
private float timer;
```

### Launcher

**职责**：投射物发射器（箭矢陷阱、火球陷阱等）。

**字段**：
```java
private int damage;
private ThrownItem projectile;
```

## actor 与脚本的关系

每个 actor 都可以关联一个 `ScriptBase` 子类：

| Actor | 对应 Script |
|---|---|
| `WallSwitch` | `WallSwitchScript` |
| `PressurePlate` | `PressurePlateScript` |
| `AlcoveActor` | `AlcoveScript` |
| `EventSquare` | （通用 `ScriptBase`） |

当 actor 被触发时，调用 `script.run()`，脚本会按顺序执行一串 `ActionBase`（详见 [09-engine-script.md](09-engine-script.md)）。

## XML 加载

actor 从 `MazeElements.xml` 或地牢 XML 加载。示例：

```xml
<square x="5" y="3">
    <door open="false" locked="true" keyId="42" />
    <wallswitch script="secretDoorSwitch" />
</square>
```

加载时 `Square.load(Element)` 会扫描子元素，根据 tag 名实例化对应 `SquareActor` 子类。

## v0.5 状态

- 所有 actor 类的**数据模型已完整**（字段、加载、保存）
- 大部分 actor 的**绘制已实现**（能在地牢里看到）
- 触发逻辑（`activate` / `deactivate`）已实现
- 但**关联的脚本执行**部分依赖 `ScriptBase.run()`，部分脚本动作未实现（详见 09）

## 学习建议

1. 先读 `SquareActor.java`（基类，最短）
2. 再读 `Door.java`（最常见，逻辑清晰）
3. 再读 `Stair.java`（理解跨楼层移动）
4. 然后看 `PressurePlate.java` + `WallSwitch.java`（理解触发器模式）
5. 最后看 `Generator.java` + `Launcher.java`（更复杂的）

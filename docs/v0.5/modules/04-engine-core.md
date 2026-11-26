# 模块 04 - `com.ntk.darkmoor.engine`（核心包）

## 概览

这是项目**最大也最重要**的包，包含所有游戏数据模型和核心逻辑。占了项目代码量的 ~40%。

按功能可以分为 6 组：

1. **地牢结构**：`Dungeon` / `Maze` / `Square` / `MazeZone`
2. **位置与视野**：`DungeonLocation` / `ViewField` / `Compass`
3. **角色与战斗**：`Team` / `Hero` / `Monster` / `Entity` / `Ability` / `Attack` / `Defense`
4. **物品与法术**：`Item` / `Spell` / `SpellBook`
5. **触发与脚本**：`WallButton` / `SwitchCount` / `ScriptedDialog`
6. **UI 数据**：`DisplayCoordinates` / `InterfaceCoordinates` / `GameColors` / `GameMessage`

## 地牢结构

### Dungeon

**位置**：`core/src/com/ntk/darkmoor/engine/Dungeon.java`

**职责**：整个地牢，由多个 `Maze`（楼层）组成。

**字段**：
```java
private Map<String, Maze> mazes;   // 楼层名 → Maze 实例
private String name;
```

**主要方法**：
```java
public boolean init()                 // 从 XML 初始化
public Maze getMaze(String name)
public void dispose()
```

### Maze

**位置**：`core/src/com/ntk/darkmoor/engine/Maze.java`

**职责**：一个楼层（二维网格），由 `Square[][]` 组成。

**字段**：
```java
private Square[][] squares;           // 二维网格
private int width, height;
private Dungeon dungeon;
```

**主要方法**：
```java
public boolean init()
public Square getSquare(int x, int y)
public Square getSquare(Vector2 coord)
public void resize(Vector2 newsize)
```

### Square

**位置**：`core/src/com/ntk/darkmoor/engine/Square.java`

**职责**：一个网格格子，可以包含若干 actor（门、楼梯、陷阱等）。

**事件**（注释里写明）：
- `OnTeamEnter` - 队伍进入
- `OnTeamStand` - 队伍站定
- `OnTeamLeave` - 队伍离开
- `OnDropItem` - 物品被放下
- `OnCollectItem` - 物品被拾取
- `OnClick` - 用户点击

**字段**：
```java
private List<SquareActor> actors;     // 格子里的所有 actor
private List<Item> items;              // 地上的物品
private boolean wallNorth, wallEast, wallSouth, wallWest;  // 四面墙
```

### MazeZone

**职责**：maze 内的逻辑分区（用于触发器分组）。

## 位置与视野

### DungeonLocation

**位置**：`core/src/com/ntk/darkmoor/engine/DungeonLocation.java`

**职责**：完整的位置描述 = maze + 坐标 + 朝向 + 子位置。

**字段**：
```java
private String maze;                   // 楼层名
private Vector2 coordinate;            // 二维网格坐标
private SquarePosition position;       // 子位置（格子里具体哪个点）
private CardinalPoint direction;       // 朝向（北/东/南/西）
```

**用途**：所有"在某处发生某事"的逻辑都用 `DungeonLocation` 描述位置。

### ViewField

**位置**：`core/src/com/ntk/darkmoor/engine/ViewField.java`

**职责**：计算当前视野里的 15 个格子（A-O），用于第一人称渲染。

**枚举 `ViewFieldPosition`**：
```
A E B D C    （第 3 行，最远）
F J G I H    （第 2 行）
  K M L      （第 1 行）
  N ^ O      （第 0 行，^ 是玩家位置）
```

字母对应屏幕上的固定位置（在 `DisplayCoordinates` 里定义）。

**主要方法**：
```java
public ViewField(Maze maze, DungeonLocation location)
public Square getBlock(ViewFieldPosition pos)
public boolean isTeamVisible()
```

### Compass

**职责**：罗盘，4 方位枚举。

```java
public enum CardinalPoint {
    NORTH, EAST, SOUTH, WEST
}
```

## 角色与战斗

### Team

**位置**：`core/src/com/ntk/darkmoor/engine/Team.java`

**职责**：玩家小队，最多 6 个 `Hero`。

**字段**：
```java
public static final int MAX_HEROES = 6;
private Hero[] heroes = new Hero[MAX_HEROES];
private DungeonLocation location;       // 当前位置
private Item itemInHand;                 // 手上拿着的物品
```

**主要方法**：
```java
public void moveForward()
public void moveBackward()
public void turnLeft()
public void turnRight()
public DungeonLocation getLocation()
public Hero[] getHeroes()
public Item getItemInHand()
```

### Hero

**位置**：`core/src/com/ntk/darkmoor/engine/Hero.java`

**职责**：英雄角色。

**内部枚举**：
```java
public enum HeroClass { FIGHTER, MAGE, CLERIC, THIEF, ... }
public enum HeroHand { PRIMARY, SECONDARY, TWO_HANDED }
public enum HeroGender { MALE, FEMALE }
public enum HeroRace { HUMAN, ELF, DWARF, ... }
public enum InventoryPosition { PRIMARY, SECONDARY, ARMOR, ... }
```

**字段**：
```java
private String name;
private HitPoint hp;
private Ability strength, intelligence, wisdom, dexterity, constitution, charisma;
private Set<HeroClass> classes;          // D&D 多职业
private List<Item> inventory;
private SpellBook spellBook;
private int headTextureId;               // 头像贴图 id
```

**继承**：`Hero extends Entity`，`Entity` 是生物基类（Hero 和 Monster 共用）。

### Monster

**位置**：`core/src/com/ntk/darkmoor/engine/Monster.java`

**职责**：怪物。

**字段**：
```java
private int hp;
private int ac;                          // 装甲等级
private MonsterState state;              // AI 状态（Idle/Attack/Hurt）
private DungeonLocation location;
```

**v0.5 状态**：怪物能加载和显示，但 AI 状态机的行为未实现（不会主动攻击）。

### Entity

**位置**：`core/src/com/ntk/darkmoor/engine/Entity.java`

**职责**：生物基类。包含 alignment（阵营）等公共字段。

### Ability

**职责**：D&D 6 维属性（STR/INT/WIS/DEX/CON/CHA）。

### Attack / Defense / CriticalHit

**职责**：战斗数据。`Attack` 封装攻击骰子，`Defense` 封装 AC，`CriticalHit` 封装暴击规则。

### Dice

**职责**：骰子工具，用于 D&D 的 d20、d6 等。

```java
public static int roll(int faces)            // 掷一个 N 面骰
public static int roll(int count, int faces)  // 掷 N 个 M 面骰
```

### Profession

**职责**：职业数据。

### HitPoint

**职责**：HP 数据（current + max + 临时加成）。

## 物品与法术

### Item

**位置**：`core/src/com/ntk/darkmoor/engine/Item.java`

**职责**：物品（武器/护甲/消耗品/杂物）。

**XML 示例**（`Item.xml`）：
```xml
<item name="Long Sword">
    <type value="Weapon" />
    <damage dice="1" faces="8" />
    <weight value="4" />
    <icon id="15" />
</item>
```

### Spell

**位置**：`core/src/com/ntk/darkmoor/engine/Spell.java`

**职责**：法术定义。

**XML 示例**（`Spell.xml`）：
```xml
<spell name="Cure Minor Wounds">
    <description>This is an extremely weak healing spell.</description>
    <level value="1" />
    <class value="Cleric" />
    <script script="Spells" interface="CureMinorWounds" />
</spell>
```

### SpellBook

**位置**：`core/src/com/ntk/darkmoor/engine/SpellBook.java`

**职责**：英雄的法术书，记录已学法术。也提供法术选择 UI（`draw(Batch, ...)`）。

## 触发与脚本

### WallButton

**职责**：墙上的按钮（可被点击触发脚本）。

### SwitchCount

**职责**：记录开关被触发次数（用于"按下 3 次开门"这类逻辑）。

### ScriptedDialog

**位置**：`core/src/com/ntk/darkmoor/engine/ScriptedDialog.java`

**职责**：脚本驱动的对话（NPC 对话、剧情对话）。从 `Script.xml` 加载。

## UI 数据

### DisplayCoordinates

**职责**：屏幕上各个固定位置的坐标常量（用于地牢视图）。

```java
public static final int TOP_LEFT_X = 0;
public static final int TOP_LEFT_Y = 0;
public static final int PLAYABLE_WINDOW_WIDTH = 640;
public static final int PLAYABLE_WINDOW_HEIGHT = 400;
// ... 各 ViewFieldPosition 的坐标
```

### InterfaceCoordinates

**职责**：UI 控件的固定坐标常量（按钮、头像栏等）。

### GameColors

**职责**：颜色常量。

```java
public static final Color Main = new Color(...);
public static final Color Light = ...;
public static final Color Dark = ...;
public static final Color Red = ...;
```

### GameMessage / ScreenMessage

**职责**：游戏内浮动的消息文本（如"你受了 5 点伤害"）。

## 其他

### AutoMap

**位置**：`core/src/com/ntk/darkmoor/engine/AutoMap.java`

**职责**：自动地图。**v0.5 状态：只有 `draw(batch, "TODO...", 100, 100)` 占位。**

### CampDialog

**职责**：营地对话框（堆栈式管理多个子窗口）。详见 [modules/07-engine-gui.md](07-engine-gui.md)。

### DialogBase

**职责**：对话框基类。

### Decoration / DecorationSet

**职责**：装饰物（火把、雕像、家具等不可交互的视觉元素）。

### HandAction

**职责**：手部动作（使用物品时的动画状态）。

### HeroAllowedClasses / HeroExperienceLevels / HeroHPBonus

**职责**：英雄相关的小数据类。

### SavingThrowType

**职责**：D&D 豁免检定类型枚举（韧性/反射/意志等）。

### State

**职责**：游戏状态枚举。

### ThrownItem

**职责**：投掷物（飞刀、火球等）。

## 数据流示例：玩家移动一格

```
用户按前进按钮
  ↓
NavigationControlsGroup.button.touchDown
  ↓
team.moveForward()
  ↓
DungeonLocation.coordinate.y += 1  (按当前朝向)
  ↓
Maze.getSquare(newCoordinate) → newSquare
  ↓
触发 newSquare 的 OnTeamEnter 事件
  ↓
如果 newSquare 里有 EventSquare actor → 执行其脚本
  ↓
脚本可能调用 ActionBase 子类（如 DisplayMessage / Teleport / SpawnMonster）
  ↓
下一帧 MazeGroup.update() 重新计算 ViewField → 渲染新视野
```

## 学习建议

按这个顺序读，难度递增：

1. `Compass.java`（最短，5 分钟）
2. `Dice.java`（短，理解 D&D 骰子）
3. `DungeonLocation.java`（短，理解位置模型）
4. `HitPoint.java` + `Ability.java`（数据类）
5. `Team.java`（理解小队操作）
6. `Square.java`（理解格子模型）
7. `Maze.java` + `Dungeon.java`（理解地牢结构）
8. `Hero.java`（最长，~500 行，但分块清晰）
9. `ViewField.java`（理解第一人称渲染的数据来源）
10. `Monster.java` + `Item.java` + `Spell.java`（数据类）

读源码时建议结合对应的 XML 文件（`android/assets/data/`）一起看，能更快理解数据结构。

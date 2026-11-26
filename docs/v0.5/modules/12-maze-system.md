# 12. 地图系统模块（Maze System）

> **覆盖包**：`com.ntk.darkmoor.engine`（根包内的迷宫/方块/位置相关类）
> **关键类**：`Dungeon`、`Maze`、`Square`、`SquareActor` 及其子类、`DungeonLocation`、`Compass`、`Team`、`ViewField`、`DisplayCoordinates`
> **适合谁看**：想理解"我在哪儿"、"地图怎么存"、"主视角怎么画出来"、"如何加小地图/自动化地图"的人

---

## 12.1 一句话总结

darkmoor 的地图系统是一个**多层 tile 网格**：

- 一个 `Dungeon`（地下城）包含若干 `Maze`（楼层）
- 每个 `Maze` 是 `width × height` 的 `Square` 二维网格
- 每个 `Square` 上可以放：墙/地面、1 个 `SquareActor`（门/楼梯/陷阱/传送器等）、最多 4 个 `Monster`、若干 `Item`
- `Team.location` 是**唯一**的玩家位置真相来源，类型是 `DungeonLocation`（迷宫名 + 坐标 + 朝向 + 子位置）
- `ViewField` 把 `DungeonLocation` 转换成"眼前能看见的 15 个方块"
- `DisplayCoordinates` 是从 XML 加载的静态查找表，决定 15 个方块各自落在屏幕的哪个像素位置

---

## 12.2 关键概念速查表

| 概念 | 类 / 字段 | 取值范围 | 说明 |
|---|---|---|---|
| 楼层 | `Maze.name` (String) | `"Forest"`、`"Catacomb - 01"`、`"Temple - 02"` 等 | 不是整数编号，是字符串名 |
| 楼层尺寸 | `Maze.size` (Vector2) | 典型 20×17 ~ 40×40 | x=宽，y=高 |
| Tile 坐标 | `DungeonLocation.coordinate` (Vector2) | (0,0) ~ (size.x-1, size.y-1) | **(0,0) = NW 角，+X 向东，+Y 向南** |
| 朝向 | `DungeonLocation.direction` (CardinalPoint) | North / South / West / East | North=-Y，South=+Y，West=-X，East=+X |
| 子位置 | `DungeonLocation.position` (SquarePosition) | NW / NE / SW / SE / Center | 一个 tile 内的 4 个角 + 中心，用于英雄/怪物精确站位 |
| 玩家位置 | `Team.location` | 上述 4 个的组合 | 整个项目**唯一**的"我在哪儿"字段 |
| 当前楼层实例 | `Team.maze` | `Maze` 引用 | 由 `location.maze` 字符串解析得到 |
| 当前 tile | `Team.square` | `Square` 引用 | 缓存，避免每次都查 `maze.getSquare(...)` |
| 前方 tile | `Team.getFrontSquare()` | `Square` | 当前朝向上的相邻 tile |
| 视野 | `new ViewField(team.getMaze(), team.getLocation())` | 15 个 `ViewFieldPosition` (A~O + Team) | 计算可见的 15 个方块 |

---

## 12.3 坐标系详解

### 12.3.1 迷宫 tile 坐标

```
                +X (East)
                ──────►
           ┌──────────────────┐
           │(0,0) (1,0) (2,0) │   ← 迷宫北边（NW 角 = (0,0)）
           │(0,1) (1,1) (2,1) │
       +Y  │(0,2) (1,2) (2,2) │
       (South)                │
           │                  │
           └──────────────────┘
                                ↑
                            迷宫南边（SW 角 = (0, height-1)）
```

**关键约定**：

- 原点 (0,0) 在迷宫的 **西北角**（NW corner，左上角）
- +X 方向是**东**（→）
- +Y 方向是**南**（↓）
- 这与"屏幕像素坐标系 Y 向下"一致，但与"libGDX scene2d 坐标系 Y 向上"**相反**——做小地图时要小心

### 12.3.2 子位置（SquarePosition）

一个 tile 内部分成 4 个角 + 中心，用于精确站位：

```
    ┌──────┬──────┐
    │  NW  │  NE  │     0 = NorthWest
    │  (0) │  (1) │     1 = NorthEast
    ├──┐   ┌──┤     2 = SouthWest
    │  │ E │  │     3 = SouthEast
    │  │(4)│  │     4 = Center
    ├──┘   └──┤
    │  SW  │  SE  │
    │  (2) │  (3) │
    └──────┴──────┘
```

### 12.3.3 朝向（CardinalPoint）

```java
public enum CardinalPoint {
    North(0), South(1), West(2), East(3);
}
```

- `North` → -Y 方向（向迷宫北边走）
- `South` → +Y 方向
- `West` → -X 方向
- `East` → +X 方向

`Compass` 类提供静态工具方法：

| 方法 | 作用 |
|---|---|
| `Compass.rotate(dir, CompassRotation.Rotate90)` | 顺/逆时针旋转 90° |
| `Compass.getOppositeDirection(dir)` | 取反方向（North↔South、West↔East） |
| `Compass.seekDirection(from, target)` | 从 `from` 朝 `target` 的方向 |
| `Compass.isFacing(from, target)` | 是否面对面 |
| `Compass.getDirectionFromView(view, side)` | 把"玩家朝向 + 相对方位"翻译成绝对方向 |

### 12.3.4 屏幕 / Stage 坐标

| 常量 | 值 | 含义 |
|---|---|---|
| `DarkmoorGame.DISPLAY_WIDTH` | 1280 | 设计分辨率宽 |
| `DarkmoorGame.DISPLAY_HEIGHT` | 720 | 设计分辨率高 |
| `DisplayCoordinates.TOP_LEFT_X` | 0 | 第一人称迷宫视图左上角 X |
| `DisplayCoordinates.TOP_LEFT_Y` | 505 | 第一人称迷宫视图左上角 Y（从屏幕底部量） |
| `DisplayCoordinates.PLAYABLE_WINDOW_WIDTH` | 712 | 第一人称视图宽 |
| `DisplayCoordinates.PLAYABLE_WINDOW_HEIGHT` | 775 | 第一人称视图高 |

> ⚠️ **已知 bug**：`GameScreenBase` 用的是 `new StretchViewport(DISPLAY_WIDTH, DISPLAY_WIDTH)`（即 1280×1280），不是 `1280×720`。Stage 内部坐标系被纵向压缩到 720 高。所有 scene2d 坐标都按 1280×1280 系设计。

### 12.3.5 视野（ViewField）

`ViewField` 把"玩家所在 DungeonLocation"翻译成"眼前能看见的 15 个方块"。布局如下（玩家朝北时）：

```
   远处
   ─────
   A E B D C       ← 第 3 行（最远）
   F J G I H       ← 第 2 行
     K M L         ← 第 1 行
     N ↑ O         ← 第 0 行（玩家所在行，↑ = Team）
   ─────
   近处
```

`ViewField` 构造函数一次完成所有 15 个 `Square` 的查找：

```java
ViewField pov = new ViewField(team.getMaze(), team.getLocation());
Square s = pov.getBlock(ViewFieldPosition.G);  // 玩家正前方 2 格的中间
```

无论玩家朝哪个方向，`ViewField` 内部会根据 `direction` 旋转坐标偏移，所以 `ViewFieldPosition.A` 永远是"远处最左"。

---

## 12.4 类层级与职责

### 12.4.1 数据模型层

```
Dungeon
  └─ HashMap<String, Maze> mazes         // 楼层名 → 楼层实例
  └─ DungeonLocation startLocation       // 入口

Maze
  └─ ArrayList<List<Square>> squares     // [y][x] 二维网格
  └─ Vector2 size                        // width × height
  └─ String wallTilesetName              // 墙体贴图集
  └─ ArrayList<ThrownItem> thrownItems   // 飞行中的物品（投掷物/法术）
  └─ ArrayList<MazeZone> zones           // 触发区（脚本钩子）

Square
  └─ Vector2 location                    // (x, y)
  └─ SquareType type                     // Ground / Wall / Illusion
  └─ SquareActor actor                   // 门/楼梯/陷阱...（可空）
  └─ Monster[4] monsters                 // 4 个子位置各一个怪物槽
  └─ int[4] decorations                  // 4 个方向的装饰
  └─ ArrayList<List<Item>> items         // 4 个子位置的物品堆

DungeonLocation
  └─ String maze                         // 楼层名
  └─ Vector2 coordinate                  // (x, y)
  └─ CardinalPoint direction             // 朝向
  └─ SquarePosition position             // 子位置（NW/NE/SW/SE/Center）
```

### 12.4.2 行为层

| 类 | 角色 | 关键方法 |
|---|---|---|
| `Team` | 玩家小队（**位置的唯一修改者**） | `walk(front, strafe)`、`offset(dir, n)`、`teleport(loc)`、`getFrontLocation()`、`getFrontSquare()` |
| `Hero` | 单个角色（不存自身位置，通过 `GameScreen.getTeam()` 访问） | `useHand()`、`attack()` |
| `Monster` | 怪物（自带位置） | `move(offset)`、`teleport(square, pos)`、`getLocation()` |
| `SquareActor` (abstract) | tile 上的"东西"基类 | `onTeamEnter/Stand/Leave`、`onMonsterEnter/Leave`、`onItemDropped/Collected`、`onClick` |
| `Door` | 门（9 种类型，6 种状态） | `isBlocking` 取决于状态 |
| `Stair` | 楼梯 → 楼层切换 | `onTeamEnter()` 调用 `team.teleport(target)` |
| `Teleporter` | 传送器 | 同上 |
| `Pit` | 陷阱 | 落入后传送到 `target`，可能造成伤害 |
| `PressurePlate` | 压力板 | 触发 `PressurePlateScript` 列表 |
| `WallSwitch` | 墙上开关/拉杆 | 需要 `neededItem` 或解锁 `lockLevel` |
| `ForceField` | 力场（旋转/推动队伍） | `onTeamEnter()` 改变朝向或位置 |
| `EventSquare` | 触发区 | 触发 `ScriptedDialog` 或消息 |
| `AlcoveActor` | 壁龛（4 个方向各一个 `Alcove`） | 物品放入/取出脚本钩子 |

### 12.4.3 渲染层

| 类 | 角色 | 说明 |
|---|---|---|
| `MazeGroup` | 主视角 3D-like 视图（scene2d Group） | 每帧用 `ViewField` 重画 15 个方块的墙/装饰/Actor |
| `DisplayCoordinates` | 静态坐标查找表 | 从 `MazeElements.xml` 加载，包含 16 个 ViewFieldPosition 各自的墙/门/楼梯/陷阱/地面/怪物位置 |
| `TileDrawing` | 单个绘制指令数据 | (tileset ID, 屏幕坐标, 朝向, 翻转, 缩放) |
| `SpriteEffects` | 翻转枚举 | NONE / FLIP_HORIZONTALLY / FLIP_VERTICALY |
| `NavigationControlsGroup` | 底部导航按钮 + 罗盘 | 6 个方向按钮 + 3 张罗盘图 |
| `HeroInterfaceGroup` | 右侧 6 个角色面板 | 头像 + 血条 + 双手装备 |
| `Maze.drawMiniMap(batch, point)` | **遗留未实现**的小地图方法 | 只有颜色逻辑，`batch.fillRectangle` 是 TODO 注释 |

---

## 12.5 玩家位置的全局访问点

```java
// 玩家位置的唯一来源
GameScreen.getTeam()                        // → Team 单例
GameScreen.getTeam().getLocation()          // → DungeonLocation（位置结构体）
GameScreen.getTeam().getMaze()              // → Maze（当前楼层实例）
GameScreen.getTeam().getSquare()            // → Square（当前 tile）
GameScreen.getTeam().getDirection()         // → CardinalPoint（朝向）
GameScreen.getTeam().getFrontLocation()     // → DungeonLocation（前方一格）
GameScreen.getTeam().getFrontSquare()       // → Square（前方一格）

// 全局资源
DarkmoorGame.getInstance()                  // → DarkmoorGame 单例
Resources.getDungeon()                      // → Dungeon（当前地下城）
Settings.getLastLoadedInstance()            // → Settings 单例
```

> **重要**：项目中**没有任何其他地方**保存玩家位置。所有需要"我在哪儿"的代码都通过 `GameScreen.getTeam().getLocation()` 读取。

---

## 12.6 玩家移动的完整流程

以"按 ↑ 前进一步"为例：

```
用户点击 NavigationControlsGroup 的"前进"按钮
  ↓
team.walk(0, -1)            // front=0（不转向）, strafe=-1（向前）
  ↓
Team.walk():
  1. 检查 canMove()          // 冷却时间 + 是否在战斗
  2. 计算 destCoord = location.coordinate + direction 的偏移
  3. maze.getSquare(destCoord)  // 取目标 tile
  4. 检查 square.isWall() / actor.isBlocking()  // 撞墙检查
  5. 若可走：
     a. 调用 location.getSquare().onTeamLeave()
     b. location.coordinate.add(offset)
     c. 调用 newSquare.onTeamEnter()   // 可能触发楼梯/陷阱/事件
     d. 缓存 this.square = newSquare
     e. lastMove = now                  // 更新冷却计时
  ↓
GameScreen.update() → mazeGroup.update()
  ↓
MazeGroup.update():
  1. 读取 team.getLocation()
  2. new ViewField(team.getMaze(), location)  // 重新计算视野
  3. setUpBackground(location)         // 背景翻转判断
  4. for each ViewFieldPosition in [A..O]:
       updateSquare(...)               // 设置 Image pool 中的对应 sprite
  ↓
stage.draw() → MazeGroup.drawChildren() → 用 ScissorStack 裁剪 → 绘制所有 sprite
```

---

## 12.7 楼层切换的完整流程

以"踩到楼梯"为例：

```
玩家移动到楼梯所在 tile
  ↓
Team.walk() 调用 newSquare.onTeamEnter()
  ↓
Square.onTeamEnter() 转发给 actor.onTeamEnter()
  ↓
Stair.onTeamEnter():
  1. 读取 this.target (一个 DungeonLocation，包含目标楼层名 + 坐标 + 朝向)
  2. team.teleport(target)
  ↓
Team.teleport(DungeonLocation):
  1. 调用 location.getSquare().onTeamLeave()   // 离开旧 tile
  2. this.maze = Resources.getDungeon().getMaze(target.maze)  // 切楼层
  3. this.location = new DungeonLocation(target)
  4. this.square = maze.getSquare(target.coordinate)  // 缓存新 tile
  5. 调用 newSquare.onTeamStand()
  ↓
下一帧 GameScreen.update() → mazeGroup.update()
  → 用新的 team.getLocation() 重建 ViewField → 显示新楼层的视图
```

---

## 12.8 渲染管线（每帧）

```
DarkmoorGame.render()                  // libGDX Game 主循环
  ↓
GameScreen.render(delta)               // 来自 GameScreenBase
  ↓
GameScreenBase.render(delta):
  ├── update(delta, true, false)
  │     └── GameScreen.update():
  │            ├── team.getMaze() != null → mazeGroup.update()
  │            │     └── 重建 ViewField，更新 128 个 Image sprite 的 Drawable
  │            ├── navControlsGroup.update()
  │            │     └── 根据 team.getDirection() 切换罗盘图
  │            └── heroInterfaceGroup.update()
  │                  └── 更新血条、装备图标
  └── draw(delta)
        └── stage.act(delta) + stage.draw()
              └── 每个 Group 绘制其子 Actor
                    └── MazeGroup.drawChildren() 用 ScissorStack 裁剪到第一人称视图区
```

---

## 12.9 已知遗留问题

| 问题 | 影响 | 备注 |
|---|---|---|
| `Maze.drawMiniMap()` 完全没实现 | 没有任何地图功能 | 只有颜色判断逻辑，`batch.fillRectangle` 是 TODO 注释 |
| `AutoMap` 类是 73 行的 stub | 没有"按 M 看大地图"功能 | 继承 `ScreenAdapter`，`draw()` 只打印 "TODO…" |
| `StretchViewport(1280, 1280)` | Stage 纵向被压缩到 720 | 应该是 `1280, 720`；目前所有 scene2d 坐标都按 1280×1280 设计 |
| `DungeonLocation` 字段可变 | 没有线程安全保证 | `Offset()` 直接修改 `coordinate`，不是返回新对象 |
| `Square.monsters` 是固定 4 槽数组 | 一个 tile 最多 4 个怪物 | 子位置约束 |
| `MazeZone` 脚本钩子未完全接线 | 触发区可能不工作 | 需要在 `Maze.update()` 中调用，但目前 `Maze.update()` 只更新 `thrownItems` |

---

## 12.10 小地图集成指南

### 12.10.1 为什么需要小地图

当前游戏截图（见 `docs/screenshots/dungeon.png`）显示：玩家面对一条石砖走廊，但**完全不知道自己在楼层里的哪个位置**。这是地牢爬行类游戏的致命问题——没有空间感，玩家会迷路、卡关。

### 12.10.2 推荐实现：`MinimapGroup`

新建 `core/src/com/ntk/darkmoor/engine/graphics/MinimapGroup.java`，继承 `GameScreenGroup`：

```java
public class MinimapGroup extends GameScreenGroup implements Disposable {
    public static final int MINIMAP_WIDTH = 220;
    public static final int MINIMAP_HEIGHT = 220;
    public static final float MINIMAP_X = 8f;
    public static final float MINIMAP_Y = DisplayCoordinates.PLAYABLE_WINDOW_HEIGHT
                                        - MINIMAP_HEIGHT - 8f;

    private Pixmap pixmap;
    private Texture texture;
    private Image image;

    @Override
    public void initialize() {
        pixmap = new Pixmap(MINIMAP_WIDTH, MINIMAP_HEIGHT, Pixmap.Format.RGBA8888);
        texture = new Texture(pixmap);
        image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        image.setBounds(MINIMAP_X, MINIMAP_Y, MINIMAP_WIDTH, MINIMAP_HEIGHT);
        addActor(image);
    }

    @Override
    public void update() {
        if (team == null || team.getMaze() == null) return;
        Settings s = Settings.getLastLoadedInstance();
        setVisible(s == null || s.isShowMinimap());
        if (!isVisible()) return;
        redraw();
    }

    private void redraw() {
        Maze maze = team.getMaze();
        int mazeW = (int) maze.getSize().x;
        int mazeH = (int) maze.getSize().y;
        float tilePx = Math.min(
            (float)(MINIMAP_WIDTH - 8) / mazeW,
            (float)(MINIMAP_HEIGHT - 8) / mazeH);
        // ...详见 08-minimap-design.md 和源码
    }
}
```

### 12.10.3 在 `GameScreen` 注册

```java
// 在 initScreenObjects() 末尾添加：
minimapGroup = new MinimapGroup();
minimapGroup.team = GameScreen.team;
minimapGroup.uiSkin = uiSkin;
minimapGroup.initialize();
stage.addActor(minimapGroup);

// 在 update() 中添加（紧跟 mazeGroup.update() 之后）：
if (team.getMaze() != null) {
    minimapGroup.setVisible(interfaceType == TeamInterface.Main);
    minimapGroup.update();
}
```

### 12.10.4 颜色方案（沿用 `Maze.drawMiniMap` 的逻辑并扩展）

| 元素 | 颜色 | RGBA |
|---|---|---|
| 墙 | 深灰 | (48, 48, 48, 255) |
| 幻象墙 | 中灰 | (102, 102, 102, 255) |
| 地面 | 浅灰 | (217, 217, 217, 255) |
| 门 | 黄 | (255, 255, 85, 255) |
| 楼梯 | 浅绿 | (146, 207, 138, 255) |
| 陷阱 | 深蓝 | (44, 48, 138, 255) |
| 传送器 | 紫 | (179, 77, 230, 255) |
| 怪物 | 红 | (255, 85, 85, 255) |
| 物品 | 橙 | (255, 165, 0, 255) |
| 玩家 | 蓝 | (101, 105, 182, 255) |
| 朝向箭头 | 白 | (255, 255, 255, 255) |

### 12.10.5 在 Settings 加开关

```java
// Settings.java
private static final String SETTING_SHOW_MINIMAP = "ShowMinimap";
private boolean showMinimap;

// 在 loadSettings() 中：
settings.setShowMinimap(settings.extractBooleanProperty(SETTING_SHOW_MINIMAP, true));

public boolean isShowMinimap() { return showMinimap; }
public void setShowMinimap(boolean v) { this.showMinimap = v; }
```

并在 `settings.properties` 加一行：

```
ShowMinimap=true
```

---

## 12.11 未来扩展方向

| 方向 | 实现思路 | 难度 |
|---|---|---|
| **战争迷雾**（只显示走过的 tile） | 在 `Square` 加 `boolean explored`，`onTeamEnter()` 时置 true；minimap 只画 explored tile | 中 |
| **大地图模式**（按 M 全屏查看） | 实现 `AutoMap` stub，做成全屏 `ScreenAdapter`，按 M 切换 | 中 |
| **小地图缩放** | 鼠标滚轮调整 `tilePx`，小地图区域只显示玩家周围 N×N | 中 |
| **小地图旋转**（玩家朝向始终朝上） | 渲染时旋转 `Pixmap` 或用 `Image.setRotation()` | 难 |
| **多楼层缩略图** | 旁边显示其他楼层的迷你缩略图，当前楼层高亮 | 中 |
| **路径标记** | 玩家走过的路径用淡色线连接 | 易 |
| **注释标记** | 玩家可在 tile 上加文字注释（"这里有宝箱"） | 中 |

---

## 12.12 相关文档

- [08-minimap-design.md](../08-minimap-design.md) — 小地图模块的详细设计与实现
- [06-engine-graphics.md](06-engine-graphics.md) — `MazeGroup` 等图形组的总览
- [04-engine-core.md](04-engine-core.md) — `Maze`/`Square`/`Team` 等核心数据类
- [05-engine-actor.md](05-engine-actor.md) — `Door`/`Stair`/`Pit` 等 tile 上的 Actor

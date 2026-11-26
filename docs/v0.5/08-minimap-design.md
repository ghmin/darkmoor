# 08. 小地图模块设计与实现

> **状态**：v0.5 阶段新增模块
> **目的**：解决"玩家不知道自己在哪儿"的核心可用性问题
> **相关源码**：
> - `core/src/com/ntk/darkmoor/engine/graphics/MinimapGroup.java`（新建）
> - `core/src/com/ntk/darkmoor/GameScreen.java`（修改：注册 MinimapGroup）
> - `core/src/com/ntk/darkmoor/config/Settings.java`（修改：加 showMinimap 开关）
> - `android/assets/data/settings.properties` + `desktop/resources/data/settings.properties`（加 `ShowMinimap=true`）

---

## 8.1 背景与问题

在 v0.5 截图（见用户上传截图）中，玩家看到的是：

```
┌─────────────────────────────────┐
│                                 │
│   第一人称石砖走廊视图           │
│   （空无一物，没有地标）         │
│                                 │
├─────────────────────────────────┤
│  Nav   | Compass | CAMP         │
└─────────────────────────────────┘
```

**问题**：玩家完全不知道自己在楼层里的哪个位置、面朝什么方向、附近有什么。

对地牢爬行类游戏（Blobber CRPG）来说，这是致命的可用性缺陷。原版 EOB2 通过画在屏幕角落的"自动化地图"（AutoMap）解决，darkmoor 项目中 `AutoMap.java` 是一个 73 行的 stub，`Maze.drawMiniMap()` 也只有颜色逻辑、`batch.fillRectangle` 是 TODO 注释。

## 8.2 设计目标

| 目标 | 说明 |
|---|---|
| 实时显示玩家在楼层里的位置 | 包括坐标和朝向 |
| 显示附近的可交互对象 | 门、楼梯、陷阱、传送器 |
| 显示威胁与战利品 | 怪物（红）、地面物品（橙） |
| 不遮挡主视角 | 放在第一人称视图的左上角，半透明 |
| 可开关 | Settings 里加 `ShowMinimap` 开关 |
| 性能可接受 | 每帧重绘 220×220 像素的 Pixmap，开销低 |
| 通用、可扩展 | 后续可加战争迷雾、大地图模式 |

## 8.3 屏幕布局选择

截图分析与候选位置：

| 候选位置 | 优势 | 劣势 | 是否选用 |
|---|---|---|---|
| **左上角**（第一人称视图左上） | 不遮挡主走廊视图；视觉习惯（HUD 通常在角落） | 可能遮挡远处墙体的左上角 | ✅ 选用 |
| 右上角 | 同上 | 右上是角色面板区，没空间 | ✗ |
| 左下角（导航按钮上方） | 有空间 | 遮挡第一人称视图的近处地面，影响操作预判 | ✗ |
| 中间偏左 | 显眼 | 遮挡主视图 | ✗ |
| 浮窗（按 M 切换） | 不占常驻空间 | 玩家无法持续感知位置 | 留作未来扩展 |

**最终位置**：

```
MINIMAP_X = 8                              // 距屏幕左边 8 px
MINIMAP_Y = PLAYABLE_WINDOW_HEIGHT
          - MINIMAP_HEIGHT - 8             // 距第一人称视图顶部 8 px
                                          //（PLAYABLE_WINDOW_HEIGHT = 775）
MINIMAP_WIDTH = MINIMAP_HEIGHT = 220       // 正方形
```

stage 坐标系（Y 向上）下，这相当于：

```
y = 775 - 220 - 8 = 547    // Image 左下角的 Y
```

正好覆盖第一人称视图的左上角，但因为视图本身在那里通常是远处的墙顶/天花板，遮挡影响小。

## 8.4 渲染技术选型

### 候选 A：scene2d Image 池（仿 MazeGroup）

为每个 tile 创建一个 `Image`，每帧更新 `Drawable`。

- ✅ 与项目现有风格一致
- ❌ 40×40 楼层 = 1600 个 Image，过多
- ❌ 每帧切换 1600 个 Drawable 性能差

### 候选 B：ShapeRenderer 直接绘制

重写 `draw(Batch, float)`，在 `Batch.end()` 后用 `ShapeRenderer` 画方块。

- ✅ 灵活、性能好
- ❌ scene2d Group 与 ShapeRenderer 混用容易出错（坐标系、Batch 状态）
- ❌ 与项目其他 Group 风格不一致

### 候选 C：Pixmap + Texture + Image（最终选用）

每帧用 `Pixmap` 在内存中重绘整个 minimap，上传到一个 `Texture`，再由一个 `Image` 渲染。

- ✅ 单个 Image，scene2d 集成干净
- ✅ Pixmap 提供 `fillRectangle`、`fillTriangle` 等基本绘图，够用
- ✅ 220×220 RGBA8888 = ~190KB，每帧重绘开销可接受
- ⚠️ `texture.draw(pixmap, 0, 0)` 每帧调用会触发 GPU 上传，但单帧 < 1ms
- ✅ 易于扩展（战争迷雾、注释等只需在 Pixmap 上画更多东西）

## 8.5 颜色方案

沿用 `Maze.drawMiniMap()` 的颜色判断逻辑，扩展物品和传送器：

| 元素 | 颜色 | RGBA | 来源 |
|---|---|---|---|
| 墙 (Wall) | 深灰 | (48, 48, 48, 255) | 比纯黑更柔和 |
| 幻象墙 (Illusion) | 中灰 | (102, 102, 102, 255) | 略亮于墙 |
| 地面 (Ground) | 浅灰 | (217, 217, 217, 255) | 主背景色 |
| 门 (Door) | 黄 | (255, 255, 85, 255) | `GameColors.Yellow` |
| 楼梯 (Stair) | 浅绿 | (146, 207, 138, 255) | `GameColors.LightGreen` |
| 陷阱 (Pit) | 深蓝 | (44, 48, 138, 255) | `GameColors.DarkBlue` |
| 传送器 (Teleporter) | 紫 | (179, 77, 230, 255) | 新增，区别于楼梯 |
| 怪物 (Monster) | 红 | (255, 85, 85, 255) | `GameColors.Red` |
| 地面物品 (Item) | 橙 | (255, 165, 0, 255) | 新增 |
| 玩家 (Team) | 蓝 | (101, 105, 182, 255) | `GameColors.Blue` |
| 朝向箭头 | 白 | (255, 255, 255, 255) | 高对比 |

**优先级**（同一 tile 上有多个东西时的覆盖顺序，从低到高）：

```
Wall/Illusion/Ground (基础)
  → Item (橙)
    → Door / Stair / Pit / Teleporter (Actor)
      → Monster (红，最高优先级，因为最威胁)
        → Team (蓝，玩家自己)
```

## 8.6 坐标系处理

**关键陷阱**：迷宫坐标系 Y 向下（+Y = South），Pixmap 坐标系 Y 也向下（(0,0) = 左上）。两者方向一致，所以**直接用** `pixmap.fillRectangle(x, y, ...)` 即可。

但 `Image` 在 stage 坐标系中 Y 向上，Texture 默认会被 Image 正确渲染（Pixmap 顶部 = Image 顶部）。这一点通过实测验证：libGDX 的 Texture 默认 `useMipMaps = false`，TextureRegion 默认不翻转 Y。

**tile 像素大小计算**：

```java
float tilePx = Math.min(
    (float)(MINIMAP_WIDTH - 8) / mazeW,
    (float)(MINIMAP_HEIGHT - 8) / mazeH);
```

- 40×40 楼层 → tilePx ≈ 5.3 → 5px
- 20×17 楼层 → tilePx ≈ 10.85 → 10px
- 始终保持长宽比，地图居中显示

## 8.7 朝向箭头绘制

玩家朝向用三角形箭头表示，从玩家 tile 中心向朝向方向延伸：

```java
int cx = px + sz / 2;
int cy = py + sz / 2;
int armLen = sz + 2;
switch (dir) {
    case North: // 朝上（迷宫 -Y 方向）
        pixmap.fillTriangle(cx, cy - armLen, cx - sz/2, cy, cx + sz/2, cy);
        break;
    case South: // 朝下
        pixmap.fillTriangle(cx, cy + armLen, cx - sz/2, cy, cx + sz/2, cy);
        break;
    case East:  // 朝右
        pixmap.fillTriangle(cx + armLen, cy, cx, cy - sz/2, cx, cy + sz/2);
        break;
    case West:  // 朝左
        pixmap.fillTriangle(cx - armLen, cy, cx, cy - sz/2, cx, cy + sz/2);
        break;
}
```

由于 Pixmap Y 向下，`cy - armLen` 实际显示在玩家方块上方（北边），符合"北朝上"的视觉惯例。

## 8.8 类结构

```
MinimapGroup extends GameScreenGroup implements Disposable
├── 字段
│   ├── public Team team           // 由 GameScreen 注入
│   ├── public Skin uiSkin         // 由 GameScreen 注入
│   ├── private Pixmap pixmap      // 220×220 内存位图
│   ├── private Texture texture    // pixmap 上传到 GPU
│   └── private Image image        // 显示 texture 的 scene2d Actor
├── 方法
│   ├── initialize()               // 创建 pixmap/texture/image，加入 Group
│   ├── update()                   // 每帧调用，触发 redraw() 并处理可见性
│   ├── redraw()                   // 实际重绘逻辑
│   └── dispose()                  // 释放 pixmap/texture
└── 常量
    ├── MINIMAP_WIDTH = 220
    ├── MINIMAP_HEIGHT = 220
    ├── MINIMAP_X = 8
    └── MINIMAP_Y = PLAYABLE_WINDOW_HEIGHT - 228
```

## 8.9 集成到 GameScreen

### 8.9.1 字段声明

```java
// 在 GameScreen 类的字段区添加：
private MinimapGroup minimapGroup;
```

### 8.9.2 在 `initScreenObjects()` 末尾添加

```java
minimapGroup = new MinimapGroup();
minimapGroup.team = GameScreen.team;
minimapGroup.uiSkin = uiSkin;
minimapGroup.initialize();
stage.addActor(minimapGroup);
```

### 8.9.3 在 `update()` 中添加更新调用

在 `if (team.getMaze() != null) mazeGroup.update();` 之后：

```java
if (team.getMaze() != null) {
    minimapGroup.setVisible(interfaceType == TeamInterface.Main);
    minimapGroup.update();
}
```

只在主界面（非 Inventory/Statistic）显示小地图，避免遮挡物品/统计视图。

### 8.9.4 在 `unloadContent()` 中添加 dispose

```java
if (minimapGroup != null)
    minimapGroup.dispose();
minimapGroup = null;
```

## 8.10 Settings 开关

### 8.10.1 Settings.java 修改

```java
// 1. 加常量
private static final String SETTING_SHOW_MINIMAP = "ShowMinimap";

// 2. 加字段
private boolean showMinimap;

// 3. 在 loadSettings() 中添加
settings.setShowMinimap(settings.extractBooleanProperty(SETTING_SHOW_MINIMAP, true));

// 4. 加 getter/setter
public boolean isShowMinimap() { return showMinimap; }
public void setShowMinimap(boolean v) { this.showMinimap = v; }
```

### 8.10.2 settings.properties 修改

在文件末尾添加：

```
ShowMinimap=true
```

需同时改 `android/assets/data/settings.properties` 和 `desktop/resources/data/settings.properties`。

## 8.11 性能分析

| 操作 | 频率 | 开销估算 |
|---|---|---|
| `pixmap.fill()` 清空 | 每帧 | ~0.05ms (220×220) |
| `maze.getSquare(x,y)` 调用 | 每帧 1600 次（40×40） | ~0.5ms |
| `pixmap.fillRectangle()` | 每帧 1600 次 | ~0.3ms |
| `texture.draw(pixmap, 0, 0)` | 每帧 | ~0.2ms（GPU 上传） |
| **总计** | 每帧 | ~1ms，可接受 |

如果未来需要优化：
- 只在 `team.hasMoved` 时重绘（移动事件驱动）
- 用脏矩形只重绘变化区域
- 缓存静态部分（墙/门），只重绘动态部分（怪物/玩家）

## 8.12 测试清单

| 测试项 | 期望结果 |
|---|---|
| 加载存档进入游戏 | 左上角出现 220×220 半透明 minimap |
| 玩家移动 | minimap 中蓝色方块移动，箭头方向不变 |
| 玩家转向 | 箭头方向变化，蓝色方块位置不变 |
| 进入有怪物的楼层 | minimap 中可见红点 |
| 走过门 | 黄色方块在身后出现 |
| 踩楼梯切楼层 | minimap 整体刷新为新楼层布局 |
| Settings 设 `ShowMinimap=false` | minimap 消失 |
| Inventory/Statistic 视图 | minimap 隐藏 |
| 40×40 大楼层 | minimap 缩小显示完整布局 |
| 20×17 小楼层 | minimap 居中显示，tiles 较大 |

## 8.13 后续扩展方向

详见 [12-maze-system.md §12.11](modules/12-maze-system.md#1211-未来扩展方向)。

短期可考虑：

1. **战争迷雾**：在 `Square` 加 `boolean explored`，初始 false，`onTeamEnter()` 时置 true。Minimap 只画 `explored=true` 的 tile，未探索区域画黑。这能恢复游戏的"探索感"。
2. **大地图模式**：实现 `AutoMap` stub，做成全屏 `ScreenAdapter`，按 M 键切换。可以显示当前楼层的全图（无迷雾）+ 楼层切换列表。
3. **小地图旋转**：让玩家朝向始终朝上（北朝上→玩家朝上）。需要旋转 Pixmap 或用 `Image.setRotation()`，但 Pixmap 旋转较复杂，可能需要用 `SpriteBatch.draw(texture, ..., rotation)` 路径。

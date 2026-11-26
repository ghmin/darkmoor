# 模块 06 - `com.ntk.darkmoor.engine.graphics`

## 概览

把数据模型画到屏幕上。每个屏幕的视觉部分由若干 libGDX `Group` 组成，加到 `Stage` 里。

| 类 | 文件 | 角色 |
|---|---|---|
| `GameScreenGroup` | `GameScreenGroup.java` | 所有 game screen Group 的基类 |
| `MazeGroup` | `MazeGroup.java` | 第一人称地牢视图（最复杂） |
| `HeroInterfaceGroup` | `HeroInterfaceGroup.java` | 小队头像栏 |
| `InventoryGroup` | `InventoryGroup.java` | 物品栏 |
| `NavigationControlsGroup` | `NavigationControlsGroup.java` | 移动控制按钮 |
| `StatisticsGroup` | `StatisticsGroup.java` | 数值面板 |
| `TileDrawing` | `TileDrawing.java` | 单个 tile 的绘制数据 |
| `SpriteEffects` | `SpriteEffects.java` | 翻转效果枚举 |

## GameScreenGroup（基类）

**位置**：`core/src/com/ntk/darkmoor/engine/graphics/GameScreenGroup.java`

**继承**：libGDX `Group`（scene2d 容器）

**职责**：所有 game screen Group 的基类，提供：
- `initialize()` 模板方法（创建子 actor）
- `update()` 模板方法（每帧更新逻辑）
- `drawChildren(Batch, float)` 模板方法（带剪裁绘制子 actor）

子类要做的：
1. 重写 `initialize()` 创建内部 Actor 池
2. 重写 `update()` 每帧更新可见性和位置
3. 可重写 `drawChildren()` 加自定义剪裁

## MazeGroup（最复杂）

**位置**：`core/src/com/ntk/darkmoor/engine/graphics/MazeGroup.java`

**职责**：第一人称地牢视图。把 `ViewField` 的 15 个格子（A-O）按深度和位置画到屏幕上。

**核心思路**：
1. 预创建 128 个 `Image` actor 作为 sprite 池
2. 每帧 `update()` 时根据 `ViewField` 计算每个格子要画哪些 tile
3. 把 sprite 池里的 `Image` 设置成对应 tile，按深度顺序排列
4. 隐藏多余的 sprite

**绘制顺序**（远到近）：
```
A E B D C    第 3 行（最远）
F J G I H    第 2 行
  K M L      第 1 行
  N ^ O      第 0 行（最近）
```

**剪裁**：地牢视图区域用 `ScissorStack` 剪裁，避免画到 UI 区域。

```java
@Override
public void drawChildren(Batch batch, float parentAlpha) {
    Rectangle scissors = new Rectangle();
    Rectangle clipBounds = new Rectangle(-1f, -0.21f, 1.111f, 1.25f);
    // v0.5 修复：libGDX 1.13+ 删除了 4-arg calculateScissors，改用 8-arg 重载
    ScissorStack.calculateScissors(
            DarkmoorGame.getInstance().getCamera(),
            0, 0, com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight(),
            batch.getTransformMatrix(), clipBounds, scissors);

    if (ScissorStack.pushScissors(scissors)) {
        batch.flush();
        super.drawChildren(batch, parentAlpha);
        batch.flush();
        ScissorStack.popScissors();
    }
}
```

**关键常量**：`MAX_SPRITES = 128`——一帧最多显示 128 个 tile（足够画 15 个格子的墙/地/顶/装饰）。

## HeroInterfaceGroup

**位置**：`core/src/com/ntk/darkmoor/engine/graphics/HeroInterfaceGroup.java`

**职责**：屏幕下方的小队头像栏。显示 6 个英雄的头像 + HP 条。

**布局**：6 个 64×64 的头像方块，2 行 3 列。

**交互**：点击头像 → 选中该英雄（用于物品/法术目标选择）。

## InventoryGroup

**位置**：`core/src/com/ntk/darkmoor/engine/graphics/InventoryGroup.java`

**职责**：物品栏，显示选中英雄身上的装备格（主手/副手/护甲/头盔/戒指等）。

**交互**：拖拽物品到/从装备格。

## NavigationControlsGroup

**位置**：`core/src/com/ntk/darkmoor/engine/graphics/NavigationControlsGroup.java`

**职责**：移动控制按钮（前进/后退/左转/右转）。

**布局**：屏幕右下角的 4 个方向按钮。

**触发**：点击 → 调用 `team.moveForward()` / `team.moveBackward()` / `team.turnLeft()` / `team.turnRight()`。

## StatisticsGroup

**位置**：`core/src/com/ntk/darkmoor/engine/graphics/StatisticsGroup.java`

**职责**：数值面板，显示选中英雄的属性（STR/INT/WIS/DEX/CON/CHA）、HP、MP、AC 等。

## TileDrawing

**位置**：`core/src/com/ntk/darkmoor/engine/graphics/TileDrawing.java`

**职责**：单个 tile 的绘制数据（贴图 id + 位置 + 翻转效果）。

**字段**：
```java
private int tileId;             // TextureSet 里的 tile id
private Vector2 location;        // 屏幕位置
private SpriteEffects effect;    // 翻转效果
```

**用途**：`MazeGroup.update()` 内部生成 `TileDrawing` 列表，然后转成 sprite 池里的 `Image` 配置。

## SpriteEffects

**位置**：`core/src/com/ntk/darkmoor/engine/graphics/SpriteEffects.java`

**职责**：翻转效果枚举。

```java
public enum SpriteEffects {
    NONE,
    FLIP_HORIZONTALLY,
    FLIP_VERTICALLY,
    FLIP_BOTH
}
```

**用途**：`MazeElements.xml` 里大量用到，例如：
```xml
<stair position="e" x="304" y="56" tile="25" effect="FlipHorizontally, FlipVertically" />
```

## 渲染流程示例

以 `GameScreen.render(delta)` 为例：

```
1. GameScreenBase.render(delta) 调用 stage.act() + stage.draw()
2. stage 派发 update/draw 给所有子 Group：
   ├── MazeGroup
   │   ├── update(): 计算 ViewField, 配置 128 个 Image
   │   └── drawChildren(): ScissorStack 剪裁 + 绘制可见 Image
   ├── HeroInterfaceGroup
   │   ├── update(): 检查队伍变化
   │   └── drawChildren(): 绘制 6 个头像 + HP 条
   ├── NavigationControlsGroup
   │   └── drawChildren(): 绘制 4 个方向按钮
   └── StatisticsGroup
       └── drawChildren(): 绘制选中英雄的属性
```

## v0.5 修复记录

`MazeGroup.java` 的 `drawChildren` 里的 `ScissorStack.calculateScissors` 调用从 4-arg 改成 8-arg，是这次升级里**唯一**对这个包做的代码修改。其他类没有破坏性 API 变化。

## 学习建议

1. 先读 `SpriteEffects.java`（最短，5 分钟）
2. 再读 `TileDrawing.java`（数据类）
3. 再读 `GameScreenGroup.java`（基类）
4. 再读 `NavigationControlsGroup.java`（最简单的子类）
5. 再读 `HeroInterfaceGroup.java` + `StatisticsGroup.java`
6. 最后读 `MazeGroup.java`（最复杂，~220 行，但分块清晰：initialize / update / drawChildren）

读 `MazeGroup` 时建议手画一张 15 格视野图（A-O 的位置），对照 `DisplayCoordinates` 里的坐标常量，理解每帧的绘制过程。

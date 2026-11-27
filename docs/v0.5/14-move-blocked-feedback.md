# 14 · 移动受阻（撞墙）玩家反馈系统

> 轮次：v0.5 第十轮（2026-11-27）
> 关联代码：`engine/feedback/Feedback.java`、`engine/feedback/NotificationToast.java`、
> `engine/Team.java`、`GameScreen.java`
> 关联文档：`13-random-event-system.md`（同一轮次的随机事件总线）

---

## 1. 问题与根因

**现象**：向墙壁按移动键，队伍不动，屏幕上没有任何反馈——玩家不知道是撞墙了、
有怪物挡路，还是游戏卡了。连续多按几次也没有任何变化，体验很差。

**根因**（两条链路同时断裂）：

1. `Team.move()` 被阻挡时确实调用了 `GameMessage.buildMessage(1)`
   （stringtable game#1，中文表里就是"那个方向走不通！"——**文案早就翻译好了**）。
   但 `GameMessage.draw()` / `GameMessage.update()` 在整个工程里**零调用点**
   （详见 doc-13 §3.2），消息进了队列永远不会被画出来。
2. `walk()` 的返回值（移动是否成功）在键盘处理器和屏幕按钮里都被忽略，
   没有任何 UI 层机会做额外反馈。

---

## 2. 方案设计

### 2.1 反馈形式选型

结合本项目现状（EOB 式第一人称格子移动、v0.5 有 VisUI 中文字体、无音频接入）：

| 方案 | 观感 | 实现成本 | 决定 |
| --- | --- | --- | --- |
| **A. 消息横幅（toast）** | 文字直达："那个方向走不通！" | 低 | ✅ 采纳 |
| **B. 迷宫视图震动** | 拟真撞击感，EOB 原版就"哐"一下 | 低（scene2d Action） | ✅ 采纳（叠加 A） |
| C. 专用音效 | 最拟真 | 需要音频管线 + 合适素材（资源库里没有撞墙音效，door/hit/miss 语义都不对） | 📌 留给后续 |
| D. 屏幕红闪 / vignette | 醒目 | 需要全屏 shader/贴图，v0.5 过重 | ❌ 暂不做 |

A + B 的组合：**红色半透明横幅显示"那个方向走不通！"约 1.5 秒后淡出，
同时迷宫画面做一次 18px 幅度的前后轻震**。文字讲清楚原因，震动提供身体记忆。

### 2.2 挂点选择：为什么在 Team.move() 而不是输入层

移动失败有三种起因，都汇聚在 `Team.move()`：

1. 目标格 `isBlocking()`（墙/幻墙）；
2. 目标格有怪物（`getMonsterCount() > 0`）；
3. ——而 `canMove()` 冷却失败（150ms 移动间隔内连按）**不算撞墙**，不该反馈，
   否则按住前进键会误报。

因此反馈点精确放在 `move()` 内部"确实被阻挡"的分支（1/2 两种），
`canMove()` 冷静期不触发。键盘/屏幕按钮共 8 个调用点完全不用改。

### 2.3 引擎层与 UI 层解耦

`Team` 在 engine 包，不能反向依赖 scene2d/VisUI。新增静态反馈入口：

```java
public class Feedback {
    public interface Listener {
        void onMoveBlocked();          // 移动被阻挡
        void onMessage(String text);   // 一般短消息（事件喊话等）
    }
    public static void setListener(Listener l);
    public static void moveBlocked();
    public static void message(String text);
}
```

- 引擎侧（Team、EventSquare、脚本动作）只调 `Feedback.xxx()`；
- UI 侧（GameScreen.initScreenObjects）注册 Listener 实现；
- 未注册（如单元测试、菜单屏）时静默 no-op。

这条通道同时服务于随机事件系统的短消息（doc-13 §4.4 的 DisplayMessage 旁路），
一个通道两个用途，避免再开静态口子。

---

## 3. 实现

### 3.1 NotificationToast（通知横幅）

`engine/feedback/NotificationToast.java`，scene2d Group：

- 背景：1x1 白纹理着色（红色 `rgba(0.55,0.12,0.12,0.85)` 撞墙 / 深蓝 `rgba(0.10,0.25,0.45,0.85)` 消息）；
- 文本：VisLabel（默认样式已由 UITheme 注入中文字体，"那个方向走不通！"直接可显）；
- 位置：屏幕水平居中、高度 54% 处（迷宫视图下半部，不挡小地图与按钮区）；
- 动画：`fadeIn(0.12s) → delay(1.4s) → fadeOut(0.4s) → hide()`；
- **节流**：显示期间再次触发会重置内容与计时（clearActions + 重新来），
  快速连撞不会叠加多条横幅；
- `setTouchable(disabled)`：纯展示，不挡任何点击。

### 3.2 震动

GameScreen 的 Listener 里对 `mazeGroup` 施加 scene2d Action 序列：

```java
mazeGroup.clearActions();
mazeGroup.addAction(Actions.sequence(
    Actions.moveBy(0, -6, 0.05f), Actions.moveBy(0, 6, 0.05f),
    Actions.moveBy(0, -3, 0.04f), Actions.moveBy(0, 3, 0.04f)));
```

纵向（前后）两段递减抖动模拟"撞一下弹回来"，全程 180ms，结束后回到原位。
`clearActions()` 防止连按时动作序列堆叠。

### 3.3 引擎侧挂点（Team.move）

```java
// If can't pass through
if (!state) {
    GameMessage.buildMessage(1);   // 遗留链路保留（未来 GameMessage 接入后自然生效）
    Feedback.moveBlocked();        // 本轮新增：toast + 震动
    return false;
}
```

---

## 4. 参数调整速查

| 想改什么 | 位置 |
| --- | --- |
| 横幅停留时长 | `NotificationToast.show()` 里 `Actions.delay(1.4f)` |
| 横幅位置（高度） | `show()` 里 `stageHeight * 0.54f` |
| 横幅最大宽度 | `LABEL_MAX_WIDTH`（640） |
| 撞墙横幅颜色 | `GameScreen` Listener `onMoveBlocked` 里 Color(0.55f,0.12f,0.12f,0.85f) |
| 震动幅度/时长 | `GameScreen` Listener 里四个 moveBy 的位移与时长 |
| 撞墙文案 | StringTable.xml `game` 段 id=1（三语都有） |

---

## 5. 测试与后续

**测试**：向任意墙面按键（W/A/S/D 或屏幕方向钮）→ 红色横幅"那个方向走不通！"
约 1.5 秒淡出 + 迷宫画面轻震；对着已确认的怪物格移动同样触发；
150ms 内疯狂连按不会刷横幅（冷却失败不反馈，横幅自身也节流）。

**后续增强（v0.6 候选）**：
1. 加一个专用 `bump.ogg` 撞墙音效（资源库目前没有语义合适的音效）；
2. GameMessage 消息系统正式接入渲染循环后，把本 toast 升级为"消息堆栈"
   （最多 3 条并存，见 GameMessage 原始设计）；
3. 怪物挡路与撞墙可区分文案（"有东西挡住了去路！"）。

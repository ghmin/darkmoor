# 模块 08 - `com.ntk.darkmoor.engine.gui.campwindows`

## 概览

营地对话框（`CampDialog`）里的各个子窗口。每个窗口继承 `BaseWindow`，由栈式导航管理。

| 类 | 文件 | 角色 |
|---|---|---|
| `MainWindow` | `MainWindow.java` | 营地主菜单 |
| `SpellWindow` | `SpellWindow.java` | 法术选择 |
| `RestPartyWindow` | `RestPartyWindow.java` | 休息 |
| `SaveGameWindow` | `SaveGameWindow.java` | 存档 |
| `LoadGameWindow` | `LoadGameWindow.java` | 读档 |
| `GameOptionsWindow` | `GameOptionsWindow.java` | 游戏选项 |
| `PreferencesWindow` | `PreferencesWindow.java` | 偏好设置 |
| `DropNPCWindow` | `DropNPCWindow.java` | NPC 离队 |
| `ScribeScrollsWindow` | `ScribeScrollsWindow.java` | 抄写卷轴 |

## 通用模式

所有子窗口都遵循这个模式：

```java
public class XxxWindow extends BaseWindow {
    public XxxWindow(CampDialog camp, GameScreenBase parent) {
        super(camp, "Window Title", parent);
        // 创建 ScreenButton、添加 listener
    }
    
    @Override
    public void loadContent() {
        // 加载需要的贴图/数据
    }
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        // 绘制窗口内容
    }
    
    @Override
    public void update(float delta) {
        super.update(delta);
        // 处理输入、更新状态
    }
}
```

退出窗口的标准做法：
```java
protected boolean exitSelected(Event event) {
    setClosing(true);  // CampDialog 下一帧会把自己从栈里弹出
    return true;
}
```

## MainWindow

**位置**：`core/src/com/ntk/darkmoor/engine/gui/campwindows/MainWindow.java`

**职责**：营地对话框的主菜单，列出所有可选项的按钮。

**按钮列表**：
- Rest → 入栈 `RestPartyWindow`
- Memorize Spells → 入栈 `SpellWindow`
- Save Game → 入栈 `SaveGameWindow`
- Load Game → 入栈 `LoadGameWindow`
- Drop NPC → 入栈 `DropNPCWindow`
- Scribe Scrolls → 入栈 `ScribeScrollsWindow`
- Game Options → 入栈 `GameOptionsWindow`
- Exit Camp → 关闭整个 `CampDialog`

## SpellWindow

**位置**：`core/src/com/ntk/darkmoor/engine/gui/campwindows/SpellWindow.java`

**职责**：法术选择窗口。

**v0.5 修复点**：line 142 的 `setTitle("Spells Available :")` 改成 `getTitleLabel().setText("Spells Available :")`（libGDX 1.10+ 后 `Dialog` 没有 `setTitle` 方法）。

**界面布局**：
- 顶部：消息文本（"X of Y remaining"）
- 中部：6 个英雄头像（2×3 网格）
- 底部：6 个法术等级按钮（1-6）+ Clear + Exit

**交互流程**：
1. 显示所有英雄
2. 点击英雄 → 选中（高亮）+ 显示"Spells Available:"标题 + 显示等级按钮
3. 点击等级 → 高亮该等级按钮
4. 点击 Clear → 清除选择
5. 点击 Exit → 关闭窗口

## RestPartyWindow

**位置**：`core/src/com/ntk/darkmoor/engine/gui/campwindows/RestPartyWindow.java`

**职责**：休息窗口，恢复 HP/MP。

**界面**：显示"Hours rested: X"+ Rest / Stop / Exit 按钮。

**v0.5 状态**：UI 已实现，但实际恢复 HP 的逻辑未完成。

## SaveGameWindow / LoadGameWindow

**位置**：`core/src/com/ntk/darkmoor/engine/gui/campwindows/SaveGameWindow.java` 和 `LoadGameWindow.java`

**职责**：存档/读档窗口。

**界面**：6 个 slot 按钮（带存档名/空）+ Save/Load/Exit 按钮。

**v0.5 状态**：UI 已实现，但实际存档/读档逻辑未完成。

## GameOptionsWindow / PreferencesWindow

**位置**：`core/src/com/ntk/darkmoor/engine/gui/campwindows/GameOptionsWindow.java` 和 `PreferencesWindow.java`

**职责**：游戏选项和偏好设置。

**可设置项**：
- 音乐开关
- HP 显示方式（条/数字）
- 语言
- 输入方案

## DropNPCWindow

**位置**：`core/src/com/ntk/darkmoor/engine/gui/campwindows/DropNPCWindow.java`

**职责**：让 NPC 离队。

**界面**：列出当前队伍里的 NPC，点击 → 确认 → 移除。

## ScribeScrollsWindow

**位置**：`core/src/com/ntk/darkmoor/engine/gui/campwindows/ScribeScrollsWindow.java`

**职责**：抄写卷轴（把卷轴上的法术抄到法术书）。

**v0.5 状态**：UI 部分实现，逻辑未完成。

## 数据流示例：从主菜单进入法术窗口

```
1. GameScreen 显示 Camp 按钮
2. 用户点击 Camp 按钮
   ↓
3. new CampDialog(game, gameScreen) → MainWindow 入栈
   ↓
4. CampDialog 加到 stage
   ↓
5. 用户点击"Memorize Spells"
   ↓
6. MainWindow 的 listener:
   campDialog.addWindow(new SpellWindow(campDialog, gameScreen))
   ↓
7. SpellWindow 入栈，成为栈顶
   ↓
8. 下一帧 CampDialog.update():
   windows.peek().update() → 更新 SpellWindow
   windows.peek().draw() → 绘制 SpellWindow（覆盖 MainWindow）
   ↓
9. 用户点击 SpellWindow 的 Exit
   ↓
10. SpellWindow.setClosing(true)
    ↓
11. 下一帧 CampDialog.update():
    while (windows.peek().isClosing()) windows.pop();
    // SpellWindow 出栈，MainWindow 重新成为栈顶
```

## v0.5 状态总览

| 窗口 | UI | 业务逻辑 |
|---|---|---|
| `MainWindow` | ✅ 完成 | ✅ 完成 |
| `SpellWindow` | ✅ 完成 | ❌ 未完成（法术施放未实现） |
| `RestPartyWindow` | ✅ 完成 | ❌ 未完成（HP 恢复未实现） |
| `SaveGameWindow` | ✅ 完成 | ❌ 未完成（存档未实现） |
| `LoadGameWindow` | ✅ 完成 | ⚠️ 部分（只能加载默认存档） |
| `GameOptionsWindow` | ✅ 完成 | ⚠️ 部分（部分设置生效） |
| `PreferencesWindow` | ✅ 完成 | ⚠️ 部分 |
| `DropNPCWindow` | ✅ 完成 | ❌ 未完成 |
| `ScribeScrollsWindow` | ✅ 完成 | ❌ 未完成 |

## 学习建议

1. 先读 `MainWindow.java`（理解窗口导航）
2. 再读 `SpellWindow.java`（理解较复杂的窗口，特别是 v0.5 修复点）
3. 然后看其他窗口（模式都类似）
4. 想加新窗口？参考 [07-engine-gui.md](07-engine-gui.md) 的 `BaseWindow` 部分

# 10. CAMP / 营地系统模块

> **覆盖**：CAMP 按钮、`CampDialog`、营地子窗口栈
> **关键类**：`CampDialog`、`BaseWindow`、`ScreenButton`、`MainWindow`、`RestPartyWindow`、`SpellWindow`、`SaveGameWindow`、`LoadGameWindow`、`GameOptionsWindow`、`PreferencesWindow`、`DropNPCWindow`、`ScribeScrollsWindow`
> **适合谁看**：想理解"点 CAMP 应该发生什么"、"为什么 CAMP 现在没反应"、"怎么修 CAMP"的人

---

## 10.1 CAMP 按钮的三个问题（v0.5 之前）

### 问题 1：CAMP 是图片，不是文本按钮

**现状**：CAMP 这四个字母是**直接画在 `Interface.png` 背景图的 sprite 0 上**的像素，由 `GameScreen.setupBackground()` 返回的 `Image` 显示。这个 `Image` 没有 `InputListener`，所以点击 CAMP 区域不会触发任何事件。

**证据**：
- `GameScreen.setupBackground()`（line 92–96）返回 `new Image(textureSet.getSprite(0))`
- `DisplayCoordinates.campButton = new Rectangle(578, 354, 62, 42)` 定义了点击区域
- `DisplayCoordinates.getCampButton()` **从未被任何代码调用**（grep 验证）
- `InterfaceCoordinates.java` 中**没有** campButton 字段

**i18n 影响**：因为 CAMP 是图片像素，无论玩家选 English / Francais / Chinese，屏幕上永远显示"CAMP"四个英文字母。这违反了项目的多语言规则。

### 问题 2：CAMP 点击无反应

**现状**：即使玩家精确点击 (578, 354) 区域，也没有任何事件触发。

**根本原因链**：
1. **没有点击目标**：CAMP 区域是背景像素，没有 scene2d actor 占据 `Rectangle(578, 354, 62, 42)`
2. **`CampDialog` 从未被实例化**：`grep "new CampDialog"` 在生产代码中**零命中**，只有 `docs/v0.5/modules/08-engine-campwindows.md` 提到过
3. **即使实例化也打不开子窗口**：`MainWindow` 等 9 个营地子窗口创建了 `ScreenButton` 但**从未调用 `getButtons().add(button)`**（所有相关行都是注释 `// getButtons().add(button);`），且 `ScreenButton` 不是 scene2d actor，不会自动响应点击

### 问题 3：营地子窗口的按钮系统损坏

**`ScreenButton` 设计**：自定义 `Button` 子类，持有 `Rectangle` 和 `EventListener`，需要外部代码**手动调用** `onSelectEntry()` 来触发 listener。

**实际使用**：
- `MessageBox.update()` 通过 `Gdx.input.isButtonPressed(Buttons.LEFT)` 轮询 + `Rectangle.contains()` 命中测试来调用 `onSelectEntry()` — 这一个能用
- `MainWindow` 等 9 个营地子窗口**没有类似的轮询代码**，所以即使按钮存在也永远不触发

---

## 10.2 CAMP 系统的设计意图（从代码与文档推断）

### 10.2.1 应有的流程

```
玩家点击 CAMP 按钮
  ↓
GameScreen 检测到点击
  ↓
new CampDialog(gameScreen, gameScreen)   ← 从未实例化
  ↓
stage.addActor(campDialog)
  ↓
CampDialog 构造函数：
  - 标题 = LanguagesManager.getString("titles", 1)  // "营地"
  - 创建 windows 栈
  - addWindow(new MainWindow(this, parent))
  ↓
CampDialog.update(delta) 每帧调用：
  - 检查栈顶窗口是否 closing → 弹出
  - 栈空 → exit()
  - 否则 → windows.peek().update(delta)
  ↓
MainWindow 构造函数：
  - 标题 = LanguagesManager.getString("titles", 2)  // "营地 :"
  - 创建 7 个 ScreenButton（Rest/Memorize/Pray/Scribe/Prefs/Options/Exit）
  - 每个按钮 addEventListener(...) 绑定 handler
  - 注释掉了 getButtons().add(button) ← 这里坏了
  ↓
玩家点击某个按钮（应该的流程）：
  - ScreenButton.onSelectEntry() 被调用  ← 从未发生
  - EventListener.handle(event) 触发
  - handler 调用 getCamp().addWindow(new XxxWindow(...))
  ↓
新窗口入栈，覆盖 MainWindow
  ↓
玩家点 Exit → setClosing(true) → 下一帧被弹出 → 回到 MainWindow
  ↓
玩家再点 Exit → CampDialog.exit() → closeAndRemoveFromParent(this)
```

### 10.2.2 营地子窗口清单

| 类 | 标题 (titles id) | 功能 | 当前状态 |
|---|---|---|---|
| `MainWindow` | 2 ("营地 :") | 主菜单，列出 7 个选项 | ❌ 按钮未注册 |
| `RestPartyWindow` | 4 ("小队休息 :") | 休息恢复 HP/法术位 | ❌ 按钮未注册 |
| `SpellWindow` | (动态) | 选择法师/牧师记忆法术 | ❌ 按钮未注册 |
| `ScribeScrollsWindow` | 5 ("抄写卷轴 :") | 法师抄写卷轴 | ❌ 按钮未注册 |
| `PreferencesWindow` | 6 ("偏好设置 :") | 游戏偏好（HPAsBar 等） | ❌ 按钮未注册 |
| `GameOptionsWindow` | 10 ("游戏选项 :") | 游戏选项（保存/读取/退出） | ❌ 按钮未注册 |
| `SaveGameWindow` | 8 ("保存游戏") | 保存到槽位 | ❌ 按钮未注册 |
| `LoadGameWindow` | 7 ("读取游戏") | 从槽位读取 | ❌ 按钮未注册（但 MainMenu 用它能工作，因为那里的代码不同） |
| `DropNPCWindow` | 9 ("移除角色") | 移除小队中的 NPC | ❌ 按钮未注册 |

### 10.2.3 StringTable 中的 Camp 相关字符串

**`titles` stringtable**：
- id=1: "Camp" / "Campement" / "营地" — `CampDialog` 标题
- id=2: "Camp :" / "Campement :" / "营地 :" — `MainWindow` 标题
- id=4: "Rest Party :" / "..." / "小队休息 :" — `RestPartyWindow` 标题
- id=5: "Scribe Scrolls :" / "..." / "抄写卷轴 :" — `ScribeScrollsWindow` 标题
- id=6: "Preferences :" / "..." / "偏好设置 :" — `PreferencesWindow` 标题
- id=7: "Load Game" / "..." / "读取游戏" — `LoadGameWindow` 标题
- id=8: "Save Game" / "..." / "保存游戏" — `SaveGameWindow` 标题
- id=9: "Drop Character" / "..." / "移除角色" — `DropNPCWindow` 标题
- id=10: "Game Options :" / "..." / "游戏选项 :" — `GameOptionsWindow` 标题
- id=11: "Quit Game" / "..." / "退出游戏" — 退出按钮文本

**`Camp` stringtable**（7 个选项）：
- id=1: "Rest party" / "..." / "小队休息"
- id=2: "Memorize Spells" / "..." / "记忆法术"
- id=3: "Pray for Spells" / "..." / "祈求法术"
- id=4: "Scribe Scrolls" / "..." / "抄写卷轴"
- id=5: "Preferences" / "..." / "偏好设置"
- id=6: "Game Options" / "..." / "游戏选项"
- (无 id=7 — Exit 用 `campui` id=1)

**`campui` stringtable**（15 个 UI 字符串）：
- id=1: "Exit" / "..." / "退出"
- id=2: "Cancel" / "..." / "取消"
- id=3: "Yes" / "..." / "是"
- id=4: "No" / "..." / "否"
- id=5: "Clear" / "..." / "清除"
- id=6: "Empty slot %d" / "..." / "空槽位 %d"
- id=7: "Hours rested : %d" / "..." / "已休息小时数 : %d"
- id=8: "%d of %d remaining." / "..." / "剩余 %d / %d"
- id=9: "Spells Available :" / "..." / "可用法术 :"
- id=10–15: 音乐/音效/血条 ON/OFF 等

---

## 10.3 v0.5 修复方案

### 10.3.1 修复 1：CAMP 改为文本按钮

在 `NavigationControlsGroup.initialize()` 中**新增**一个 `TextButton`：

```java
private TextButton campButton;

// initialize() 中：
campButton = new TextButton(
    LanguagesManager.getString("titles", 1),  // "营地"
    uiSkin,
    "transparent-font64"
);
campButton.setBounds(578, 354, 120, 50);  // 原 campButton 区域稍大
campButton.addListener(new InputListener() {
    public boolean touchDown(InputEvent e, float x, float y, int p, int b) {
        openCampDialog();
        return true;
    }
});
addActor(campButton);
```

这样：
- ✅ CAMP 是真正的文本按钮，可点击
- ✅ 文本走 i18n，中文显示"营地"
- ✅ 点击触发 `openCampDialog()`

### 10.3.2 修复 2：实例化并打开 CampDialog

`openCampDialog()` 实现：

```java
private void openCampDialog() {
    if (team == null || team.getMaze() == null) return;
    GameScreenBase parent = ...;  // 需要拿到 GameScreen 实例
    CampDialog dialog = new CampDialog(parent, parent);
    parent.getStage().addActor(dialog);
}
```

注意 `NavigationControlsGroup` 目前没有 `parent` 字段，需要：
- **方案 A**：在 `GameScreenGroup` 加 `public GameScreenBase parent;` 字段
- **方案 B**：`NavigationControlsGroup` 通过 `GameScreen.getTeam()` 反查（但 `GameScreen` 单例不暴露 `getStage()`）
- **方案 C**：把 CAMP 按钮的创建移到 `GameScreen.initScreenObjects()` 里，直接用 `this` 作为 parent

推荐**方案 C** — 最简单，不破坏现有架构。

### 10.3.3 修复 3：让营地子窗口按钮可点（部分）

**完整修复**需要把 9 个营地子窗口的 `ScreenButton` 全部换成 scene2d `TextButton`。工作量大。

**v0.5 阶段的最小修复**：在 `BaseWindow.update(delta)` 加鼠标轮询，模仿 `MessageBox.update()`：

```java
// BaseWindow.java
public void update(float delta) {
    // ... 现有 messageBox 处理 ...

    // 让 ScreenButton 响应点击（如果子类注册了的话）
    if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
        Vector2 mouse = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        // 注意：要把屏幕坐标转 stage 坐标
        parent.getStage().screenToStageCoordinates(mouse);
        for (ScreenButton btn : screenButtons) {
            if (btn.getRectangle().contains(mouse)) {
                btn.onSelectEntry();
                break;
            }
        }
    }
}
```

但 `MainWindow` 等子类**注释掉了 `getButtons().add(button)`**，所以即使 `BaseWindow` 加了轮询，按钮列表也是空的。

**v0.5 决策**：
1. ✅ **取消注释** `MainWindow` 中的 `getButtons().add(button)` 行（7 处）
2. ✅ 在 `BaseWindow` 加 `protected ArrayList<ScreenButton> screenButtons` 字段
3. ✅ `MainWindow` 等子类把 `ScreenButton` 加到这个列表（替换原来的 `getButtons().add(button)`）
4. ✅ `BaseWindow.update(delta)` 加鼠标轮询调用 `onSelectEntry()`

但完整测试 9 个子窗口需要时间。**v0.5 只修复 MainWindow 的按钮注册 + BaseWindow 轮询**，其他子窗口留待 v0.6。

### 10.3.4 修复 4：键盘 C 键也能打开营地

`KeyboardInputProcessor` 中 `Action.Camp` → `gameScreen.openCampDialog()`。

需要在 `GameScreen` 加一个 `openCampDialog()` 方法供 keyboard 和 CAMP 按钮共用：

```java
// GameScreen.java
public void openCampDialog() {
    if (team == null || team.getMaze() == null) return;
    CampDialog dialog = new CampDialog(this, this);
    stage.addActor(dialog);
}
```

---

## 10.4 修复后的 CAMP 流程

```
玩家点击 CAMP 文本按钮  (或按 C 键)
  ↓
NavigationControlsGroup 的 InputListener 触发  (或 KeyboardInputProcessor)
  ↓
gameScreen.openCampDialog()
  ↓
new CampDialog(this, this)
  ↓
CampDialog 构造：
  - 标题 = "营地"（i18n）
  - windows 栈压入 new MainWindow(this, this)
  ↓
stage.addActor(campDialog)
  ↓
Dialog 显示，标题栏"营地"可见
  ↓
（v0.5: MainWindow 的 ScreenButton 已注册到 BaseWindow.screenButtons）
（v0.5: BaseWindow.update 轮询鼠标 → 点击按钮 → onSelectEntry → handler）
  ↓
点击"小队休息" → MainWindow.restPartySelected() → getCamp().addWindow(new RestPartyWindow(...))
  ↓
RestPartyWindow 入栈，覆盖 MainWindow
  ↓
... (RestPartyWindow 的功能取决于其内部实现)
  ↓
点击"退出" → setClosing(true) → 下一帧弹出 → 回到 MainWindow
  ↓
点击"退出" → setClosing(true) → 下一帧弹出 → 栈空 → CampDialog.exit() → 关闭
```

---

## 10.5 已知遗留问题

| 问题 | 影响 | 解决思路 |
|---|---|---|
| `ScreenButton` 不是 scene2d actor | 不能用 scene2d 命中测试，需手动轮询 | 重构为 `TextButton` 子类 |
| `MainWindow` 等 9 个子窗口的按钮注册全注释掉 | 即使打开 CampDialog 也只能看到空窗口 | 取消注释 + 加 BaseWindow 轮询 |
| `CampDialog.draw()` 被注释 | 子窗口可能不显示 | 测试 BaseWindow（继承 Dialog）的默认 draw 是否够用 |
| `CampDialog` 的 `game` 字段类型是 `GameScreenBase` 而非 `GameScreen` | 限制了 API | 改为 `GameScreen` |
| `DisplayCoordinates.campButton` 仍存在但无用 | 死代码 | 删除或重新利用 |
| Interface.png 仍有 CAMP 像素 | 文本按钮会和背景图重叠 | 可裁剪 Interface.png 或用半透明背景遮挡 |

---

## 10.6 测试清单

| 测试项 | 期望结果 |
|---|---|
| 进入游戏后看屏幕底部 | 显示"营地"文本按钮（中文时） |
| 切换语言到 English | 按钮文本变为"Camp" |
| 切换语言到 Francais | 按钮文本变为"Campement" |
| 点击"营地"按钮 | 弹出 CampDialog，标题"营地" |
| 按 C 键 | 同上 |
| CampDialog 显示后点"小队休息" | RestPartyWindow 入栈显示 |
| 点 RestPartyWindow 的"退出" | 回到 MainWindow |
| 点 MainWindow 的"退出" | CampDialog 关闭 |
| 按 ESC | 关闭 CampDialog（如果打开） |

---

## 10.7 相关文档

- [09-input-system.md](09-input-system.md) — 键盘输入系统（C 键打开营地）
- [modules/07-engine-gui.md](modules/07-engine-gui.md) — `BaseWindow` / `ScreenButton` / `GUI` 等通用 GUI 类
- [modules/08-engine-campwindows.md](modules/08-engine-campwindows.md) — 9 个营地子窗口的详细清单

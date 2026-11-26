# 模块 07 - `com.ntk.darkmoor.engine.gui`

## 概览

游戏内 UI 组件——按钮、窗口、消息框等基础控件。营地子窗口（`campwindows` 包）在 [08-engine-campwindows.md](08-engine-campwindows.md) 单独讲。

| 类 | 文件 | 角色 |
|---|---|---|
| `GUI` | `GUI.java` | UI 工具类（静态方法） |
| `BaseWindow` | `BaseWindow.java` | 弹窗基类 |
| `CampDialog` | `CampDialog.java` | 营地对话框 |
| `MessageBox` | `MessageBox.java` | 消息提示框 |
| `MessageBox2` | `MessageBox2.java` | 消息提示框（v2） |
| `ScreenButton` | `ScreenButton.java` | 自定义按钮 |

## GUI

**位置**：`core/src/com/ntk/darkmoor/engine/gui/GUI.java`

**职责**：UI 工具类，全是静态方法。提供：
- 画斜面边框（bevel）
- 画矩形
- 获取共享字体

**主要 API**：
```java
// 画斜面边框（带凸/凹效果）
public static void drawDoubleBevel(Batch batch, Rectangle rect)
public static void drawDoubleBevel(Batch batch, Rectangle rect, boolean reverse)
public static void drawDoubleBevel(Batch batch, Rectangle rect, Color bg, Color light, Color dark, boolean reverse)

public static void drawSimpleBevel(SpriteBatch batch, Rectangle rect)
public static void drawSimpleBevel(SpriteBatch batch, Rectangle rect, boolean reverse)
public static void drawSimpleBevel(SpriteBatch batch, Rectangle rect, Color bg, Color light, Color dark, boolean reverse)

// 画矩形描边
public static void drawRectangle(Rectangle rect, Color color)

// 取共享字体
public static BitmapFont getDialogFont()
public static BitmapFont getMenuFont()
```

**`reverse` 参数**：true = 凹陷效果（按钮按下时），false = 凸起效果（按钮弹起时）。

## BaseWindow

**位置**：`core/src/com/ntk/darkmoor/engine/gui/BaseWindow.java`

**继承**：libGDX `Dialog`（继承 `Window` 继承 `Table` 继承 `Group` 继承 `Actor`）

**职责**：所有弹窗的基类。

**字段**：
```java
private CampDialog camp;                  // 所属营地对话框
private boolean closing;                   // 是否正在关闭
private MessageBox messageBox;             // 内嵌消息框
protected TextButton[] buttons;            // 按钮数组
protected GameScreenBase parent;           // 父屏幕
```

**主要方法**：
```java
public BaseWindow(CampDialog camp, String title, GameScreenBase parentScreen)
public void close()
public boolean isClosing()
public void setClosing(boolean c)
public void update(float delta)
public void loadContent()                  // 子类重写加载内容
public void unloadContent()                // 子类重写卸载内容
protected void closeAndRemoveFromParent(BaseWindow window)
```

**v0.5 重要说明**：libGDX 1.10+ 的 `Dialog` / `Window` 类**没有** `setTitle(String)` 方法。要改标题文本必须用：

```java
getTitleLabel().setText("new title");
```

`SpellWindow.java:142` 原来用的 `setTitle(...)` 在 v0.5 已修复。

## CampDialog

**位置**：`core/src/com/ntk/darkmoor/engine/CampDialog.java`

**职责**：营地对话框。用栈管理多个子窗口（`BaseWindow`），类似浏览器的历史记录。

**字段**：
```java
private GameScreenBase game;
private Rectangle rectangle;
private ArrayList<ScreenButton> buttons;
private ArrayDeque<BaseWindow> windows;   // 栈式管理
```

**栈式导航**：
```java
public void addWindow(BaseWindow window) {  // 入栈
    windows.push(window);
}

public void update(float delta) {
    // 清理正在关闭的窗口
    while (windows.size() > 0 && windows.peek().isClosing())
        windows.pop();
    
    if (windows.isEmpty())
        exit();
    else
        windows.peek().update(delta);  // 只更新栈顶窗口
}
```

**子窗口示例**：进入营地 → `MainWindow` 入栈 → 点击"Spell" → `SpellWindow` 入栈 → 点击"Exit" → `SpellWindow` 出栈 → 回到 `MainWindow`。

## MessageBox / MessageBox2

**位置**：`core/src/com/ntk/darkmoor/engine/gui/MessageBox.java` 和 `MessageBox2.java`

**职责**：消息提示框。`MessageBox2` 是改进版（支持多按钮）。

**主要 API**：
```java
public void show(String text)
public void update(float delta)
public boolean isClosing()
```

**典型用途**：在 `BaseWindow` 内显示"你确定要丢弃这件物品吗？"等确认对话。

## ScreenButton

**位置**：`core/src/com/ntk/darkmoor/engine/gui/ScreenButton.java`

**继承**：libGDX `Button`

**职责**：自定义按钮，比 libGDX 默认 `TextButton` 多了：
- 矩形区域（`Rectangle`）
- 鼠标悬停反应（`setReactOnMouseOver`）
- 文字颜色（`setTextColor`）

**主要 API**：
```java
public ScreenButton(String text, Rectangle rectangle)
public void addListener(EventListener listener)
public void setReactOnMouseOver(boolean b)
public void setVisible(boolean b)
public void setTextColor(Color c)
public String getText()
```

**用法示例**：
```java
ScreenButton button = new ScreenButton("Exit", new Rectangle(256, 244, 80, 28));
button.addListener(new EventListener() {
    public boolean handle(Event event) {
        return exitSelected(event);
    }
});
addActor(button);
```

## UI 资源加载

`GameScreenBase` 启动时加载 UI 皮肤：

```java
uiSkin = new Skin();
uiSkin.addRegions(new TextureAtlas(Gdx.files.internal("data/skin/uiskin.atlas")));
uiSkin.add("font48", font48);
uiSkin.add("font64", font64);
uiSkin.add("gameFont48", gameFont48);
uiSkin.load(Gdx.files.internal("data/skin/uiskin.json"));
uiSkin.remove("default-font", BitmapFont.class);
```

UI 皮肤文件位于 `android/assets/data/skin/`：
- `uiskin.atlas` - 贴图集（按钮背景等）
- `uiskin.json` - 控件样式定义
- `uiskin.png` - 贴图本身

## v0.5 状态

- `GUI` 工具类完全可用
- `BaseWindow` 可用，但子类（`campwindows/*`）部分未完成
- `CampDialog` 栈式导航可用
- `MessageBox` / `MessageBox2` 可用
- `ScreenButton` 可用

## 学习建议

1. 先读 `GUI.java`（短，全是静态方法）
2. 再读 `ScreenButton.java`（理解自定义控件）
3. 再读 `BaseWindow.java`（理解弹窗基类）
4. 再读 `CampDialog.java`（理解栈式导航）
5. `MessageBox` / `MessageBox2` 看一个就够

读完这些再去看 [08-engine-campwindows.md](08-engine-campwindows.md)，理解具体子窗口的实现。

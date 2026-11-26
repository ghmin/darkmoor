# 模块 01 - 顶层包 `com.ntk.darkmoor`

## 概览

这是项目的"门面"包，包含 libGDX 应用入口和所有屏幕类。

| 类 | 文件 | 角色 |
|---|---|---|
| `DarkmoorGame` | `DarkmoorGame.java` | 程序入口，单例，管理生命周期 |
| `ScreenManager` | `ScreenManager.java` | 屏幕切换工具 |
| `GameScreenBase` | `GameScreenBase.java` | 屏幕抽象基类 |
| `MainMenu` | `MainMenu.java` | 主菜单屏幕 |
| `GameScreen` | `GameScreen.java` | 地牢探索主屏幕 |
| `CharGen` | `CharGen.java` | 角色生成屏幕（未完成） |
| `IntroScreen` | `IntroScreen.java` | 开场剧情屏幕 |
| `OptionsWindow` | `OptionsWindow.java` | 选项窗口 |

## DarkmoorGame

**位置**：`core/src/com/ntk/darkmoor/DarkmoorGame.java`

**继承**：`com.badlogic.gdx.Game`（libGDX 内置类，实现了 `ApplicationListener`）

**职责**：
- 程序启动时初始化（`create()`）
- 加载启动资源（设置 + 默认存档）
- 创建 `SpriteBatch` 和 `OrthographicCamera`
- 切到主菜单屏幕
- 提供 `getInstance()` 全局单例访问
- 提供 `submitTask(AsyncTask)` 后台任务接口（用于异步加载地牢）

**关键常量**：
```java
public static final int GAME_HEIGHT = 400;     // 游戏内部坐标系高
public static final int GAME_WIDTH = 640;      // 游戏内部坐标系宽
public static final int DISPLAY_HEIGHT = 720;  // 窗口高
public static final int DISPLAY_WIDTH = 1280;  // 窗口宽
public static final String DATA_ASSET_FOLDER = "./data";
```

**生命周期**：

```
create()
  ↓
  instance = this
  executor = new AsyncExecutor(2)
  loadStartupData()
    ↓
    Settings.loadSettings(...)        // 读 settings.properties
    loadSaveGameFile(...)             // 读 savegame.xml
    Resources.loadGameStartupResources()  // 启动 AssetManager
  camera = new OrthographicCamera()
  batch = new SpriteBatch()
  setScreen(getMainMenu())            // 进入主菜单

每帧 render() 由 Game 转发给当前 Screen
  ↓
pause() / resume() → 转发 + 暂停/恢复音乐
dispose()
  ↓
  gameScreen.dispose()
  mainMenu.dispose()
  stopMusic()
  batch.dispose()
  Resources.unloadResources()
```

## ScreenManager

**位置**：`core/src/com/ntk/darkmoor/ScreenManager.java`

**职责**：屏幕间切换的静态工具。

**主要方法**：
```java
public static void init(DarkmoorGame parentGame)
public static void showIntroScreen()
public static void showMainMenu()
public static void showGameScreen()
public static void exitScreen()
public static void addScreen(GameScreenBase screen)
```

**注意**：当前项目里很多地方直接用 `game.setScreen(...)` 而不走 `ScreenManager`，这是历史遗留，未来可统一。

## GameScreenBase

**位置**：`core/src/com/ntk/darkmoor/GameScreenBase.java`

**继承**：`com.badlogic.gdx.ScreenAdapter`（libGDX 提供的空实现适配器），实现 `Disposable`

**职责**：所有屏幕的抽象基类，封装：
- `Stage stage` - 场景图（UI 容器）
- `Skin uiSkin` - UI 皮肤
- `setupStage()` / `setupButtons()` 模板方法（子类实现）

**关键代码**：
```java
protected void setupStage() {
    stage = new Stage(new StretchViewport(DISPLAY_WIDTH, DISPLAY_WIDTH));
    // 注意：上面这个 viewport 高度用了 DISPLAY_WIDTH，疑似 bug，v0.5 不修
    Image background = setupBackground();
    if (background != null) stage.addActor(background);
    setupButtons(uiSkin);
    Gdx.input.setInputProcessor(stage);
}

public void render(float delta) {
    Display.clearBuffers();
    stage.act(Gdx.graphics.getDeltaTime());
    stage.draw();
}

public void resize(int width, int height) {
    stage.getViewport().update(width, height, true);
}
```

**子类要做的**：
1. 在构造函数里调 `super(game)`
2. 重写 `loadContent()` 加载资源
3. 重写 `setupBackground()` 返回背景图（可选）
4. 重写 `setupButtons(Skin)` 添加按钮（可选）
5. 重写 `update()` 处理游戏逻辑（可选）

## MainMenu

**位置**：`core/src/com/ntk/darkmoor/MainMenu.java`

**职责**：主菜单屏幕。

**UI 布局**：
- 背景：`mainmenu.png`
- 三个按钮（垂直排列）：
  - Load Game → 加载存档进入游戏
  - Options → 弹出选项窗口
  - Exit → 退出程序

**关键代码**：
```java
buttons[0].addListener(new InputListener() {
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        darkmoorGame.loadGameSlot(0);  // 加载第 0 个存档槽
        return true;
    }
});
```

## GameScreen

**位置**：`core/src/com/ntk/darkmoor/GameScreen.java`

**职责**：地牢探索主屏幕。

**关键组件**：
- `MazeGroup` - 第一人称地牢视图（屏幕上方）
- `HeroInterfaceGroup` - 小队头像栏（屏幕下方）
- `NavigationControlsGroup` - 移动控制按钮
- `StatisticsGroup` - 数值面板

**主循环**：
```
render(delta)
  ↓
  update(delta)        // 调用各 Group 的 update()
  draw()               // 调用 stage.draw() 让各 Group 自绘
```

**输入处理**：通过 `NavigationControlsGroup` 上的按钮点击触发 `Team.moveForward()` / `Team.turnLeft()` 等。

## CharGen

**位置**：`core/src/com/ntk/darkmoor/CharGen.java`

**职责**：角色生成屏幕。**v0.5 状态：部分实现，流程未走通。**

**状态机**：
```
SelectHero     → 选已有英雄加入小队
  ↓
SelectRace     → 选种族
  ↓
SelectClass    → 选职业
  ↓
SelectAbilities → 滚动属性
  ↓
SelectPortrait  → 选头像
  ↓
Confirm        → 确认创建
```

**v0.5 修复点**：line 517 的 `font.draw(batch, str, x, y, w, h)` 改成了 `font.draw(batch, str, x, y, w, Align.center, false)`。

## IntroScreen

**位置**：`core/src/com/ntk/darkmoor/IntroScreen.java`

**职责**：开场剧情屏幕，显示 Waterdeep 城主召唤英雄的对话。

**v0.5 状态**：部分实现，可能未走到。

## OptionsWindow

**位置**：`core/src/com/ntk/darkmoor/OptionsWindow.java`

**职责**：选项窗口（弹窗形式，不是独立屏幕）。

**继承**：`BaseWindow`（继承 libGDX `Dialog`）

**可设置项**：
- 音乐开关
- HP 显示方式（条/数字）
- 语言
- 输入方案（QWERTY/AZERTY）

## 学习建议

新手想读懂这个包，建议顺序：

1. 先看 `DarkmoorGame.java`（约 200 行，理解生命周期）
2. 再看 `GameScreenBase.java`（约 150 行，理解屏幕基类）
3. 再看 `MainMenu.java`（约 200 行，理解最简单的具体屏幕）
4. 最后看 `GameScreen.java`（最复杂，理解游戏主屏）

读完这 4 个类，应该就能上手改 UI 了。

# 05 - libGDX 速览（给 Java 新手）

如果你以前没碰过 libGDX，这一页能让你在 10 分钟内看懂 Darkmoor 的代码风格。libGDX 是 Java 老牌跨平台游戏框架，可以一套代码同时跑桌面/Android/iOS/Web。

## 核心概念

### 1. ApplicationListener - 程序生命周期

libGDX 的"主循环"由框架驱动，你只需要实现这几个回调：

```java
public class DarkmoorGame extends Game {  // Game 实现了 ApplicationListener
    public void create()  { /* 程序启动时调用一次 */ }
    public void render()  { /* 每帧调用（默认 60 FPS） */ }
    public void pause()   { /* 失焦/最小化时 */ }
    public void resume()  { /* 重新获焦时 */ }
    public void dispose() { /* 程序退出时 */ }
    public void resize(int w, int h) { /* 窗口尺寸变化时 */ }
}
```

`Game` 类比直接实现 `ApplicationListener` 多了一个能力：可以管理多个 `Screen`，自动把 `render`/`resize` 等转发给当前 screen。

### 2. Screen - 屏幕

一个屏幕代表一个独立的"场景"，比如主菜单、游戏、角色生成：

```java
public class MainMenu extends GameScreenBase {  // GameScreenBase 继承 ScreenAdapter
    public void show()   { /* 进入这个屏幕时 */ }
    public void render(float delta) { /* 每帧绘制，delta 是上一帧耗时（秒） */ }
    public void hide()   { /* 离开屏幕时 */ }
}
```

切换屏幕：

```java
game.setScreen(new GameScreen(game));
```

### 3. Gdx - 全局门面

`Gdx` 是一个静态类，所有平台相关的子系统都从这里访问：

```java
Gdx.graphics.getWidth()    // 屏幕宽
Gdx.graphics.getHeight()   // 屏幕高
Gdx.graphics.getDeltaTime() // 上一帧耗时（秒）
Gdx.gl.glClearColor(r,g,b,a)  // OpenGL 清屏
Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
Gdx.files.internal("data/x.png")  // 读 assets 里的文件
Gdx.audio.newMusic(file)   // 创建音乐
Gdx.audio.newSound(file)   // 创建音效
Gdx.input.getX()           // 鼠标 X 坐标
Gdx.input.isKeyPressed(Keys.W)  // W 键是否按下
Gdx.app.exit()             // 退出程序
Gdx.app.debug("tag", "msg")   // 日志
```

### 4. FileHandle - 文件抽象

不管在桌面还是 Android，访问资源都用 `FileHandle`：

```java
FileHandle fh = Gdx.files.internal("data/mainmenu.png");
// internal = 在 classpath / assets 里
// local    = 应用私有目录
// external = 用户外部存储
// absolute = 绝对路径
```

Darkmoor 里所有游戏资源都在 `android/assets/data/`，访问时写 `Gdx.files.internal("data/xxx.png")`。

### 5. Texture / Sprite / SpriteBatch - 2D 绘制

- `Texture` = 加载到显存的一张图
- `Sprite` = Texture 的一部分（可变换位置/旋转/翻转）
- `SpriteBatch` = 批量绘制 sprite 的工具

典型绘制：

```java
SpriteBatch batch = new SpriteBatch();
Texture tex = new Texture("player.png");

// 在 render() 里：
batch.begin();
batch.draw(tex, 100, 200);   // 把 tex 画到 (100,200)
batch.end();
```

> **注意**：`begin()` 和 `end()` 之间不能调用 OpenGL 直接命令。

### 6. Stage / Actor / Group - 场景图 UI

libGDX 的 scene2d 提供了一套类似 DOM 的 UI 系统：

- `Actor` = 屏幕上一个对象（有 x/y/width/height/rotation）
- `Group` = Actor 容器，可以嵌套
- `Stage` = 顶层容器，负责派发输入事件和绘制

```java
Stage stage = new Stage(new StretchViewport(1280, 720));
Gdx.input.setInputProcessor(stage);  // 让 stage 接收输入

Image img = new Image(texture);
img.setPosition(100, 100);
stage.addActor(img);

// 在 render() 里：
stage.act(delta);   // 更新逻辑
stage.draw();       // 绘制
```

Darkmoor 里 `MazeGroup`、`HeroInterfaceGroup` 等都是 `Group` 的子类，被加到 `GameScreenBase` 的 `Stage` 里。

### 7. Skin - UI 皮肤

`Skin` 是 libGDX UI 控件（Button/Label/Window 等）的样式集合，从 JSON + atlas 加载：

```java
Skin skin = new Skin();
skin.addRegions(new TextureAtlas(Gdx.files.internal("data/skin/uiskin.atlas")));
skin.load(Gdx.files.internal("data/skin/uiskin.json"));
```

### 8. AssetManager - 异步资源加载

加载大量贴图/字体会卡 UI 线程，`AssetManager` 帮你后台加载：

```java
AssetManager am = new AssetManager();
am.load("data/player.png", Texture.class);
am.load("data/font.fnt", BitmapFont.class);
am.finishLoading();   // 阻塞到全部加载完
Texture tex = am.get("data/player.png", Texture.class);
```

Darkmoor 里 `Resources` 类包装了 `AssetManager`。

### 9. InputListener - 输入处理

```java
actor.addListener(new InputListener() {
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        // 鼠标/手指按下
        return true;  // 返回 true 才会收到 touchUp
    }
    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
        // 抬起
    }
});
```

### 10. BitmapFont / GlyphLayout - 文字绘制

```java
BitmapFont font = new BitmapFont();   // 默认字体
font.setColor(Color.WHITE);
font.draw(batch, "Hello", 100, 200);  // 在 (100,200) 画文字

// 计算文字宽高（v0.5 修复后的写法）
GlyphLayout layout = new GlyphLayout();
layout.setText(font, "Hello");
float w = layout.width;
float h = layout.height;
```

> **历史坑**：libGDX 1.5 时代用 `BitmapFont.TextBounds` + `font.getBounds(text)`，1.10 之后这个 API 被删了，必须改用 `GlyphLayout`。

### 11. Viewport - 屏幕适配

不同设备分辨率不同，`Viewport` 帮你处理缩放：

```java
// 拉伸适配（忽略宽高比，可能变形）
new StretchViewport(1280, 720)

// 保持比例，加黑边
new FitViewport(1280, 720)

// 保持比例，多出来的部分裁掉
new ExtendViewport(1280, 720)
```

Darkmoor 在 `GameScreenBase` 里用 `StretchViewport`，因为游戏内部坐标都按 1280×720 算。

## Darkmoor 里的 libGDX 模式

读完上面后，看 Darkmoor 时注意这些"套路"：

1. **入口套路**：`DesktopLauncher` → `Lwjgl3Application` → `DarkmoorGame`
2. **生命周期**：`DarkmoorGame.create()` 加载资源 + 切到主菜单 → `MainMenu.render()` 每帧画菜单 → 用户点 Load → `setScreen(GameScreen)` → `GameScreen.render()` 每帧画地牢
3. **场景图套路**：每个屏幕有一个 `Stage`，`Stage` 里加若干 `Group`（如 `MazeGroup`、`HeroInterfaceGroup`）
4. **资源套路**：所有贴图都通过 `Resources.getAssetManager().load(...)` 加载，需要时 `get(...)`
5. **字体套路**：用 `FreeTypeFontGenerator` 从 TTF 生成 `BitmapFont`，放到 `Skin` 里供 UI 控件使用

## 推荐学习资料

- **libGDX 官方 Wiki**：https://libgdx.com/wiki/ — 入门教程、API 索引
- **libGDX 源码**：https://github.com/libgdx/libgdx — 看 API 不清楚时直接读源码
- **Desktop 后端 LWJGL3**：https://www.lwjgl.org/ — 如果要碰底层 OpenGL
- **Scene2D UI 教程**：https://github.com/libgdx/libgdx/wiki/Scene2d — UI 控件

## 下一步

回到 [README.md](README.md) 选一个子模块深入。

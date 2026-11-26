# 模块 03 - `com.ntk.darkmoor.resource`

## 概览

资源加载层，包装 libGDX 的 `AssetManager`，统一管理贴图、字体、动画的加载与缓存。

| 类 | 文件 | 角色 |
|---|---|---|
| `Resources` | `Resources.java` | 全局资源门面（静态类） |
| `TextureSet` | `TextureSet.java` | 贴图集（一张大图 + 切割元数据） |
| `TextureMetadata` | `TextureMetadata.java` | 单个 tile 的元数据 |
| `GraphicAssets` | `GraphicAssets.java` | 通用图形资产 |
| `AnimationAssets` | `AnimationAssets.java` | 动画资产 |
| `DecorationAssets` | `DecorationAssets.java` | 装饰物资产 |
| `ItemAssets` | `ItemAssets.java` | 物品图标资产 |
| `ResourceUtility` | `ResourceUtility.java` | XML 读取工具 |
| `ResizableRectangle` | `ResizableRectangle.java` | 可缩放矩形（UI 用） |
| `DungeonLoadingTask` | `DungeonLoadingTask.java` | 后台加载地牢任务 |

## Resources

**位置**：`core/src/com/ntk/darkmoor/resource/Resources.java`

**职责**：全局资源管理器，所有贴图/字体加载都通过它。

**关键 API**：
```java
// 启动时调用一次
public static void loadGameStartupResources()  // 加载主菜单资源
public static void loadGameResources()          // 进入游戏时加载地牢资源
public static void unloadResources()            // 退出时卸载

// 取资源
public static AssetManager getAssetManager()
public static TextureSet loadSharedTextureSetAsset(String name)
public static Texture createTextureAsset(String name)
public static Item getItemAsset(String name)
public static BitmapFont createFontAsset(String name)

// 路径配置
public static String getResourcePath()
public static void setResourcePath(String path)
```

**内部状态**：
```java
private static AssetManager assetManager;     // libGDX 资源管理器
private static BitmapFont font48, font64, gameFont48;  // 共享字体
private static Dungeon dungeon;                // 当前地牢
```

**字体加载方式**（v0.5 关键部分）：
```java
FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
    Gdx.files.internal(FONT_PATH + DEVILSUMMONEREXPAND_TTF));
FreeTypeFontParameter parameter = new FreeTypeFontParameter();
parameter.size = 48;
font48 = gen.generateFont(parameter);
gen.dispose();
```

**v0.5 注意点**：
- 历史代码里的 `BitmapFont.TextBounds` 和 `getBounds()` 已删除，调用方需改用 `GlyphLayout`（已在 `GUIScriptButton.java` 修复）
- `AssetManager` 错误处理在 libGDX 1.10+ 默认抛异常（以前静默记日志）

## TextureSet

**位置**：`core/src/com/ntk/darkmoor/resource/TextureSet.java`

**职责**：贴图集——一张大 PNG + 元数据，把图切成多个 Sprite。

**字段**：
```java
private String name;
private String textureFile;            // 大图文件名
private Texture texture;               // 加载到显存的大图
private Sprite[] spriteCache;          // 切好的 sprite 数组
private Sprite[] xFlippedSpriteCache;  // 水平翻转版本（避免运行时翻转影响其他实例）
private Map<Integer, TextureMetadata> metadata;  // tile id → 元数据
```

**主要方法**：
```java
public TextureSet(int capacity)
public Sprite getSprite(int id)            // 取第 id 个 sprite
public Sprite getXFlippedSprite(int id)    // 取水平翻转版本
public void loadTexture()
public void dispose()
```

**XML 配置**（`TextureSet.xml`）：
```xml
<tileset name="wall-forest" texture="wall-forest.png">
    <tile id="0" x="0"   y="0" width="64" height="64" />
    <tile id="1" x="64"  y="0" width="64" height="64" />
    <tile id="2" x="128" y="0" width="64" height="64" effect="FlipHorizontally" />
    ...
</tileset>
```

**用法示例**：
```java
TextureSet wallSet = Resources.loadSharedTextureSetAsset("wall-forest");
Sprite wall = wallSet.getSprite(5);   // 取第 5 个 tile
batch.draw(wall, x, y);
```

## TextureMetadata

**位置**：`core/src/com/ntk/darkmoor/resource/TextureMetadata.java`

**职责**：单个 tile 的元数据（位置、尺寸、翻转效果）。

**字段**：
```java
private int id;
private int x, y;            // 在大图里的位置
private int width, height;   // 尺寸
private SpriteEffects effects;  // 翻转效果
```

## GraphicAssets / AnimationAssets / DecorationAssets / ItemAssets

这四个类的模式很相似，都是从对应 XML 加载数据并缓存：

| 类 | XML 文件 | 内容 |
|---|---|---|
| `GraphicAssets` | （内部） | 通用图形 |
| `AnimationAssets` | `Animation.xml` | 动画帧序列 |
| `DecorationAssets` | `DecorationSet.xml` | 装饰物（火把、雕像等） |
| `ItemAssets` | `Item.xml` | 物品图标和属性 |

**通用接口模式**：
```java
public void load(Element root)         // 从 XML 加载
public T get(String name)              // 按名取
public void dispose()
```

## ResourceUtility

**位置**：`core/src/com/ntk/darkmoor/resource/ResourceUtility.java`

**职责**：XML 读取工具。

**主要方法**：
```java
public static Element extractRootElement(String fileName)  // 解析 XML 根元素
public static boolean isStandaloneMode()                    // 是否脱离 libGDX 运行（如 JUnit 测试）
```

**`isStandaloneMode()` 的用途**：在 JUnit 测试里没有 libGDX 的 `Gdx.files`，需要从文件系统直接读。这个方法判断 `Gdx.files` 是否为 null。

## ResizableRectangle

**位置**：`core/src/com/ntk/darkmoor/resource/ResizableRectangle.java`

**职责**：继承 libGDX `Rectangle`，加了"原始尺寸"字段，方便 UI 缩放。

## DungeonLoadingTask

**位置**：`core/src/com/ntk/darkmoor/resource/DungeonLoadingTask.java`

**职责**：实现 libGDX `AsyncTask<Dungeon>`，在后台线程加载地牢数据。

**调用方**：
```java
// 在 DarkmoorGame.java:
public void submitTask(AsyncTask<Dungeon> task) {
    executor.submit(task);
}

// 在 Resources.java:
DarkmoorGame.getInstance().submitTask(
    new DungeonLoadingTask(dungeonName));
```

**为什么需要异步**：地牢数据包含大量贴图和 XML，同步加载会卡 UI 几秒钟，用户会以为程序崩了。异步加载 + 主菜单过渡能让用户体验顺滑。

## 数据流

```
启动：
  Resources.loadGameStartupResources()
    ↓
    new AssetManager()
    assetManager.load("mainmenu.png", Texture.class)
    initFonts()  // 用 FreeType 生成 3 个 BitmapFont
    assetManager.finishLoading()

进入游戏：
  Resources.loadGameResources()
    ↓
    submitTask(DungeonLoadingTask)  // 后台加载
    assetManager.load("items.png", Texture.class, param)
    assetManager.load("Heads.png", Texture.class, param)
    ... // 多个贴图
    assetManager.finishLoading()

退出：
  Resources.unloadResources()
    ↓
    assetManager.dispose()
    dungeon.dispose()
```

## v0.5 修复记录

- 移除了 `GUIScriptButton.java` 里对 `BitmapFont.TextBounds` / `getBounds()` 的依赖，改用 `GlyphLayout`
- 其他资源类没有破坏性 API 变化，正常使用

## 学习建议

1. 先读 `Resources.java`（约 250 行，理解 AssetManager 用法）
2. 再读 `TextureSet.java`（约 200 行，理解贴图集）
3. 再读 `ResourceUtility.java`（很短，理解 XML 读取）
4. 最后看 4 个 `*Assets` 类（模式都一样，看一个就够）

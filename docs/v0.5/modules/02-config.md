# 模块 02 - `com.ntk.darkmoor.config`

## 概览

游戏配置、存档、多语言、日志。这个包**不依赖** libGDX 的渲染部分，主要做数据读写。

| 类 | 文件 | 角色 |
|---|---|---|
| `Settings` | `Settings.java` | 用户设置（单例） |
| `SaveGame` | `SaveGame.java` | 存档文件（最多 6 个 slot） |
| `SaveGameSlot` | `SaveGameSlot.java` | 单个存档槽 |
| `LanguagesManager` | `LanguagesManager.java` | 多语言管理（单例） |
| `Log` | `Log.java` | 日志工具 |
| `GameLanguage` | `GameLanguage.java` | 语言枚举 |
| `GameMechanics` | `GameMechanics.java` | 游戏规则常量 |
| `InputType` | `InputType.java` | 输入方式枚举 |

## Settings

**位置**：`core/src/com/ntk/darkmoor/config/Settings.java`

**职责**：读写 `settings.properties` 文件，管理用户偏好。

**字段**（对应 properties 里的 key）：
```java
inputscheme  → String (qwerty / azerty)
language     → String (English / French / ...)
HPAsBar      → boolean
Music        → boolean
Effects      → boolean
FullScreen   → boolean
saveSlots    → int (默认 6)
```

**单例访问**：
```java
Settings settings = Settings.loadSettings(fileHandle);  // 加载
Settings.getLastLoadedInstance();                        // 获取已加载的实例
settings.saveSettings();                                 // 保存
```

**默认 properties 文件**：`android/assets/data/settings.properties`

## SaveGame

**位置**：`core/src/com/ntk/darkmoor/config/SaveGame.java`

**职责**：管理存档文件，最多 6 个 slot。

**XML 结构**：
```xml
<savegame>
    <slot id="0">
        <name>My Party</name>
        <dungeon>Temple of Darkmoon</dungeon>
        <!-- 英雄、物品、位置等数据 -->
    </slot>
    <slot id="1">...</slot>
    ...
</savegame>
```

**主要方法**：
```java
public SaveGame(String filename)
public SaveGameSlot getSlot(int id)        // 取第 N 个槽
public boolean isSlotEmpty(int id)         // 槽是否为空
public boolean load()                       // 从 XML 加载
public boolean save() throws IOException    // 写回 XML
public String getDungeonName()              // 取当前地牢名
```

**v0.5 状态**：能加载预定义存档（`savegame.xml`），但保存功能未完整。

## SaveGameSlot

**位置**：`core/src/com/ntk/darkmoor/config/SaveGameSlot.java`

**职责**：单个存档槽的数据。

**字段**：
```java
private String name;          // 存档名
private String dungeonName;   // 地牢名
// 其他游戏状态字段
```

## LanguagesManager

**位置**：`core/src/com/ntk/darkmoor/config/LanguagesManager.java`

**职责**：多语言管理。从 `StringTable.xml` 加载所有字符串，按当前语言查询。

**单例访问**：
```java
LanguagesManager.getInstance(stringTableFile);  // 初始化
LanguagesManager.getInstance();                  // 取实例
LanguagesManager.getString("Chargen", 1);        // 取 "Chargen" 分区 id=1 的字符串
```

**XML 结构**：
```xml
<bank>
    <stringtable name="Intro">
        <default name="English" />
        <language name="English">
            <string id="1">We, the Lords of Waterdeep...</string>
            <string id="2">Give call to the heroes...</string>
        </language>
        <language name="French">
            <string id="1">Nous, les Seigneurs de Waterdeep...</string>
        </language>
    </stringtable>
    <stringtable name="Chargen">
        ...
    </stringtable>
</bank>
```

**用法示例**（在 `CharGen.java:517`）：
```java
font.draw(batch, LanguagesManager.getString("Chargen", 1), 304, 160, 300, Align.center, false);
```

这会从 "Chargen" 分区取 id=1 的字符串，按当前语言返回。

## Log

**位置**：`core/src/com/ntk/darkmoor/config/Log.java`

**职责**：日志工具，包装 libGDX 的 `Gdx.app.debug/error`。

**主要方法**：
```java
public static void debug(String tag, String message, Object... args)
public static void error(String tag, String message, Object... args)
```

**用法**：
```java
Log.debug(Dungeon.TAG, "Loaded maze %s in %d ms", mazeName, elapsedMs);
Log.error(Team.TAG, "Hero %s not found", heroName);
```

> 日志默认不输出 debug 级别，需要 `Gdx.app.setLogLevel(Application.LOG_DEBUG)` 开启。

## GameLanguage

**位置**：`core/src/com/ntk/darkmoor/config/GameLanguage.java`

**职责**：语言枚举，列出支持的语言（English / French / Italian / German / Spanish）。

## GameMechanics

**位置**：`core/src/com/ntk/darkmoor/config/GameMechanics.java`

**职责**：游戏规则常量。例如：
- 最大英雄数
- 最大等级
- 经验值表
- 战斗规则

**v0.5 状态**：大部分常量未填充。

## InputType

**位置**：`core/src/com/ntk/darkmoor/config/InputType.java`

**职责**：输入方式枚举（QWERTY / AZERTY），影响键盘映射。

## 数据流

```
启动时：
  Settings.loadSettings() → settings.properties → Settings 单例
  LanguagesManager.getInstance() → StringTable.xml → LanguagesManager 单例
  new SaveGame("savegame.xml") → savegame.xml → SaveGame 实例
                                          ↓
                            DarkmoorGame.savedGame

进入游戏后：
  SaveGame.getSlot(0).getDungeonName() → "Temple of Darkmoon"
  Resources.loadGameResources() 加载该地牢的数据
```

## 学习建议

1. 先读 `Settings.java`（约 200 行，理解 properties 文件读写）
2. 再读 `SaveGame.java` + `SaveGameSlot.java`（理解 XML 存档）
3. 再读 `LanguagesManager.java`（理解多语言查询）
4. `Log.java` 是最简单的，最后看

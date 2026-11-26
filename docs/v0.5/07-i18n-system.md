# 07 - 多语言（i18n）系统详解

> 本文档是 v0.5 阶段对项目多语言系统的完整解构，同时记录了本次汉化改造所做的全部变更。

## 1. 系统总览

Darkmoor 的多语言系统由三部分组成：

```
┌───────────────────────────┐
│ StringTable.xml           │  ← 所有可翻译文本的数据源（XML）
│ (android/assets/data/     │
│  desktop/resources/data/) │
└────────────┬──────────────┘
             │ 启动时加载
             ▼
┌───────────────────────────┐
│ LanguagesManager          │  ← 单例，解析 XML → HashMap，提供查询 API
│ (config/LanguagesManager) │
└────────────┬──────────────┘
             │ getString(section, key)
             ▼
┌───────────────────────────┐
│ UI 层调用方                │  ← MainMenu / OptionsWindow / 各 camp 窗口 / GameMessage
└───────────────────────────┘
```

语言选择由 `Settings.language`（`settings.properties` 里的 `language=...`）驱动，
**三个数据副本必须同步修改**（见第 8 节）：
- `android/assets/data/StringTable.xml`（安卓 + 本地开发的运行时资源）
- `desktop/resources/data/StringTable.xml`（desktop 模块打包资源）
- `core/test/test-resources/StringTable.xml`（JUnit 测试资源）

## 2. StringTable.xml 格式规范

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<bank>
    <stringtable name="main">              <!-- 分区（section）名 -->
        <default name="English" />         <!-- 缺省语言标记（仅声明，查询时不使用） -->
        <language name="English">          <!-- 语言名 = 查询键的第一段 -->
            <string id="1">LOAD GAME IN PROGRESS</string>   <!-- id = 查询键的第三段 -->
            <string id="2">START A NEW PARTY</string>
        </language>
        <language name="Francais">...</language>
        <language name="Chinese">...</language>
    </stringtable>
    <!-- 更多 <stringtable> 分区 -->
</bank>
```

### 查询键（lookup key）

`LanguagesManager.loadMessages()` 把每条字符串放进 `HashMap`，键的格式是：

```
<language>|<section>|<id>
例如："Chinese|main|1" → "读取进行中的游戏"
```

⚠️ **关键规则：`<language name="...">` 属性值必须与 `LanguagesManager.availableLanguages`
数组中的字符串完全一致**，否则查询永远 miss（返回 "Key %d not found in string table"）。

### 文本中的换行

字符串里可以直接写 `<br />` 标记（与原版英文/法文保持一致）。
注意：libGDX 的 `BitmapFont.draw()` **不会**把 `<br />` 解析成换行——它会原样画出来。
这是原项目的历史行为；真正需要换行的地方（如 Chargen 提示语）沿用原格式即可。

## 3. LanguagesManager API

**位置**：`core/src/com/ntk/darkmoor/config/LanguagesManager.java`

| 方法 | 说明 |
|---|---|
| `getInstance(String file)` | 单例获取；传入的文件与当前实例不同时会重新加载 |
| `getString(String section, int key)` | 静态便捷方法，用当前语言查询（最常用） |
| `getString(String lang, String section, int key)` | 实例方法，指定语言查询 |
| `buildMessage(String section, int id, Object... args)` | 查询后用 `String.format` 填充占位符 |
| `setLanguage(String)` / `getLanguage()` | 当前语言（内存态，不改 settings 文件） |
| `getAvailableLanguages()` | 可用语言数组 |
| `getAllCharacters()` | **v0.5 新增**：返回字表里出现过的全部字符（用于字体子集） |

### 两个"两处真相"陷阱

1. **`Settings.language` 与 `LanguagesManager.language` 是两个独立字段。**
   构造函数从 Settings 读一次初始值；之后改 Settings **不会**自动同步到 manager。
   `OptionsWindow` 切换语言时必须**同时**调用
   `Settings.setLanguage(next)` 和 `stringTable.setLanguage(next)`，
   否则 `getString(...)` 会继续用旧语言（v0.5 已在 OptionsWindow 修复）。

2. **`availableLanguages` 数组里的 "French" 与 XML 里的 "Francais" 不一致（历史 bug）。**
   原代码数组是 `{"English","French"}`，但法文分区的 name 属性是 `"Francais"`，
   导致切到"French"后所有法文查询 miss。v0.5 已改为 `{"English","Francais","Chinese"}`。
   顺带说明：`Spells` 分区的法文 name 写的是 `"French"`（又一个不一致），
   因此法文的法术名查询仍然 miss——这是遗留问题，见第 10 节。

## 4. 占位符格式：必须用 `%s`/`%d`，不是 `{0}`

`buildMessage()` 底层是 `String.format(message, args)`，所以带参数的字符串必须用
`java.util.Formatter` 语法：

```xml
<!-- 正确 -->
<string id="2">%s taken.</string>
<!-- 错误（原项目的历史 bug，占位符永远不会被替换） -->
<string id="2">{0} taken.</string>
```

v0.5 已把 `game` 分区 id 2 从 `{0} taken.` 修正为 `%s taken.`（中/英/法三语同步修正）。
新增字符串一律使用 `%d`（整数）和 `%s`（字符串）。

## 5. 字体渲染管线（为什么中文需要特殊处理）

### 5.1 问题的根源

项目用 `gdx-freetype` 在运行时把 TTF 烘焙成 `BitmapFont` 贴图：

```
TTF 字体文件 → FreeTypeFontGenerator.generateFont(parameter) → BitmapFont（含字符集贴图）
```

关键约束：**只有 `parameter.characters` 里列出的字符才会被烘焙进贴图**。
libGDX 默认值 `FreeTypeFontGenerator.DEFAULT_CHARS` 只有 ASCII 可打印字符。

原项目的三个字体全部来自纯拉丁字体：

| Skin 里的名字 | 生成自 | 用途 |
|---|---|---|
| `font48` | `devilsummonerexpand.ttf`（28KB，拉丁） | 菜单按钮（transparent-font48 样式） |
| `font64` | 同上 | 选项窗口复选框（transparent-font64） |
| `gameFont48` | `tahoma.ttf`（拉丁，无 CJK） | 游戏内文字（GUI.getMenuFont 等） |
| `default-font` | `default.fnt`（预烘焙位图字体） | Window 标题、Label 等 |

即使把字符串全部翻成中文，用这些字体也只会画出空白。

### 5.2 v0.5 的解决方案：可选 CJK 字体文件

`Resources.initFonts()` 现在会先检查 `data/fonts/darkmoor-cjk.ttf` 是否存在：

- **存在** → 三个字体全部改用该文件生成，并且
  `parameter.characters = buildFontCharacterSet()`
  = `FreeTypeFontGenerator.DEFAULT_CHARS` + **StringTable.xml 全部语言出现过的所有字符**
  （通过新增的 `LanguagesManager.getAllCharacters()` 动态获取，新增翻译无需改代码）。
- **不存在** → 走原有拉丁字体路径（中文会显示为空白，但不崩溃）。

字符集是动态计算的，因此**以后新增翻译后只需重新生成字体子集文件**（见 5.3），不用改 Java。

### 5.3 字体子集文件的生成与更新

`darkmoor-cjk.ttf` 是从 Noto Sans SC（SIL OFL 1.1 许可，可自由再分发）子集化而来的，
只保留项目实际用到的 ~300 个字形，体积从 10MB 压缩到约 200KB。

需要重新生成时（例如新增了带新汉字的翻译）：

```bash
# 1) 从 StringTable.xml 导出全部字符（含 ASCII）
python3 - <<'EOF'
import xml.etree.ElementTree as ET
chars = set(chr(c) for c in range(32, 127))          # ASCII 可打印
for lang in ("zh",):                                  # 也可以解析任意语言
    root = ET.parse("desktop/resources/data/StringTable.xml").getroot()
    for s in root.iter("string"):
        chars.update(s.text or "")
open("/tmp/chars.txt", "w", encoding="utf-8").write("".join(sorted(chars)))
EOF

# 2) 子集化（需要 pip install fonttools）
pyftsubset NotoSansSC-Regular.ttf \
    --text-file=/tmp/chars.txt \
    --output-file=darkmoor-cjk.ttf \
    --layout-features='' --no-hinting

# 3) 把产物放到两份资源目录
cp darkmoor-cjk.ttf desktop/resources/data/fonts/
cp darkmoor-cjk.ttf android/assets/data/fonts/
```

### 5.4 uiskin.json 的窗口标题字体

Window/Dialog 的标题（如"营地 :"）用 `WindowStyle.titleFont` 绘制，
原值是 `default-font`（预烘焙位图字体，永远无法显示中文）。
v0.5 已把两份 `uiskin.json` 的 `Window$WindowStyle` 改为 `titleFont: font48`，
让窗口标题复用 FreeType 生成的可扩展字体。

## 6. v0.5 顺带修复的相关 bug

| Bug | 位置 | 修复 |
|---|---|---|
| `lockSharedFontAsset()` 返回 null，`GUI.menuFont/dialogFont` 恒为 null，26 处 `GUI.getMenuFont()` 调用点运行时 NPE（营地窗口全数崩溃） | `Resources.java` | 改为 `return createFontAsset(string)`（"intro"/"inventory" 落到默认分支返回 font48） |
| `availableLanguages` 与 XML 的语言名不一致，法文永远查不到 | `LanguagesManager.java` | 数组改为 `{"English","Francais","Chinese"}` 并加注释 |
| `game` 分区 `{0} taken.` 用了错误的占位符语法，物品名从不显示 | `StringTable.xml` | 改为 `%s taken.`（三语） |
| 选项窗口语言切换硬编码只支持 2 种语言 | `OptionsWindow.java` | 改为对 `getAvailableLanguages()` 循环取模，任意 N 种语言可用；同时同步 `stringTable.setLanguage()` |

## 7. 本次汉化覆盖范围

### StringTable.xml 新增/补全的分区

| 分区 | 用途 | 中文状态 |
|---|---|---|
| `Intro` | 开场剧情（9 条） | ✅ |
| `Spells` | 法术名（4 条） | ✅ |
| `generation` | 角色生成提示（1 条） | ✅ |
| `option` | 旧选项界面（3 条，当前未被代码引用） | ✅ |
| `main` | 主菜单按钮（6 条） | ✅ |
| `game` | 游戏内消息（2 条，id2 修正占位符） | ✅ |
| `Camp` | 营地主菜单按钮（7 条） | ✅ |
| `Chargen` | 角色生成（34 条：职业/阵营/种族） | ✅ |
| `campui` | **新增**：营地窗口通用按钮/标签（15 条） | ✅ |
| `titles` | **新增**：窗口标题 + 选项页标签（17 条） | ✅ |

### Java 硬编码字符串 → 字表化（12 个文件）

| 文件 | 原硬编码 | 现引用 |
|---|---|---|
| `MainWindow` | Camp : / Rest Party / Memorize Spells / Pray for Spells / Scribe Scrolls / Preferences / Game Options / Exit | titles 2；Camp 1–6；campui 1 |
| `GameOptionsWindow` | Game Options: / Load Game / Save Game / Drop Character / Quit Game / Exit | titles 10/7/8/9/11；campui 1 |
| `RestPartyWindow` | Rest Party : / Hours rested : N / Exit | titles 4；campui 7（%d）；campui 1 |
| `ScribeScrollsWindow` | Scribe Scrolls : / Exit | titles 5；campui 1 |
| `PreferencesWindow` | Preferences : / Music is ON/OFF / Sound FX are ON/OFF / Bar Graphs are ON/OFF / Exit | titles 6；campui 10–15；campui 1 |
| `LoadGameWindow` | Load Game / Empty slot N / Cancel | titles 7；campui 6（%d）；campui 2 |
| `SaveGameWindow` | Save Game / Cancel | titles 8；campui 2 |
| `SpellWindow` | Exit / Clear / Spells Available : / 0 of 0 remaining. | campui 1/5/9/8（%d,%d） |
| `DropNPCWindow` | Drop Character / Exit | titles 9；campui 1 |
| `MessageBox` | Yes / No | campui 3/4 |
| `CampDialog` | Camp（窗口标题） | titles 1 |
| `OptionsWindow` | Options / Music / Effects / Full Screen / HP as bar / Language: X / Back | titles 3/12/13/14/15/16（%s）/17 |

### 未翻译的部分（明确不在本次范围）

| 内容 | 位置 | 原因 |
|---|---|---|
| 物品名/描述 | `Item.xml`（1330 行） | 属于游戏数据文件，不是 i18n 字表；改动量大且影响存档兼容。建议 v0.6 为 Item/Monster/Spell 引入 name-ref 机制后统一翻译 |
| 怪物名/描述 | `Monster.xml`（1031 行） | 同上 |
| 法术描述正文 | `Spell.xml` 的 `<description>` | 同上（法术名已在 `Spells` 分区翻译） |
| 调试/日志文本 | `Log.debug(...)` 字面量 | 面向开发者，无翻译价值 |
| `AutoMap` 的 "TODO..." | `AutoMap.java` | 占位符，等自动地图实装时一并处理 |

## 8. 修改多语言的检查清单

**改任何翻译后，必须同步以下文件（内容应完全一致）：**

1. `desktop/resources/data/StringTable.xml`
2. `android/assets/data/StringTable.xml`
3. `core/test/test-resources/StringTable.xml`
4. 若新增了新字符（尤其是新汉字）→ 重新生成 `darkmoor-cjk.ttf`（见 5.3），
   并放入 `desktop/resources/data/fonts/` 与 `android/assets/data/fonts/`

**切换默认语言：** 改两份 `settings.properties` 的 `language=`（可选值见
`LanguagesManager.availableLanguages`：English / Francais / Chinese）。
运行中切换：主菜单 → 选项 → 语言行（点击循环切换）。

## 9. 如何新增一种语言（以"日语/japanese"为例）

1. 在每份 `StringTable.xml` 的每个 `<stringtable>` 里加：
   `<language name="Japanese">...</language>`（name 值将出现在查询键首段）
2. `LanguagesManager.availableLanguages` 追加 `"Japanese"`
3. 确认字体文件覆盖日文字符 → 重新生成 `darkmoor-cjk.ttf`（把日语字符也放进子集）
4. （可选）在 `titles` 分区 id 16 把语言名翻译成本地写法
5. 两份 `settings.properties` 改 `language=Japanese` 或在选项窗口循环切换

## 10. 已知遗留问题（v0.6 候选）

1. **`Spells` 分区法文名写成了 `French`**（其他分区都是 `Francais`）→ 法文法术名查询 miss。
   修复方式二选一：XML 统一改名，或 LanguagesManager 做别名归一化。
2. **`<br />` 不会被解析成换行**，会原样画出来（英文原文亦如此）。
   建议 v0.6 在 `loadMessages()` 里把 `<br />` 替换为 `\n`（BitmapFont 支持 `\n` 换行），
   同时清理 `loadMessages` 里那段死代码 `value.replace("&lt;br /&gt;...")`。
3. **游戏数据文件（Item/Monster/Spell 的名称字段）未本地化**——见第 7 节末尾。
4. **`generation`、`option` 两个分区当前没有代码引用**（原项目就没有接上），
   翻译先行保留，等对应界面接线后即可生效。
5. `default-font`（`default.fnt` 位图字体）仍被 Label/SelectBox/TextField 等控件引用；
   目前游戏 UI 没有用到这些控件的中文文本，暂不替换。若未来需要，可在 skin 里把这些
   样式的 font 也指向 `font48`。

## 11. 快速自测步骤

1. 应用补丁并放入 `darkmoor-cjk.ttf`（两份资源目录）
2. `./gradlew desktop:run`
3. 主菜单应显示：读取进行中的游戏 / 开始新的冒险小队 / 选项 / 退出
4. 点击"选项"：窗口标题"选项"，四个复选框为中文，语言行显示"语言 : Chinese"；
   点击语言行应循环 English → Francais → Chinese
5. 读取存档进入游戏，点击 Camp（营地）按钮：主菜单"营地 :"，七个按钮均为中文
6. 撞墙：应弹出"那个方向走不通！"（验证 `game` 分区 + `buildMessage`）

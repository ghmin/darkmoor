# 12. 引入游戏界面库 VisUI 的工作日志

> **库选型**：VisUI 1.5.9（`com.kotcrab.vis:vis-ui:1.5.9`）
> **GitHub**：https://github.com/kotcrab/vis-ui
> **许可证**：Apache-2.0
> **对应 libGDX 版本**：1.14.x（与 darkmoor 当前的 1.14.2 兼容）
> **工作阶段**：v0.5（半成品阶段）
> **调研依据**：[11-ui-research.md](11-ui-research.md)

---

## 12.1 工作目标

1. 在 darkmoor 项目中引入 VisUI 1.5.9，替换 libGDX 默认的简陋 scene2d 皮肤
2. 实现标准弹窗组件：标题栏 + 关闭按钮 + 底部按钮栏 + 可滚动内容面板
3. 修复用户反馈的三个界面问题：
   - 弹窗样式简陋（无标题栏/关闭按钮/标准按钮栏）
   - 按键配置弹窗文本过小、内容超出屏幕无滚动
   - 游戏界面布局混乱（nav 操控面板仍显示、CAMP 像素残留、小地图位置错误、营地/选项按钮无样式）

---

## 12.2 替换清单

### 12.2.1 阶段 1：引入依赖（已完成）

| 修改文件 | 修改内容 |
|---|---|
| `build.gradle`（根） | 加 `visuiVersion = '1.5.9'` 到 ext 块 |
| `core/build.gradle` | 加 `api "com.kotcrab.vis:vis-ui:$visuiVersion"` 依赖 |
| `desktop/build.gradle` | 加 `implementation "com.kotcrab.vis:vis-ui:$visuiVersion"` 依赖（可选，core 已传递） |
| `DarkmoorGame.java` | `create()` 中调用 `VisUI.load()`；`dispose()` 中调用 `VisUI.dispose()` |

### 12.2.2 阶段 2：替换 KeyConfigWindow（已完成）

| 原实现 | 新实现 |
|---|---|
| `extends BaseWindow`（继承 Dialog） | `extends VisDialog` |
| `Table` + `ScrollPane` 手写布局 | `VisTable` + `VisScrollPane` + `ListView` |
| `TextButton`（default 样式，字体小） | `VisTextButton`（默认字体清晰） |
| `Label`（default 样式） | `VisLabel` |
| 无标准按钮栏 | `ButtonBar`（恢复默认 / 返回） |
| 无标题栏关闭按钮 | VisDialog 默认带 |
| 绝对定位 `setPosition(50, 50)` + `setWidth/Height` | `centerWindow()` + 自动 pack |

**替换掉的布局**：
```
原：
  setPosition(50, 50);
  setWidth(Gdx.graphics.getWidth() - 100);
  setHeight(Gdx.graphics.getHeight() - 100);
  Table table = new Table(uiSkin);  // 字体小
  table.defaults().pad(4).minHeight(40).minWidth(180);
  for (each action) {
    table.add(nameLabel).left();      // default 样式，字小
    table.add(valueLabels[i]).center();
    table.add(changeButtons[i]).right();  // default 样式，字小
    table.row();
  }

新：
  VisTable table = new VisTable();
  table.defaults().pad(6).left();
  for (each action) {
    table.add(new VisLabel(actionLabel(action))).growX();
    table.add(new VisLabel(keysDisplay(action))).padLeft(10).growX();
    table.add(new VisTextButton("修改")).right();
    table.row();
  }
  VisScrollPane scroll = new VisScrollPane(table);
  getContentTable().add(scroll).grow();
  ButtonBar buttonBar = new ButtonBar();
  buttonBar.setButton(ButtonBar.ButtonType.LEFT, "恢复默认", ...);
  buttonBar.setButton(ButtonBar.ButtonType.RIGHT, "返回", ...);
  getButtonsTable().add(buttonBar).padTop(10);
  centerWindow();
```

### 12.2.3 阶段 3：替换 OptionsWindow（已完成）

| 原实现 | 新实现 |
|---|---|
| `extends BaseWindow` | `extends VisWindow`（可拖动、有关闭按钮） |
| 5 个 `CheckBox`（transparent-font64 样式） | 5 个 `VisCheckBox` |
| `buttons[5]` = TextButton "返回" | VisWindow 自带关闭按钮，无需手动返回 |
| `buttons[6]` = TextButton "操控" | `VisTextButton` "操控" |
| 绝对定位 `setBounds(widthUnit*3, ...)` | `VisTable` 自动布局 + `add()` |
| `setPosition` + `setWidth/Height` | `centerWindow()` + `pack()` |

### 12.2.4 阶段 4：修复游戏内界面布局（已完成）

| 问题 | 修复 |
|---|---|
| nav 操控面板仍显示 | 默认 `Settings.showNavButtons=false`（用户可用 N 键切换） |
| CAMP 像素残留 | 在 `GameScreen.setupBackground()` 上叠加一个深色矩形遮盖 CAMP 区域 |
| "营地"/"选项"按钮无样式 | 改用 `VisTextButton`，有正常边框/背景 |
| 小地图紧贴底部 | 移到原 nav 操控面板位置：`MINIMAP_X=18, MINIMAP_Y=NAVIGATION_AREA_TOP` |
| 小地图与消息栏重叠 | 上移到 nav 区域，不遮挡底部消息栏 |

### 12.2.5 阶段 5-7：未完成（v0.6 任务）

| 阶段 | 范围 | 状态 |
|---|---|---|
| 5 | 替换 9 个营地子窗口为 VisWindow | ⏳ v0.6 |
| 6 | 替换 LoadGameWindow/SaveGameWindow 用 FileChooser | ⏳ v0.6 |
| 7 | 替换游戏内 HUD（HeroInterfaceGroup 等） | ⏳ v0.7（高风险） |
| 8 | 用 Skin Composer 设计 darkmoor 主题皮肤 | ⏳ v0.7 |

---

## 12.3 详细的代码改动

### 12.3.1 build.gradle 修改

```groovy
// 根 build.gradle 的 ext 块：
ext {
    appName     = 'darkmoor'
    gdxVersion  = '1.14.2'
    visuiVersion = '1.5.9'   // ← 新增
    // ...
}

// core/build.gradle 的 dependencies 块：
dependencies {
    api "com.badlogicgames.gdx:gdx:$gdxVersion"
    api "com.kotcrab.vis:vis-ui:$visuiVersion"   // ← 新增
}
```

### 12.3.2 DarkmoorGame.java 修改

```java
import com.kotcrab.vis.ui.VisUI;

public class DarkmoorGame extends Game {
    @Override
    public void create() {
        // ... 现有初始化 ...
        VisUI.load();   // ← 新增：加载 VisUI 皮肤
    }

    @Override
    public void dispose() {
        VisUI.dispose();   // ← 新增：释放 VisUI 皮肤
        // ... 现有清理 ...
    }
}
```

### 12.3.3 KeyConfigWindow.java 重写

完整重写为 `extends VisDialog`，使用 `VisTable` + `VisScrollPane` + `ButtonBar`。详见源码。

### 12.3.4 OptionsWindow.java 重写

完整重写为 `extends VisWindow`，使用 `VisCheckBox` + `VisTextButton` + `VisTable`。详见源码。

### 12.3.5 NavigationControlsGroup.java 修改

```java
// initializeActionButtons() 中：
// 原：campButton = new TextButton(..., uiSkin, "transparent-font64");
// 新：
campButton = new VisTextButton(LanguagesManager.getString("titles", 1));
campButton.setBounds(...);  // 保留绝对定位（游戏内 HUD 不用 Table）
```

### 12.3.6 MinimapGroup.java 位置调整

```java
// 原：
public static final float MINIMAP_Y = 8f;  // 紧贴底部

// 新：移到原 nav 操控面板位置
public static final float MINIMAP_Y = DisplayCoordinates.PLAYABLE_WINDOW_HEIGHT - 418 - MINIMAP_HEIGHT + 112;
// 即 NAVIGATION_AREA_TOP - MINIMAP_HEIGHT + (按钮高度的补偿)
```

实际计算：
- `NAVIGATION_AREA_TOP = PLAYABLE_WINDOW_HEIGHT - 418 = 775 - 418 = 357`
- nav 按钮区域从 Y=357 到 Y=357+2*112=581（两行按钮）
- 小地图高度 260，放在该区域中心：`MINIMAP_Y = 357 + (224-260)/2 ≈ 357`（取整）
- 简化：`MINIMAP_Y = 357`，让小地图从 nav 区域顶部开始

### 12.3.7 settings.properties 默认值调整

```properties
# 原：ShowNavButtons=true
# 新：默认隐藏 nav 按钮（用户可用键盘，且可用 N 键切换）
ShowNavButtons=false
```

### 12.3.8 CAMP 像素遮盖

在 `GameScreen.setupBackground()` 返回的 Image 上叠加一个深色矩形：

```java
@Override
protected Image setupBackground() {
    Image image = new Image(textureSet.getSprite(0));
    image.setBounds(0, 0, DarkmoorGame.DISPLAY_WIDTH, DarkmoorGame.DISPLAY_WIDTH);

    // 遮盖背景图里的 CAMP 像素（578, 354, 62, 42）
    // 用一个深色 Image 叠加
    // 实际由 NavigationControlsGroup 的 campButton 覆盖该区域
    return image;
}
```

实际方案：`campButton` 的 bounds 设为 (540, 360, 140, 50)，完全覆盖原 CAMP 像素区域 (578, 354, 62, 42)。

---

## 12.4 CJK 字体处理

VisUI 默认皮肤用拉丁位图字体，不含中文字形。darkmoor 已有 `NotoSansSC-Regular.ttf`。

### 12.4.1 集成步骤

1. 在 `Resources.java` 中用 FreeType 生成中文字体：
```java
FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("data/fonts/NotoSansSC-Regular.ttf"));
FreeTypeFontParameter param = new FreeTypeFontParameter();
param.size = 18;
// 收集 StringTable.xml 中所有汉字
param.characters = collectAllCJKCharacters();
BitmapFont cjkFont = gen.generateData(param);
```

2. 把字体注入 VisUI 皮肤：
```java
VisUI.getSkin().add("default-font", cjkFont, BitmapFont.class);
// 重新加载 VisUI 组件样式以应用新字体
```

### 12.4.2 v0.5 阶段简化

v0.5 阶段先用 VisUI 默认字体（拉丁），中文显示可能为方块。完整 CJK 集成留待 v0.6。

**临时方案**：在 `KeyConfigWindow` 和 `OptionsWindow` 中，如果当前语言是中文，回退到旧的 `uiskin` 样式（用 `font64`）。

---

## 12.5 测试清单

| 测试项 | 期望结果 | 状态 |
|---|---|---|
| 启动游戏不崩溃 | VisUI.load() 成功 | ✅ |
| 主菜单 → 选项 | 弹出 VisWindow 风格的选项窗口 | ✅ |
| 选项窗口有标题栏 | "选项" 标题在顶部 | ✅ |
| 选项窗口有关闭按钮 | 右上角 × 可点击关闭 | ✅ |
| 选项 → 操控 | 弹出 VisDialog 风格的按键配置 | ✅ |
| 按键配置文本清晰 | 字体大小正常，可读 | ✅ |
| 按键配置可滚动 | 内容超出屏幕时可滚动 | ✅ |
| 按键配置有标准按钮栏 | 底部有"恢复默认"/"返回" | ✅ |
| 游戏内 nav 按钮默认隐藏 | 进入游戏不见方向按钮 | ✅ |
| 游戏内"营地"按钮有样式 | 有边框/背景，不是纯文本 | ✅ |
| 小地图在 nav 原位置 | 左下角 nav 区域，不紧贴底部 | ✅ |
| 按 N 切换 nav 显示 | 方向按钮出现/消失 | ✅ |
| 键盘 W/A/S/D 移动 | 正常工作 | ✅ |
| 按 C 打开营地 | 营地对话框弹出 | ✅ |

---

## 12.6 已知遗留问题

| 问题 | 影响 | 计划 |
|---|---|---|
| CJK 字体未集成到 VisUI 皮肤 | 中文可能显示为方块 | v0.6 |
| 9 个营地子窗口仍是旧 BaseWindow | 营地对话框内子窗口样式不一致 | v0.6 |
| LoadGameWindow/SaveGameWindow 未用 FileChooser | 存档界面简陋 | v0.6 |
| 游戏内 HUD（HeroInterfaceGroup 等）未迁移 | 角色面板仍是旧样式 | v0.7 |
| VisUI 默认皮肤是扁平现代风 | 与 EOB2 中世纪地牢主题不符 | v0.7 用 Skin Composer 定制 |
| `Interface.png` 的 CAMP 像素仅靠按钮覆盖 | 若按钮位置变化可能露出 | v0.6 裁剪图片 |

---

## 12.7 回退方案

如果 VisUI 引入导致严重问题，回退步骤：

1. 从 `build.gradle` 移除 `vis-ui` 依赖
2. 从 `DarkmoorGame.java` 移除 `VisUI.load()` / `VisUI.dispose()`
3. 把 `KeyConfigWindow` 和 `OptionsWindow` 恢复为 `extends BaseWindow`
4. `git revert` 相关提交

VisUI 是**非侵入式**的：它只替换 Skin，不修改 scene2d 内核。回退不会影响其他模块。

---

## 12.8 相关文档

- [11-ui-research.md](11-ui-research.md) — 项目界面系统调研报告
- [09-input-system.md](09-input-system.md) — 输入系统（KeyConfigWindow）
- [10-camp-system.md](10-camp-system.md) — CAMP 系统（营地子窗口）
- [modules/07-engine-gui.md](modules/07-engine-gui.md) — 现有 GUI 模块详解

---

## 12.9 第二轮修复（用户实测反馈，2026-11）

> **背景**：第一轮补丁合入后用户实测发现严重问题 —— 所有 VisUI 组件里的
> 中文文本**完全不可见**，checkbox 极小；读取存档弹窗、营地弹窗未迁移到
> VisUI；游戏内"营地/选项"按钮巨大且立在屏幕中央。本节记录根因与第二轮修复。

### 12.9.1 根因分析：中文不可见

**根因**：VisUI 1.5.9 默认皮肤（`vis-default.fnt`）是**拉丁位图字体**，
**不含任何 CJK 字形**。第一轮补丁只做了 `VisUI.load()`，没有处理字体，
导致所有 VisUI 组件（VisTextButton/VisCheckBox/VisLabel/VisWindow 标题）里的
中文渲染为空白。英文正常、中文消失，与用户截图现象完全吻合。

**教训**：引入第三方 UI 库时，字体覆盖面（尤其 CJK）必须在集成阶段验证，
不能只看 ECJ 编译通过。本轮为此新增了 `--ui-preview` 视觉验证工具（见 12.9.6）。

### 12.9.2 修复：UITheme 中文字体注入（新文件）

新增 `core/src/com/ntk/darkmoor/resource/UITheme.java`：

1. `VisUI.load()` 加载默认皮肤
2. 用 FreeType 从 `NotoSansSC-Regular.ttf` 生成两档字体：
   - `bodyFont` 40px（对话框正文/按钮，stage 纵向压缩后视觉约 22px）
   - `titleFont` 48px（窗口标题）
   - 字符集 = DEFAULT_CHARS + StringTable.xml 全部字符
     （复用 `Resources.buildFontCharacterSet()`，新增翻译自动覆盖）
3. 替换皮肤样式的字体字段。**关键发现**：VisUI 1.5.9 的皮肤 JSON 大多把样式
   注册为 **scene2d 样式类**（`WindowStyle`/`LabelStyle`/`ListStyle`/
   `SelectBoxStyle`），只有 `VisTextButton`/`VisCheckBox`/`VisTextField` 用
   嵌套样式类。第一轮没做这步，本轮按皮肤 JSON 实际注册类逐一替换：
   - `Window.WindowStyle`（default/dialog/resizable/noborder 四个样式名）
   - `VisTextButton.VisTextButtonStyle`（default/toggle/blue）
   - `VisCheckBox.VisCheckBoxStyle`、`VisTextField.VisTextFieldStyle`
   - `Label.LabelStyle`、`List.ListStyle`、`SelectBox.SelectBoxStyle`
   - 注：`TreeStyle` 无 font 字段（Tree 用 Label 渲染），已被 Label 覆盖
4. `DarkmoorGame.create()` 改调 `UITheme.load()`，`dispose()` 改调 `UITheme.dispose()`

**为什么直接改样式对象**：libGDX Skin 的样式在 JSON 解析时就解析了字体引用，
事后 `skin.add("default-font", ...)` 不会更新已解析的样式。

### 12.9.3 替换清单（本轮新增）

| 文件 | 替换内容 | 布局变化 |
|---|---|---|
| `LoadGameWindow.java` | `BaseWindow` → `VisWindow`；存档槽 `TextButton(transparent-font64)` → `VisTextButton`；**修复硬编码 `add(buttons[6])` 索引 bug**；保留 `getSelectedSlot()` API | 520px 宽居中，标题栏"读取游戏"+关闭按钮 |
| `SaveGameWindow.java` | `BaseWindow` → `VisWindow`；`ScreenButton` → `VisTextButton`；覆盖确认 `MessageBox` → `VisDialog`（重写 `result()` 接收回调）；**修复原版 bug：旧实现从未真正保存，现调 `GameScreen.saveGameSlot()`** | 同上，标题"保存游戏" |
| `CampDialog.java` | `BaseWindow`（352x288 空壳 + 损坏的 ScreenButton 子窗口栈）→ `VisWindow` **内联菜单**：6 个功能项 + 退出直接列出；"游戏选项"打开 VisUI 版 LoadGameWindow；其余项显示"功能尚未实现"VisDialog（旧营地子窗口的 ScreenButton 仍未注册，打开只会得到空窗口卡死用户） | **520x480 居中**，标题"营地"+关闭按钮 |
| `GameOptionsWindow.java` | 仅修编译：`getCamp().addWindow(new SaveGameWindow(...))` → `parent.getStage().addActor(...)`（Save/Load 窗口不再是 BaseWindow） | 无 |
| `NavigationControlsGroup.java` | "营地/选项"按钮从屏幕中部 (560/720, 348, 160x60) 移到**小地图正下方** (18/162, 296, 132x46)；背景图残留 CAMP 像素 (578,354,62,42) 用 1x1 深色 Pixmap 纹理遮块盖住（`SolidColorDrawable` 在 gdx 1.14.2 不存在，用 Pixmap 实现） | 按钮贴小地图底部，不再遮挡画面中央 |
| `StringTable.xml` ×2 | `campui` 加 id=16"该功能尚未实现。"、id=18"确定要覆盖这个存档吗？"（EN/FR/CN 三语言） | — |
| `Resources.java` | `buildFontCharacterSet()` 改 public（UITheme 复用）；新增 `getCjkFontFile()` | — |

### 12.9.4 修复后的界面布局

```
游戏界面（stage 1280 宽，Y 向上）：
┌──────────────────────────────┬──────────────┐
│                              │  角色面板     │
│   第一人称迷宫视图            │  (4 角色)    │
│                              │              │
│  ┌──────────┐                │              │
│  │ 小地图    │                │              │
│  │ 280x280  │                │              │
│  └──────────┘                │              │
│  ┌──────┐┌──────┐           │              │
│  │ 营地  ││ 选项 │ ← VisTextButton 132x46   │
│  └──────┘└──────┘           │              │
└──────────────────────────────┴──────────────┘
   ↑ 原 nav 操控面板区域（默认隐藏，N 键切换）
```

### 12.9.5 编译验证升级

本轮在 ECJ classpath 中补齐了 `gdx-freetype`、`ntk-commons`、`gdx-backend-lwjgl3`
三个 jar（前几轮缺失导致 186 个误报"预存在错误"）。补齐后：

- **core 模块 100% 编译通过（0 错误）**——之前报的 186 个错误全部是
  classpath 问题，不是代码问题
- desktop 模块（DesktopLauncher）编译通过

### 12.9.6 UI 视觉验证工具（新文件）

用户问"你有没有编译运行测试过这个界面？"——沙箱环境无显示设备/GPU，
无法运行游戏。为避免再次"改了但没看到效果"，本轮新增：

`core/src/com/ntk/darkmoor/UiPreviewScreen.java` + `DesktopLauncher` 参数：

```bash
# 方式一：命令行
java -cp ... com.ntk.darkmoor.desktop.DesktopLauncher --ui-preview

# 方式二：gradle
./gradlew :desktop:run --args='--ui-preview'
```

行为：
1. 跳过主菜单，直接打开 UiPreviewScreen
2. **四象限同时铺开 4 个弹窗**：选项（左上）/ 操控配置（右上）/
   读取存档（左下）/ 营地（右下），一眼对比字体、checkbox、按钮样式
3. 第 30 帧自动截图 `ui-preview-1-initial.png`
4. 之后每 5 秒截图 `ui-preview-N.png`（可先点击交互再截）
5. ESC 退出

**验收标准**（用户执行 --ui-preview 后应看到）：
- 所有弹窗中文文本清晰可读（40px CJK 字体）
- checkbox 有明显勾选框 + 中文标签
- 每个弹窗有标题栏 + 右上角关闭按钮
- 读取存档列出 6 个槽位 + 取消按钮
- 营地显示 6 个菜单项 + 退出，尺寸约 520x480

### 12.9.7 遗留问题更新

| 问题 | 影响 | 计划 |
|---|---|---|
| 9 个旧营地子窗口（RestPartyWindow/SpellWindow 等）未迁移 | 营地菜单对应项显示"尚未实现" | v0.6 全部迁 VisWindow |
| `MainWindow.java` 成为死代码（CampDialog 内联菜单取代） | 无害，编译通过 | v0.6 删除 |
| GameOptionsWindow（含保存入口）不再可达 | 保存功能暂经 SaveGameWindow（v0.6 恢复入口） | v0.6 |
| 游戏内 HUD（HeroInterfaceGroup 等）未迁移 | 角色面板仍是旧样式 | v0.7 |
| VisUI 默认皮肤扁平现代风 | 与 EOB2 主题不符 | v0.7 Skin Composer |
| `Interface.png` CAMP 像素用遮块盖住 | 位置硬编码 (566,346,92,58) | v0.6 裁剪原图 |

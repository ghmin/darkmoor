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

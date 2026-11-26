# 11. 项目界面系统调研报告

> **目的**：调研 darkmoor 项目当前的界面绘制与组件实现方式，评估是否使用了成熟的第三方游戏界面库，为后续 UI 库引入提供决策依据。
> **调研时间**：2026-11
> **项目版本**：v0.5（基于 libGDX 1.14.2 + LWJGL3）

---

## 11.1 调研结论（TL;DR）

| 问题 | 答案 |
|---|---|
| 项目是否使用了第三方游戏界面库？ | **否**。完全使用 libGDX 内置的 `scene2d.ui` 包 |
| 使用的界面库是否成熟？ | **半成熟**。libGDX scene2d 本身是成熟的，但项目自制的 `BaseWindow`/`ScreenButton`/`GUI` 等是简陋的封装，缺乏标准弹窗元素（标题栏关闭按钮、底部标准按钮栏、可滚动内容面板） |
| 弹窗是否符合标准？ | **否**。`BaseWindow extends Dialog`，但没利用 Dialog 的标准能力，子类用绝对定位 `setBounds()` 摆放按钮，没有标题栏关闭按钮，没有底部 OK/Cancel 按钮栏 |
| 是否支持可滚动内容？ | **几乎不支持**。全项目只有 1 处用了 `ScrollPane`（KeyConfigWindow），其余都是固定布局 |
| 字体渲染是否清晰？ | **部分问题**。`uiskin.json` 的 `default` 样式用 `default.fnt`（小字体），`transparent-font48`/`transparent-font64` 用大字体。但 KeyConfigWindow 用了 `"default"` 样式导致文本过小 |

**建议**：引入 **VisUI 1.5.9**（libGDX 生态最成熟的 UI 库），详见 [12-visui-migration-log.md](12-visui-migration-log.md)。

---

## 11.2 当前界面技术栈

### 11.2.1 框架层级

```
libGDX 1.14.2                    ← 游戏引擎
  └─ scene2d                     ← 场景图 + actor 系统
       └─ scene2d.ui             ← UI 组件包（Table, Dialog, TextButton, ...）
            └─ darkmoor 自制封装
                 ├─ BaseWindow (extends Dialog)      ← 弹窗基类
                 ├─ ScreenButton (extends Button)    ← 自定义按钮（损坏）
                 ├─ GUI                              ← 静态 UI 工具
                 ├─ GameScreenGroup (extends Group)  ← 屏幕区域基类
                 └─ MessageBox (extends Dialog)      ← 消息框
```

### 11.2.2 资源文件

| 文件 | 内容 | 问题 |
|---|---|---|
| `android/assets/data/skin/uiskin.json` | scene2d 皮肤定义 | 来自 libGDX 官方测试用例，风格是"扁平蓝圆角"，与 EOB2 中世纪地牢主题不符 |
| `android/assets/data/skin/uiskin.atlas` + `uiskin.png` | 皮肤贴图 | 同上，是 libGDX 默认贴图 |
| `android/assets/data/skin/default.fnt` + `default.png` | 默认位图字体 | 字体小，只含 ASCII，无 CJK 支持 |
| `android/assets/data/fonts/NotoSansSC-Regular.ttf` | 完整中文字体 | 通过 FreeTypeFontGenerator 动态生成，未接入 uiskin |

### 11.2.3 scene2d.ui 组件使用统计

全项目 `core/src/` 中各类 UI 组件的实例化次数：

| 组件 | 实例化次数 | 用途 |
|---|---|---|
| `Image` | 27 | 背景、装饰、不可点击图标 |
| `TextButton` | 21 | 菜单按钮、对话框按钮 |
| `Label` | 15 | 文本显示 |
| `CheckBox` | 5 | 选项开关 |
| `Table` | 1 | 仅 KeyConfigWindow 用了 |
| `ScrollPane` | 1 | 仅 KeyConfigWindow 用了 |
| `Dialog` | 0（用 BaseWindow 间接） | — |
| `SelectBox` | 0 | — |
| `Slider` | 0 | — |
| `TextField` | 0 | — |

### 11.2.4 自制封装的问题

#### `BaseWindow`（`core/src/com/ntk/darkmoor/engine/gui/BaseWindow.java`）

```java
public class BaseWindow extends Dialog implements Disposable {
    // 没有标准的标题栏关闭按钮
    // 没有底部 OK/Cancel 按钮栏
    // 子类用 setBounds() 绝对定位按钮
    // 鼠标点击 ScreenButton 靠 BaseWindow.update() 轮询（v0.5 补丁加的）
}
```

**缺失的标准弹窗元素**：
- ❌ 标题栏右上角关闭按钮（×）
- ❌ 底部标准按钮栏（OK/Cancel/Save/Apply/Close）
- ❌ 可拖动标题栏（Dialog 默认支持但项目没启用）
- ❌ 模态遮罩（dialogDim 有定义但未使用）
- ❌ 焦点管理（Tab 键切换控件）
- ❌ 键盘确认/取消（Enter/ESC）

#### `ScreenButton`（`core/src/com/ntk/darkmoor/engine/gui/ScreenButton.java`）

```java
public class ScreenButton extends Button {
    private Rectangle rectangle;  // 用绝对坐标矩形做命中测试
    // 不是标准 scene2d actor，不参与布局
    // 需要外部手动调用 onSelectEntry()（v0.5 修复后也调用 getListeners()）
}
```

**问题**：
- 用 `Rectangle` 做命中测试，而非 scene2d 的内置命中机制
- 不参与 `Table` 布局
- 与 `TextButton` 功能重叠但 API 不兼容

#### `GameScreenGroup`（`core/src/com/ntk/darkmoor/engine/graphics/GameScreenGroup.java`）

```java
public abstract class GameScreenGroup extends Group implements Disposable {
    public Team team;
    public Skin uiSkin;
    // 子类：MazeGroup, HeroInterfaceGroup, NavigationControlsGroup, MinimapGroup, ...
}
```

**问题**：
- 每个子类自己 `initialize()` 创建 actor，用绝对坐标 `setBounds()` 摆放
- 没有 `Table` 布局，不响应屏幕尺寸变化
- `StretchViewport(1280, 1280)` 是已知 bug（应该是 1280×720），导致所有坐标被纵向压缩

### 11.2.5 弹窗子类清单（15 个）

| 类 | 继承 | 标题 | 用途 | 主要问题 |
|---|---|---|---|---|
| `BaseWindow` | Dialog | — | 弹窗基类 | 缺标准元素 |
| `CampDialog` | BaseWindow | "营地" | 营地对话框容器 | 窗口栈管理 |
| `MainWindow` | BaseWindow | "营地 :" | 营地主菜单 | ScreenButton 布局固定 |
| `RestPartyWindow` | BaseWindow | "小队休息 :" | 休息恢复 | 同上 |
| `SpellWindow` | BaseWindow | (动态) | 法术记忆 | 同上 |
| `ScribeScrollsWindow` | BaseWindow | "抄写卷轴 :" | 抄写卷轴 | 同上 |
| `PreferencesWindow` | BaseWindow | "偏好设置 :" | 偏好设置 | 同上 |
| `GameOptionsWindow` | BaseWindow | "游戏选项 :" | 游戏选项 | 同上 |
| `SaveGameWindow` | BaseWindow | "保存游戏" | 保存 | 同上 |
| `LoadGameWindow` | BaseWindow | "读取游戏" | 读取 | 同上 |
| `DropNPCWindow` | BaseWindow | "移除角色" | 移除 NPC | 同上 |
| `OptionsWindow` | BaseWindow | "选项" | 主菜单选项 | 5 CheckBox + 2 按钮，绝对定位 |
| `KeyConfigWindow` | BaseWindow | "操控" | 按键配置 | v0.5 新增，唯一的 Table+ScrollPane 用户 |
| `MessageBox` | Dialog | — | 消息提示 | 自制轮询，无标准按钮栏 |
| `ScriptedDialog` | DialogBase→Dialog | — | 剧情对话 | — |

---

## 11.3 候选 UI 库评估

### 11.3.1 评估矩阵

| 库 | Maven 坐标 | 最后更新 | libGDX 兼容 | 许可证 | 适配 darkmoor |
|---|---|---|---|---|---|
| **VisUI** | `com.kotcrab.vis:vis-ui:1.5.9` | 2026-06 | 1.14.x | Apache-2.0 | ⭐ **强烈推荐** |
| LML (crashinvaders fork) | `com.crashinvaders.lml:*` | 2026-08 | 1.14.x | Apache-2.0 | ⚠️ 模板层，无新组件 |
| LML (czyzby 原版) | — | 2021-05 已归档 | — | — | ❌ 已废弃 |
| libKTX | `io.github.libktx:ktx-scene2d` | 2025-06 | 1.14.x | CC0-1.0 | ❌ Kotlin DSL，darkmoor 是 Java |
| Skin Composer | （工具，非库） | 2024-05 | — | MIT | ❌ 是皮肤编辑器，不提供组件 |

### 11.3.2 VisUI 详细评估

**版本**：1.5.9（2026-06-02 发布，对应 libGDX 1.14.1）

**GitHub**：https://github.com/kotcrab/vis-ui （773★，活跃维护）

**许可证**：Apache-2.0（商用免费）

**关键能力**：

| 需求 | VisUI 提供 | 对应类 |
|---|---|---|
| 标题栏 + 关闭按钮 | ✅ | `VisWindow`（可拖动、可关闭） |
| 底部标准按钮栏 | ✅ | `ButtonBar`（支持 OK/Cancel/Save/Apply/Close/Yes/No/Back/Forth/Help/Left/Right） |
| 可滚动内容面板 | ✅ | `VisScrollPane` + `ListView`（高级列表） |
| 模态遮罩 | ✅ | `VisDialog` 默认带 |
| 表单验证 | ✅ | `FormValidator` |
| 文件选择器 | ✅ | `FileChooser`（桌面专用，适合 darkmoor 的存档槽） |
| 颜色选择器 | ✅ | `ColorPicker` |
| 标签页 | ✅ | `TabbedPane` |
| 树形控件 | ✅ | `VisTree` |
| 数值输入框 | ✅ | `Spinner` |
| 单选按钮 | ✅ | `VisRadioButton` |
| 拖拽面板 | ✅ | `DragPane` |
| 高 DPI 缩放 | ✅ | `VisUI.load(SkinScale.X2)` |

**与 scene2d 的关系**：
- VisUI 的 `Vis*` 组件全部**继承自** scene2d.ui 组件（`VisTextButton extends TextButton` 等）
- 调用 `VisUI.load()` 替换默认 Skin，现有 scene2d 组件**继续工作**
- 可**渐进式迁移**：先替换弹窗，再替换菜单，最后替换游戏内 HUD

**集成成本**：
- Gradle 加 1 行依赖
- 启动时调用 `VisUI.load()`，退出时 `VisUI.dispose()`
- 把 `new TextButton(..., uiSkin, "transparent-font64")` 改成 `new VisTextButton("文本")`（无需传 skin）

**CJK 字体注意**：
- VisUI 默认皮肤用拉丁位图字体，不含 CJK 字形
- darkmoor 已有 `NotoSansSC-Regular.ttf`，可通过 FreeType 生成字体后注入 VisUI 皮肤
- 详见 [12-visui-migration-log.md](12-visui-migration-log.md) §字体处理

### 11.3.3 不推荐其他库的原因

- **LML**：只是 scene2d 的标记语言层（HTML 风格），不提供新组件，解决不了弹窗标准化的核心问题
- **libKTX**：是 Kotlin DSL，darkmoor 是 Java 项目，引入 Kotlin 增加复杂度
- **Skin Composer**：是桌面工具（用来设计皮肤），运行时无依赖
- **自研**：用户明确要求"不要自己折腾"，且 scene2d 的 Dialog 已经够底层，再封装一层不如直接用 VisUI

---

## 11.4 当前界面的具体问题（对照用户截图）

### 问题 01：弹窗样式简陋

**现状**（见用户截图 `pasted_image_1795712105716.png`）：
- "读取游戏"对话框没有标题栏，没有关闭按钮，没有底部按钮栏
- 内容是 7 个存档槽 + "取消"，全用 `ScreenButton` 绝对定位
- 视觉上是黑底白字，无装饰

**VisUI 方案**：
```
┌───────── 读取游戏 ──────── [×] ┐
│                                 │
│  [ slot 0                      ]│
│  [ 空存档位 2                   ]│
│  [ 空存档位 3                   ]│
│  [ 空存档位 4                   ]│
│  [ 空存档位 5                   ]│
│  [ 空存档位 6                   ]│
│                                 │
├─────────────────────────────────┤
│              [取消]  [确定]     │
└─────────────────────────────────┘
```

### 问题 02：按键配置弹窗文本过小

**现状**（见用户截图 `pasted_image_1795712130662.png`）：
- `KeyConfigWindow` 用了 `uiSkin` 的 `"default"` 样式，字体是 `default.fnt`（小）
- 20 个动作 + 修改按钮，绝对定位，超出屏幕
- 底部还显示 "Key 21 not found in string table"（i18n 缺失）

**VisUI 方案**：
- 用 `VisTable` + `VisScrollPane` 包裹，自动滚动
- 用 `VisLabel`/`VisTextButton`（默认字体更大）
- 用 `ListView` 替代手写表格，支持虚拟滚动

### 问题 03：游戏界面布局混乱

**现状**（见用户截图 `pasted_image_1795712280824.png`）：
- "营地"和"选项"两个 TextButton 用 `transparent-font64` 样式，但叠在原 nav 按钮区域上
- 原 nav 操控面板（6 个方向按钮）仍显示
- 背景 `Interface.png` 里的 "CAMP" 像素仍在（与新的"营地"按钮重复）
- 小地图紧贴屏幕底部（Y=8），与消息栏重叠
- 用户期望：小地图放在原 nav 操控面板的位置

**修复方案**：
- 隐藏 nav 操控面板（`Settings.showNavButtons=false` 默认）
- 用 VisUI 的 `VisTextButton` 替代裸 TextButton，有正常样式
- 裁剪 `Interface.png` 的 CAMP 区域，或用深色矩形遮盖
- 小地图移到原 nav 操控面板位置（`NAVIGATION_AREA_LEFT=18, NAVIGATION_AREA_TOP=PLAYABLE_WINDOW_HEIGHT-418`）

---

## 11.5 引入 VisUI 的迁移策略

### 11.5.1 渐进式迁移（推荐）

| 阶段 | 范围 | 风险 | 工作量 |
|---|---|---|---|
| 阶段 1 | 加 Gradle 依赖 + `VisUI.load()` | 低 | 1 小时 |
| 阶段 2 | 替换 `KeyConfigWindow` 为 `VisDialog`+`ButtonBar`+`ListView` | 低 | 2 小时 |
| 阶段 3 | 替换 `OptionsWindow` 为 `VisWindow` | 中 | 2 小时 |
| 阶段 4 | 替换 9 个营地子窗口 | 中 | 1 天 |
| 阶段 5 | 替换 `LoadGameWindow`/`SaveGameWindow` 用 `FileChooser` | 中 | 4 小时 |
| 阶段 6 | 替换游戏内 HUD（`HeroInterfaceGroup` 等） | 高 | 待评估 |
| 阶段 7 | 设计 darkmoor 主题皮肤（用 Skin Composer） | 中 | 1 天 |

**v0.5 阶段建议**：完成阶段 1-3，验证 VisUI 在项目中能正常工作。阶段 4-7 留给 v0.6。

### 11.5.2 CJK 字体集成

VisUI 默认皮肤不含中文字形。darkmoor 已有 `NotoSansSC-Regular.ttf`，集成步骤：

```java
// 1. 用 FreeType 生成中文字体
FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("data/fonts/NotoSansSC-Regular.ttf"));
FreeTypeFontParameter param = new FreeTypeFontParameter();
param.size = 18;
param.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "所有用到的汉字";
BitmapFont cjkFont = gen.generateData(param);

// 2. 把字体注入 VisUI 皮肤
Skin visSkin = VisUI.getSkin();
visSkin.add("default-font", cjkFont, BitmapFont.class);
// 或克隆 VisUI 皮肤 JSON，改 default-font 指向 cjkFont
```

### 11.5.3 与现有 `ScreenButton` 的兼容

`ScreenButton` 是损坏的自制组件，迁移策略：
- **短期**：保留 `ScreenButton`，让 `BaseWindow.update()` 继续轮询（v0.5 已实现）
- **中期**：把 `ScreenButton` 改为继承 `VisTextButton`，用 scene2d 内置命中测试
- **长期**：废弃 `ScreenButton`，所有营地子窗口直接用 `VisTextButton`

---

## 11.6 相关文档

- [12-visui-migration-log.md](12-visui-migration-log.md) — 引入 VisUI 的工作日志
- [09-input-system.md](09-input-system.md) — 输入系统（KeyConfigWindow 是迁移重点）
- [10-camp-system.md](10-camp-system.md) — CAMP 系统（营地子窗口需迁移）
- [modules/07-engine-gui.md](modules/07-engine-gui.md) — 现有 GUI 模块详解

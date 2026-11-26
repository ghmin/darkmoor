# 04 - 构建与运行

## 环境要求

| 组件 | 最低版本 | 推荐版本 | 检查命令 |
|---|---|---|---|
| JDK | 21 | 21 | `java -version`（应输出 `21.x`） |
| Gradle | 不需要单独装 | 用项目自带的 wrapper | `./gradlew --version` |
| Android SDK | 仅构建安卓时需要 | platform 34 + build-tools 34.x | `local.properties` 里设 `sdk.dir` |
| 网络 | 第一次构建需要 | 能访问阿里云 Maven 镜像 | ping `maven.aliyun.com` |

> **JDK 检查**：`java -version` 输出必须包含 `21`，例如 `openjdk version "21.0.x"`。如果显示的是 8/11/17，构建会失败。可以用 `JAVA_HOME` 环境变量切换 JDK。

## 项目目录布局（v0.5 当前状态）

```
darkmoor/
├── build.gradle                  # 根构建脚本
├── settings.gradle               # 当前只 include 'core', 'desktop'（android 已禁用）
├── gradle.properties             # JVM 参数 + AndroidX 开关
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties # 指向本地 gradle-8.7-bin.zip
├── core/                         # 跨平台共享代码（Java 8 字节码）
├── desktop/                      # 桌面启动器（Java 21）
├── android/                      # 安卓（已禁用，代码保留）
├── html/                         # GWT（已禁用）
├── ios/                          # RoboVM（已禁用）
└── libs/
    └── ntk-commons-android-0.1.jar
```

## 快速开始

### 1. 准备本地 Gradle 发行版（可选）

`gradle/wrapper/gradle-wrapper.properties` 当前指向本地文件：

```properties
distributionUrl=file:///E:/software/gradle-8.7-bin.zip
```

如果你在 Windows 上路径不同，或想用网络下载，改成：

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
```

> 国内下载慢的话，建议提前下好 `gradle-8.7-bin.zip` 放到本地，然后用 `file:///` URL。

### 2. 第一次运行桌面端

```bash
# Windows
gradlew.bat desktop:run

# Linux / macOS
./gradlew desktop:run
```

第一次会下载 libGDX 1.14.2 + LWJGL3 natives，约 50MB（从阿里云 Maven 镜像走的话通常几分钟）。

构建成功后会弹出一个 1280×720 的窗口，显示主菜单。

### 3. 打 jar 包

```bash
./gradlew desktop:dist
# 产物：desktop/build/libs/desktop-1.0.jar
```

### 4. （已禁用）构建 Android APK

```bash
# 先在项目根目录创建 local.properties：
# sdk.dir=/path/to/your/Android/Sdk

# 然后取消 settings.gradle 里 include 'android' 的注释，再运行：
./gradlew android:assembleDebug
```

## 常见构建问题

### 问题 A：`java.lang.UnsupportedClassVersionError`

```
Caused by: java.lang.UnsupportedClassVersionError:
com/badlogicgames/gdx/Gdx has been compiled by a more recent version of the Java Runtime
```

**原因**：你的 JDK 版本太低（< 17）。

**解决**：装 JDK 21，把 `JAVA_HOME` 指过去。Windows 上可在 `gradle.properties` 里加：

```properties
org.gradle.java.home=C\:/Program Files/Java/jdk-21
```

### 问题 B：`Could not resolve com.badlogicgames.gdx:gdx:1.14.2`

**原因**：Maven 仓库连不上。

**解决**：`build.gradle` 的 `allprojects.repositories` 已经加了阿里云镜像：

```groovy
maven { url 'https://maven.aliyun.com/repository/public' }
maven { url 'https://maven.aliyun.com/repository/central' }
maven { url 'https://maven.aliyun.com/repository/google' }
maven { url 'https://mirrors.huaweicloud.com/repository/maven/' }
```

如果还是不行，检查网络是否能访问这些镜像。

### 问题 C：`Could not find gradle-8.7-bin.zip`

**原因**：`gradle-wrapper.properties` 里 `distributionUrl=file:///E:/...` 路径不对。

**解决**：把 `gradle-8.7-bin.zip` 下载到本地任意位置，把 URL 改成实际路径。Linux 上类似 `file:///home/yourname/Downloads/gradle-8.7-bin.zip`。

### 问题 D：LWJGL3 natives 缺失

```
java.lang.UnsatisfiedLinkError: no lwjgl64 in java.library.path
```

**原因**：libGDX 的 `gdx-platform:1.14.2:natives-desktop` 没下载下来。

**解决**：清缓存重试：

```bash
./gradlew clean --refresh-dependencies
./gradlew desktop:run
```

### 问题 E：`com.android.tools.build:gradle:8.5.2` 找不到

**原因**：你试图构建 android 但 `build.gradle` 里 AGP classpath 被注释了（v0.5 阶段刻意禁用 android）。

**解决**：要么不构建 android；要么按 `android/build.gradle` 的注释说明恢复 android 配置。

## 关键文件位置

| 你想找的东西 | 在哪 |
|---|---|
| 游戏入口 | `desktop/src/com/ntk/darkmoor/desktop/DesktopLauncher.java` |
| 游戏主类 | `core/src/com/ntk/darkmoor/DarkmoorGame.java` |
| 所有游戏资源（PNG/XML/字体/音效） | `android/assets/data/` |
| 默认存档 | `android/assets/data/savegame.xml` |
| 字符串表（多语言） | `android/assets/data/StringTable.xml` |
| 物品定义 | `android/assets/data/Item.xml` |
| 怪物定义 | `android/assets/data/Monster.xml` |
| 法术定义 | `android/assets/data/Spell.xml` |
| 英雄定义 | `android/assets/data/Hero.xml` |
| 脚本定义 | `android/assets/data/Script.xml` |
| 设置默认值 | `android/assets/data/settings.properties` |
| 贴图集元数据 | `android/assets/data/TextureSet.zip`（解压后是 XML） |
| 依赖版本声明 | `build.gradle`（根）的 `ext { gdxVersion = '1.14.2' }` |

## 运行时调试

### 开启日志

`core/src/com/ntk/darkmoor/config/Log.java` 用的是 libGDX 的日志系统。可以临时把级别调到 `APPLICATION` 看更多信息：

```java
Gdx.app.setLogLevel(Application.LOG_DEBUG);
```

### 渲染调试

`MazeGroup.drawChildren` 里有 `ScissorStack` 剪裁，如果地牢视图区域显示异常，可以临时注释掉剪裁来看完整渲染。

### 资源加载调试

`Resources.loadGameResources()` 用 `AssetManager` 异步加载，可以在 `finishLoading()` 前打印进度：

```java
System.out.println("Progress: " + assetManager.getProgress());
```

## 下一步

跑起来后，建议读 [05-libgdx-crash-course.md](05-libgdx-crash-course.md) 了解 libGDX 基本概念，再看 [06-roadmap.md](06-roadmap.md) 决定接下来做什么。

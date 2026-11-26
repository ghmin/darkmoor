# 模块 11 - `exception` + `stub` + `util`

这三个小包放一起讲，因为都不复杂。

## `com.ntk.darkmoor.exception` - 异常体系

**职责**：项目自定义异常，全部继承 `BaseException`（继承 `RuntimeException`，是 unchecked exception）。

| 类 | 角色 |
|---|---|
| `BaseException` | 基类 |
| `InitializationException` | 初始化失败 |
| `LoadException` | 资源加载失败 |
| `ResourceException` | 资源错误 |
| `SaveException` | 存档错误 |
| `StateException` | 状态机错误 |

### BaseException

**位置**：`core/src/com/ntk/darkmoor/exception/BaseException.java`

**字段**：
```java
protected String errorCause;
protected Throwable throwable;
```

**构造函数**：
```java
public BaseException(String errorCause) {
    this.errorCause = errorCause;
}

public BaseException(Throwable t) {
    this.throwable = t;
    errorCause = t.getMessage();
}

public String getErrorCause() {
    return errorCause;
}
```

### 子类

子类都很简单，只是给 `BaseException` 起个语义化名字：

```java
public class LoadException extends BaseException {
    public LoadException(String errorCause) { super(errorCause); }
    public LoadException(Throwable t) { super(t); }
}
```

**使用示例**：
```java
if (fileHandle == null) {
    throw new LoadException("[GraphicAssets]: file '" + fileName + "' could not be opened");
}

try {
    // 解析 XML
} catch (IOException e) {
    throw new LoadException(e);  // 包装底层异常
}
```

**好处**：调用方可以用 `catch (LoadException e)` 单独捕获加载错误，而不是 `catch (Exception e)` 一网打尽。

## `com.ntk.darkmoor.stub` - 占位类

**职责**：从 C# XNA 移植时留下的占位类，多数已废弃。

> **背景**：原作者最初用 C# + XNA 写游戏，后来移植到 Java + libGDX。移植时很多 C# 概念（XNA 的 `Mouse`、`GameTime`、`Scene` 等）在 libGDX 里有对应实现，但原作者保留了这些 stub 类作为"以后再处理"的占位。结果一直没处理。

| 类 | 原本想做的事 | v0.5 实际状态 |
|---|---|---|
| `Display` | 显示工具（GL 清屏等） | 只有 `clearBuffers()` 实现了，其他空 |
| `Mouse` | 鼠标状态 | 空壳，应改用 `Gdx.input` |
| `MouseButtons` | 鼠标按键枚举 | 空壳，应改用 `Gdx.Input.Buttons` |
| `GameTime` | 游戏时间 | 空壳，应改用 `Gdx.graphics.getDeltaTime()` |
| `Scene` | 场景 | 空壳，应改用 libGDX `Stage` |
| `AudioSample` | 音效 | 空壳，应改用 `Gdx.audio.newSound()` |
| `ScriptInterface` | 脚本接口 | 空壳 |

### Display

**位置**：`core/src/com/ntk/darkmoor/stub/Display.java`

**实现部分**：
```java
public static void clearBuffers() {
    Gdx.gl.glClearColor(0, 0, 0, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
}
```

其他方法（`pushScissor` / `popScissor` / `init`）都是空实现，有 `// TODO Auto-generated method stub` 注释。

**`GameScreenBase.render()` 用到**：
```java
Display.clearBuffers();  // 每帧清屏
```

### 其他 stub 类

都是空壳，未来重构时应该删除并改用 libGDX 原生 API。

**重构建议**（v0.6+）：
1. `Mouse` → 直接用 `Gdx.input.getX()` / `getY()` / `isButtonPressed()`
2. `MouseButtons` → 用 `com.badlogic.gdx.Input.Buttons`
3. `GameTime` → 用 `Gdx.graphics.getDeltaTime()` / `getRawDeltaTime()`
4. `Scene` → 用 libGDX `Stage`
5. `AudioSample` → 用 `com.badlogic.gdx.audio.Sound`
6. `ScriptInterface` → 评估是否还需要

## `com.ntk.darkmoor.util` - 通用工具

只有 1 个类。

### RandomEnum

**位置**：`core/src/com/ntk/darkmoor/util/RandomEnum.java`

**职责**：从枚举里随机取一个值。

**用法**：
```java
public enum HeroRace { HUMAN, ELF, DWARF, HALFLING }

HeroRace random = RandomEnum.get(HeroRace.class);
// 可能返回 HUMAN/ELF/DWARF/HALFLING 中的任意一个
```

**实现**：
```java
public static <T extends Enum<?>> T get(Class<T> enumClass) {
    T[] values = enumClass.getEnumConstants();
    return values[Random.nextInt(values.length)];
}
```

## `com.ntk.darkmoor.engine.interfaces` - 抽象接口

| 接口 | 用途 |
|---|---|
| `IItem` | 物品接口（标记用） |
| `IMonster` | 怪物接口（标记用） |
| `ISpell` | 法术接口（标记用） |

**v0.5 状态**：接口已定义，但 `Item` / `Monster` / `Spell` 类**没有**显式 `implements` 这些接口。属于历史遗留，可能原作者计划后期再补。

## v0.5 总体状态

| 包 | 状态 |
|---|---|
| `exception` | ✅ 完整可用 |
| `stub` | ⚠️ 多数是空实现，但有少量被实际使用（如 `Display.clearBuffers()`） |
| `util` | ✅ 完整可用 |
| `engine.interfaces` | ⚠️ 接口已定义但未被实现 |

## 学习建议

- `exception` 包：5 分钟扫一遍，知道有哪些异常类型即可
- `stub` 包：**不要学**，这是反例。知道它存在即可，未来重构时删掉
- `util` 包：看一眼 `RandomEnum.java` 就够

## 给维护者的建议

1. **不要**在 `stub` 里加新代码——这是要被删除的包
2. **可以**在 `exception` 里加新异常类型（继承 `BaseException`）
3. **可以**在 `util` 里加新工具类（但要真的"通用"，不能是只在一处用的东西）
4. 看到 `stub` 里的类被实际使用时（如 `Display.clearBuffers()`），考虑用 libGDX 原生 API 替换

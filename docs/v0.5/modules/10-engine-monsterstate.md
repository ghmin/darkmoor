# 模块 10 - `com.ntk.darkmoor.engine.monsterstate`

## 概览

怪物 AI 状态机。每个 `Monster` 持有一个 `MonsterState`，根据当前情境切换状态。

| 类 | 文件 | 角色 |
|---|---|---|
| `MonsterState` | `MonsterState.java` | 状态基类 |
| `StateAdapter` | `StateAdapter.java` | 空实现适配器 |
| `Idle` | `Idle.java` | 待机状态 |
| `Attack` | `Attack.java` | 攻击状态 |
| `Hurt` | `Hurt.java` | 受伤状态 |

## MonsterState（基类）

**位置**：`core/src/com/ntk/darkmoor/engine/monsterstate/MonsterState.java`

**继承**：`StateAdapter`（空实现适配器，便于子类只重写关心的方法）

**职责**：怪物状态基类，持有 `Monster` 引用。

**字段**：
```java
private Monster monster;
```

**构造函数**：
```java
public MonsterState(Monster monster) {
    if (monster == null)
        throw new StateException("Monster parameter is missing in MonsterState initialization");
    this.setMonster(monster);
}
```

**主要方法**（继承自 `StateAdapter`，子类按需重写）：
```java
public void enter()    // 进入状态时调用
public void update()   // 每帧调用
public void exit()     // 离开状态时调用
```

## StateAdapter

**位置**：`core/src/com/ntk/darkmoor/engine/monsterstate/StateAdapter.java`

**职责**：状态适配器，所有方法空实现。子类继承它就不用重写所有方法。

```java
public abstract class StateAdapter {
    public void enter() {}
    public void update() {}
    public void exit() {}
}
```

## Idle

**位置**：`core/src/com/ntk/darkmoor/engine/monsterstate/Idle.java`

**职责**：待机状态。怪物原地不动，但会检测玩家是否进入视野。

**update() 逻辑**：
```java
public void update() {
    if (monster.canSeeTeam()) {
        monster.setState(new Attack(monster));  // 切到攻击状态
    }
}
```

## Attack

**位置**：`core/src/com/ntk/darkmoor/engine/monsterstate/Attack.java`

**职责**：攻击状态。怪物追击玩家并攻击。

**update() 逻辑**：
```java
public void update() {
    if (!monster.canSeeTeam()) {
        monster.setState(new Idle(monster));  // 玩家逃出视野，切回待机
        return;
    }
    if (monster.isInRangeToAttack()) {
        monster.performAttack();
    } else {
        monster.moveTowardTeam();
    }
}
```

## Hurt

**位置**：`core/src/com/ntk/darkmoor/engine/monsterstate/Hurt.java`

**职责**：受伤状态。怪物被攻击后短暂硬直。

**update() 逻辑**：
```java
public void enter() {
    timer = HURT_DURATION;
}

public void update() {
    timer -= Gdx.graphics.getDeltaTime();
    if (timer <= 0) {
        monster.setState(new Idle(monster));  // 恢复
    }
}
```

## 状态转换图

```
       进入视野
   ┌──────────────┐
   ↓              │
┌──────┐  ┌──────┐│
│ Idle │←─│Attack││
└──────┘  └──────┘│
   ↑         │    │
   │         │受伤│
   │         ↓    │
   │      ┌──────┐│
   └──────│ Hurt ││
   恢复   └──────┘│
                   │
       离开视野 ───┘
```

## Monster 如何使用状态机

`Monster` 类里：
```java
private MonsterState state = new Idle(this);

public void update(float delta) {
    state.update();
}

public void setState(MonsterState newState) {
    state.exit();
    state = newState;
    state.enter();
}

public void takeDamage(int amount) {
    hp -= amount;
    setState(new Hurt(this));
}
```

## v0.5 状态

- **骨架完整**：状态机框架已搭好
- **状态切换逻辑**：已实现（canSeeTeam / isInRangeToAttack 等检查方法）
- **具体行为**：⚠️ 大部分未实现
  - `Monster.canSeeTeam()` - 未完整实现
  - `Monster.performAttack()` - 未实现
  - `Monster.moveTowardTeam()` - 未实现

**结果**：怪物能加载、能显示、能受伤切换到 Hurt 状态，但**不会主动攻击玩家**。

## 状态机模式说明

这是经典的"状态模式"（State Pattern）：
- 把对象的不同行为封装到不同的状态类里
- 状态切换通过 `setState()` 完成
- 每个状态类只关心自己的逻辑，互不干扰

优点：
- 加新状态不用改其他状态（只需加一个新类）
- 状态切换逻辑集中（在 `setState` 里）
- 容易测试（每个状态类可单独测试）

缺点：
- 类多了（每种状态一个类）
- 状态间共享数据要通过 `Monster` 引用传递

## 学习建议

1. 先读 `StateAdapter.java`（最短，5 行）
2. 再读 `MonsterState.java`（基类）
3. 再读 `Idle.java` → `Attack.java` → `Hurt.java`（看状态转换）
4. 最后看 `Monster.java` 里的 `update()` / `setState()` / `takeDamage()` 怎么串起来

## 想加新状态？

以加一个"Flee"（逃跑）状态为例：

1. 创建 `core/src/com/ntk/darkmoor/engine/monsterstate/Flee.java`：
```java
public class Flee extends MonsterState {
    public Flee(Monster monster) {
        super(monster);
    }
    
    @Override
    public void enter() {
        // 切换动画到逃跑姿态
    }
    
    @Override
    public void update() {
        monster.moveAwayFromTeam();
        if (monster.isSafe()) {
            monster.setState(new Idle(monster));
        }
    }
    
    @Override
    public void exit() {
        // 清理动画
    }
}
```

2. 在 `Monster.takeDamage()` 里加触发条件：
```java
public void takeDamage(int amount) {
    hp -= amount;
    if (hp < maxHp * 0.2) {
        setState(new Flee(this));   // HP 低于 20% 时逃跑
    } else {
        setState(new Hurt(this));
    }
}
```

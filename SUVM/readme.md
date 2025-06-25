# SUVM 简易API文档

---

## Trait 和接口说明

### `Transaction`

- **描述**：事务抽象接口，所有具体事务需继承该trait。
- **方法**：
    - `def name: String`  
      返回事务名称。
    - `def randomize(): Unit`  
      随机化事务数据。

---

### `Driver[T <: Transaction]`

- **描述**：驱动模块的基础接口。
- **方法**：
    - `def start(): Unit`  
      启动驱动。

---

### `DriverHook[T <: Transaction]`

- **描述**：驱动回调钩子，支持驱动前后操作扩展。
- **方法**：
    - `def preDrv(tx: T): Unit`  
      驱动前调用，默认空实现。
    - `def postDrv(tx: T): Unit`  
      驱动后调用，默认空实现。

---

### `DriverHandle[T <: Transaction]`

- **描述**：驱动具体事务的函数接口。
- **字段**：
    - `def drive: T => Unit`  
      驱动事务的函数。

---

### `SequenceProvider`

- **描述**：提供序列器接口。
- **字段**：
    - `def sequencer: ISequencer`  
      获取当前序列器。

---

### `BaseDriver[T <: Transaction]` extends `Driver[T]` with `ScoreboardDriven[T]` with `SequenceProvider` with `DriverHandle[T]`

- **描述**：驱动基类，负责从Sequencer获取事务并驱动，同时推送给Scoreboard。
- **构造参数**：
    - `hook: Option[DriverHook[T]]`（默认 None）驱动回调钩子。
- **方法**：
    - `override def start(): Unit`  
      启动驱动线程，循环获取事务并调用 `drive`，并调用钩子。

---

### `Monitor[T <: Transaction]`

- **描述**：监视器接口。
- **方法**：
    - `def start(): Unit`  
      启动监视器。

---

### `MonitorHook[T <: Transaction]`

- **描述**：监视器回调钩子。
- **方法**：
    - `def preMon(tx: T): Unit`  
      监视前调用，默认空实现。
    - `def postMon(tx: T): Unit`  
      监视后调用，默认空实现。

---

### `BaseMonitor[T <: Transaction]` extends `Monitor[T]` with `ScoreboardDriven[T]`

- **描述**：监视器基类，周期调用monFn采样事务并推送给Scoreboard。
- **构造参数**：
    - `monFn: () => Option[T]` 监视采样函数。
    - `hook: Option[MonitorHook[T]]` 监视回调钩子，默认 None。
- **方法**：
    - `override def start(): Unit`  
      启动监视线程，周期调用monFn。

---

### `Sequence(priority: Int)`

- **描述**：事务序列，按优先级管理事务队列。
- **字段**：
    - `priority: Int` 优先级。
- **方法**：
    - `def doItem(tx: Transaction)(init: Transaction => Unit = _ => ()): Unit`  
      添加事务到队列，并可附带初始化操作。
    - `def lock(): Boolean`  
      请求锁，成功返回true。
    - `def unlock(): Unit`  
      释放锁。
    - `def getNext: Option[Transaction]`  
      获取下一个事务。
    - 其他辅助方法：`enqueue`, `hasPending`, `isLocked` 等。

---

### `ISequencer`

- **描述**：序列器接口。
- **方法**：
    - `def register(seq: Sequence): Unit` 注册序列。
    - `def selectNext(): Option[Transaction]` 选择下一个事务。
    - `def requestLock(seq: Sequence): Boolean` 请求锁。
    - `def releaseLock(seq: Sequence): Unit` 释放锁。

---

### `Sequencer` extends `ISequencer`

- **描述**：事务序列器实现，管理多个Sequence，按优先级和锁规则选择事务。
- **方法**：
    - 实现接口所有方法。
    - 线程安全，内部维护锁和序列队列。

---

### `RefModel[T <: Transaction]`

- **描述**：参考模型接口，基于输入事务产生期望输出。
- **方法**：
    - `def process(tx: T): Seq[T]`  
      生成期望事务序列。

---

### `ScoreboardDriven[T <: Transaction]`

- **描述**：用于驱动与监视器向Scoreboard推送事务的辅助trait。
- **字段**：
    - `def scoreboard: Option[Scoreboard[T]]` 可选Scoreboard。
    - `def Active: Boolean` 用于区分推送输入（驱动）或实际输出（监视）。
- **方法**：
    - `def push(tx: T): Unit` 推送事务到Scoreboard。
    - `def objectionClear: Boolean` 判断Scoreboard的objection是否清空。

---

### `Scoreboard[T <: Transaction]`

- **描述**：Scoreboard基类，管理期望和实际事务队列，进行比较验证。
- **构造参数**：
    - `refModel: RefModel[T]`
    - `name: String`，默认 "Scoreboard"
    - `expectTotal: Option[Int]` 可选预期事务数量
- **方法**：
    - `def pushInput(tx: T): Unit`  
      驱动输入推送，触发参考模型生成期望事务。
    - `def pushActual(tx: T): Unit`  
      监视输出推送。
    - `def compareTransactions(expected: T, actual: T): Boolean`  
      抽象比较函数，子类实现。
    - `def start(thresholdCycles: Int): Unit`  
      启动比较线程，基于objection机制控制仿真流程。
    - `def isFinished: Boolean`  
      判断objection是否已清空。
    - `def withClockDomain(clock: ClockDomain): this.type`  
      绑定时钟域。
    - 其他辅助方法：`flush()`，打印未匹配事务警告等。

---

### `SimObjection`

- **描述**：简单的objection计数器，控制仿真是否结束。
- **方法**：
    - `def raise(): Unit` 增加objection计数。
    - `def drop(): Unit` 减少objection计数。
    - `def isClear: Boolean` 判断是否计数为零。

---

### `FactoryCreatable`

- **描述**：工厂注册接口。
- **字段**：
    - `def name: String`

---

### `SimpleFactory`

- **描述**：工厂单例，实现注册和实例化。
- **方法**：
    - `def register[T <: FactoryCreatable](ctor: () => T): Unit` 注册构造器。
    - `def create[T <: FactoryCreatable](name: String): Option[T]` 通过名称创建实例。
    - `def list(): Unit` 列出所有注册组件。

---

## 测试示例组件说明

- `myTx`：具体事务类，包含输入输出字段。
- `myRefModel`：简单参考模型，将输入加1作为期望输出。
- `MyScoreboard`：继承`Scoreboard`，实现具体比较逻辑。
- `MyDriver`：继承`BaseDriver`，使用传入的驱动函数。
- `MyMonitor`：继承`BaseMonitor`，周期采样DUT输出。
- `MyTop`：测试环境，实例化并启动各模块。
- `SimTop`：仿真入口，编译DUT并运行测试环境。

---

# OpenClaw Cron 调度系统架构设计

## 1. 系统概述

### 1.1 设计目标
基于研究文件中的需求，设计一个支持以下特性的分布式 Cron 调度系统：

- **动态 Cron 管理**：支持运行时添加、删除、修改 Cron 任务
- **持久化存储**：Cron 定义可持久化，重启后自动恢复
- **分布式协调**：多节点部署时避免任务重复执行
- **高可用性**：单点故障不影响整体调度

### 1.2 核心能力映射

| 需求来源 | 实现能力 | 设计要点 |
|---------|---------|---------|
| OpenClaw 研究 | 可编程调度 | 支持动态注册/取消 |
| 多节点部署 | 分布式锁 | Redis/ZooKeeper 协调 |
| 持久化需求 | Store 模块 | 支持多种存储后端 |

---

## 2. 架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        Application Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   Cron API   │  │ Cron Console │  │  Cron Event Listener │  │
│  │  (REST/gRPC) │  │   (Web UI)   │  │    (Internal)        │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
└─────────┼─────────────────┼─────────────────────┼──────────────┘
          │                 │                     │
          ▼                 ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Scheduler Core Layer                         │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                   CronScheduler (核心调度器)               │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────────┐  │  │
│  │  │ TriggerMgr │  │  JobMgr    │  │  ScheduleEngine   │  │  │
│  │  │(触发器管理) │  │ (任务管理)  │  │   (调度引擎)       │  │  │
│  │  └────────────┘  └────────────┘  └────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │  CronParser  │  │ CronMatcher  │  │   NextExecCalc     │  │
│  │  (Cron解析器)  │  │ (匹配计算)    │  │  (下次执行时间计算)   │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
          │                 │                     │
          ▼                 ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Storage Layer (存储层)                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  CronStore (存储抽象)                       │  │
│  │         ┌─────────────────┬─────────────────┐             │  │
│  │         ▼                 ▼                 ▼             │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │  │
│  │  │ JdbcStore   │  │ RedisStore  │  │ FileStore   │      │  │
│  │  │ (数据库存储)  │  │ (缓存存储)   │  │ (文件存储)   │      │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘      │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              StateStore (运行时状态存储)                    │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │  │
│  │  │ RunningJobs │  │  JobHistory │  │   ScheduleState   │  │  │
│  │  │ (运行中任务) │  │  (执行历史)  │  │    (调度状态)       │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
          │                 │                     │
          ▼                 ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Distributed Coordination Layer                   │
│                   (分布式协调层)                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              DistributedLock (分布式锁抽象)                │  │
│  │         ┌─────────────────┬─────────────────┐             │  │
│  │         ▼                 ▼                 ▼             │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │  │
│  │  │RedisLock    │  │ZooKeeperLock│  │  DBLock     │      │  │
│  │  │(Redis锁)    │  │ (ZK 锁)      │  │ (数据库锁)   │      │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘      │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │          LeaderElection (领导者选举 - 可选)                  │  │
│  │     用于决定哪个节点承担调度协调职责，减少锁竞争             │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
          │                 │                     │
          ▼                 ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Execution Layer                             │
│                       (执行层)                                    │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    JobExecutor (任务执行器)                 │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │  │
│  │  │ SyncExecutor│  │AsyncExecutor│  │   ThreadPoolMgr    │  │  │
│  │  │ (同步执行)   │  │ (异步执行)   │  │   (线程池管理)      │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              JobHandler (任务处理器接口)                     │  │
│  │         业务代码实现此接口来定义具体的任务逻辑               │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

```

### 2.2 模块详细设计

#### 2.2.1 Core 模块 - 核心调度

```
┌─────────────────────────────────────────────────────────────────┐
│                      CronScheduler                              │
│                     (调度器主入口)                               │
├─────────────────────────────────────────────────────────────────┤
│  职责:                                                          │
│  1. 接收 Cron 定义 (CronDefinition)                            │
│  2. 协调 TriggerManager + JobManager + ScheduleEngine           │
│  3. 提供生命周期管理 (start/stop/pause/resume)                  │
│  4. 集成 Store 模块进行持久化                                   │
├─────────────────────────────────────────────────────────────────┤
│  核心方法:                                                      │
│  - registerCron(CronDefinition): CronId                         │
│  - unregisterCron(CronId): void                                 │
│  - updateCron(CronDefinition): void                             │
│  - pauseCron(CronId): void                                      │
│  - resumeCron(CronId): void                                     │
│  - listCrons(): List<CronDefinition>                            │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  TriggerManager │  │    JobManager   │  │  ScheduleEngine │
│   (触发器管理)   │  │    (任务管理)    │  │    (调度引擎)   │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

**CronDefinition 核心数据结构：**

```java
/**
 * Cron 任务定义
 */
public class CronDefinition {
    private String id;                    // 唯一标识 (UUID)
    private String name;                  // 任务名称
    private String description;           // 任务描述
    private String cronExpression;        // Cron 表达式 (标准 Unix cron)
    private String timezone;              // 时区 (默认系统时区)
    private JobType jobType;              // 任务类型
    private String jobHandler;            // 处理器 Bean 名称或全类名
    private Map<String, Object> jobData;  // 任务执行参数
    private CronStatus status;            // 状态 (ENABLED/DISABLED)
    private Long startTime;               // 生效开始时间
    private Long endTime;                 // 生效结束时间
    private String createdBy;             // 创建人
    private Long createdAt;               // 创建时间
    private Long updatedAt;               // 更新时间
}

public enum JobType {
    SPRING_BEAN,      // Spring Bean 名称
    CLASS_NAME,       // 全限定类名
    GROOVY_SCRIPT,    // Groovy 脚本
    HTTP_REQUEST      // HTTP 请求
}

public enum CronStatus {
    ENABLED,   // 启用
    DISABLED,  // 禁用
    PAUSED     // 暂停
}
```

#### 2.2.2 Store 模块 - 存储层

```
┌─────────────────────────────────────────────────────────────────┐
│                        CronStore                                │
│                      (存储抽象接口)                                │
├─────────────────────────────────────────────────────────────────┤
│  职责:                                                          │
│  1. 定义 CronDefinition 的 CRUD 操作                            │
│  2. 支持批量操作                                                │
│  3. 提供事务支持 (如果底层支持)                                  │
├─────────────────────────────────────────────────────────────────┤
│  核心方法:                                                      │
│  - save(CronDefinition): void                                   │
│  - update(CronDefinition): void                                 │
│  - delete(String id): void                                      │
│  - findById(String id): Optional<CronDefinition>                │
│  - findAll(): List<CronDefinition>                              │
│  - findByStatus(CronStatus): List<CronDefinition>               │
│  - batchSave(List<CronDefinition>): void                        │
│  - exists(String id): boolean                                   │
└─────────────────────────────────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   JdbcStore     │  │   RedisStore    │  │   FileStore     │
│    (JDBC实现)    │  │   (Redis实现)    │  │   (文件实现)     │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

**存储实现说明：**

1. **JdbcStore** - 数据库存储
   - 使用 JdbcTemplate 或 JPA
   - 自动建表脚本
   - 支持事务

2. **RedisStore** - Redis 存储
   - 使用 Hash 结构存储 Cron 定义
   - 支持 TTL
   - 轻量级部署

3. **FileStore** - 文件存储
   - JSON/YAML 格式
   - 适合单节点或开发环境
   - 文件监听自动重载

#### 2.2.3 Distributed Lock 模块 - 分布式锁

```
┌─────────────────────────────────────────────────────────────────┐
│                      DistributedLock                          │
│                      (分布式锁抽象)                               │
├─────────────────────────────────────────────────────────────────┤
│  职责:                                                          │
│  1. 确保同一时刻只有一个节点执行某个 Cron 任务                    │
│  2. 防止脑裂和死锁                                              │
│  3. 支持可重入锁 (可选)                                          │
│  4. 提供锁续期机制 (防止长时间任务导致锁过期)                     │
├─────────────────────────────────────────────────────────────────┤
│  核心方法:                                                      │
│  - lock(String lockKey, long timeout): boolean                  │
│  - tryLock(String lockKey, long waitTime, long leaseTime): boolean│
│  - unlock(String lockKey): void                                 │
│  - isLocked(String lockKey): boolean                              │
│  - renewLock(String lockKey, long additionalTime): boolean        │
└─────────────────────────────────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│    RedisLock    │  │ ZooKeeperLock   │  │     DBLock      │
│   (Redis实现)    │  │    (ZK实现)      │  │   (数据库锁)     │
│                 │  │                 │  │                 │
│  Redisson/RedLock│  │  Curator       │  │  悲观锁/乐观锁   │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

**锁实现策略对比：**

| 实现方式 | 优点 | 缺点 | 适用场景 |
|---------|------|------|---------|
| RedisLock | 性能高，实现简单 | 需要处理脑裂 | 大多数场景 |
| ZooKeeperLock | 可靠性高，无单点 | 部署复杂，性能较低 | 金融级可靠性 |
| DBLock | 无需额外组件 | 性能差，单点问题 | 简单场景 |

**Redis 分布式锁设计细节：**

```java
/**
 * Redis 分布式锁实现 (基于 Redisson)
 */
public class RedisDistributedLock implements DistributedLock {
    
    private RedissonClient redisson;
    
    // 锁的键前缀
    private static final String LOCK_PREFIX = "cron:lock:";
    // 默认锁过期时间 (30秒)
    private static final long DEFAULT_LOCK_TTL = 30000;
    // 看门狗续期时间 (锁过期时间的1/3)
    private static final long WATCH_DOG_INTERVAL = 10000;
    
    @Override
    public boolean lock(String lockKey) {
        RLock lock = redisson.getLock(LOCK_PREFIX + lockKey);
        // 尝试获取锁，使用看门狗自动续期
        return lock.tryLock();
    }
    
    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redisson.getLock(LOCK_PREFIX + lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    @Override
    public void unlock(String lockKey) {
        RLock lock = redisson.getLock(LOCK_PREFIX + lockKey);
        // 只有持有锁的线程才能解锁
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 2.2.4 Execution 模块 - 执行层

```
┌─────────────────────────────────────────────────────────────────┐
│                        JobExecutor                              │
│                       (任务执行器)                                │
├─────────────────────────────────────────────────────────────────┤
│  职责:                                                          │
│  1. 管理执行线程池                                               │
│  2. 分发任务到具体的 JobHandler                                  │
│  3. 记录执行结果和日志                                           │
│  4. 支持超时控制和熔断                                           │
├─────────────────────────────────────────────────────────────────┤
│  核心组件:                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │
│  │ThreadPool   │  │ JobContext  │  │   ExecutionListener    │   │
│  │Executor     │  │ (执行上下文) │  │    (执行监听器)         │   │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   JobHandler    │  │   JobHandler    │  │   JobHandler    │
│  (Spring Bean)  │  │ (Groovy Script) │  │  (HTTP Request) │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

**执行上下文 (JobContext)：**

```java
/**
 * 任务执行上下文
 */
public class JobContext {
    private String cronId;           // Cron 定义 ID
    private String executionId;      // 本次执行唯一 ID
    private Long fireTime;           // 触发时间
    private Long scheduledFireTime;  // 计划触发时间
    private Long prevFireTime;       // 上次触发时间
    private Long nextFireTime;       // 下次触发时间
    private Integer refireCount;     // 重试次数
    private Map<String, Object> jobData; // 任务数据
    private Map<String, Object> transientData; // 临时数据
    
    // 执行控制
    public boolean isRecovering();   // 是否恢复执行
    public boolean isRefireImmediately(); // 是否立即重试
    public void setResult(Object result); // 设置执行结果
    public Object getResult();        // 获取执行结果
}
```

---

## 3. 核心流程设计

### 3.1 Cron 注册流程

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   API    │────▶│ CronService  │────▶│  CronParser  │────▶│    Store     │
│  Request │     │   (业务校验)   │     │  (表达式验证)  │     │  (持久化)     │
└──────────┘     └──────────────┘     └──────────────┘     └──────────────┘
                                                                   │
                                                                   ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐    ┌──────────┐
│   Trigger    │◀────│  Scheduler   │◀────│    Event     │◀───│  Success │
│   (创建)      │     │ (调度注册)    │     │   (事件发布)  │    │          │
└──────────────┘     └──────────────┘     └──────────────┘    └──────────┘
```

**关键步骤说明：**

1. **API 层**：接收 REST/gRPC 请求，进行基础参数校验
2. **CronService**：业务逻辑校验（名称唯一性、权限等）
3. **CronParser**：验证 Cron 表达式合法性，计算最近执行时间
4. **Store**：持久化到数据库/Redis/文件
5. **Event**：发布 CronRegisteredEvent 事件
6. **Scheduler**：将 Cron 注册到调度引擎，创建 Trigger
7. **Trigger**：开始监听下一次触发时机

### 3.2 调度执行流程

```
时间线 ──────────────────────────────────────────────────────────────▶

节点 A                              节点 B                              节点 C
┌──────────┐                       ┌──────────┐                       ┌──────────┐
│ 10:00:00 │                       │ 10:00:00 │                       │ 10:00:00 │
│ 触发检查  │                       │ 触发检查  │                       │ 触发检查  │
└────┬─────┘                       └────┬─────┘                       └────┬─────┘
     │                                │                                │
     ▼                                ▼                                ▼
┌──────────┐                       ┌──────────┐                       ┌──────────┐
│ 尝试获取  │                       │ 尝试获取  │                       │ 尝试获取  │
│ 分布式锁  │                       │ 分布式锁  │                       │ 分布式锁  │
└────┬─────┘                       └────┬─────┘                       └────┬─────┘
     │                                │                                │
     │ SET cron:lock:job1 NX EX 30    │ SET cron:lock:job1 NX EX 30    │ SET cron:lock:job1 NX EX 30
     │ ────────────────────────────────▶│────────────────────────────────▶│
     │                                │                                │
     │ OK (获取成功)                   │ (nil) 获取失败                   │ (nil) 获取失败
     │◀────────────────────────────────│◀────────────────────────────────│
     │                                │                                │
     ▼                                ▼                                ▼
┌──────────┐                       ┌──────────┐                       ┌──────────┐
│ 执行任务  │                       │ 跳过执行  │                       │ 跳过执行  │
│ 并更新锁  │                       │ 等待下次  │                       │ 等待下次  │
│ 过期时间  │                       │ 触发时机  │                       │ 触发时机  │
└────┬─────┘                       └──────────┘                       └──────────┘
     │
     │ 任务执行完成
     ▼
┌──────────┐
│ 释放锁   │ DEL cron:lock:job1
│ 记录日志  │
└──────────┘
```

**执行流程详细说明：**

1. **触发检查**：每个节点独立计算 Cron 触发时机，到达触发时间时开始竞争
2. **分布式锁**：使用 Redis SET key NX EX 原子命令获取锁，30秒过期时间
3. **竞争结果**：
   - 获取成功的节点：执行任务，启动看门狗线程续期锁
   - 获取失败的节点：直接返回，等待下次触发时机
4. **任务执行**：调用 JobHandler 执行业务逻辑
5. **锁续期**：看门狗每 10 秒检查一次，若任务未完成则续期锁
6. **完成清理**：任务完成后释放锁，记录执行日志

### 3.3 集群加载与恢复流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    节点启动时加载流程                            │
└─────────────────────────────────────────────────────────────────┘

节点启动
    │
    ▼
┌──────────────┐
│  初始化 Store │◀──── 连接存储 (DB/Redis/File)
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  加载 Cron   │◀──── 查询所有 status = ENABLED 的 Cron
│  定义列表     │       FROM cron_definition
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  校验 Cron   │◀──── 验证表达式合法性、检查 jobHandler 存在性
│  定义有效性   │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  注册到调度   │◀──── 调用 CronScheduler.registerCron()
│  引擎        │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  启动完成     │◀──── 发布 SchedulerStartedEvent
└──────────────┘


┌─────────────────────────────────────────────────────────────────┐
│                   运行时动态新增 Cron 流程                        │
└─────────────────────────────────────────────────────────────────┘

API 调用 registerCron()
        │
        ▼
┌───────────────┐
│  参数校验      │◀──── 校验 cronExpression、jobHandler
└───────┬───────┘
        │
        ▼
┌───────────────┐
│  持久化存储    │◀──── 调用 CronStore.save()
└───────┬───────┘
        │
        ▼
┌───────────────┐
│  发布事件      │◀──── 发布 CronRegisteredEvent
└───────┬───────┘
        │
        ├──────────────────────────────────────┐
        │                                      │
        ▼                                      ▼
┌───────────────┐                    ┌───────────────┐
│  本地节点处理  │                    │  其他节点监听  │
│  (当前节点)   │                    │  (通过消息队列) │
└───────┬───────┘                    └───────┬───────┘
        │                                  │
        ▼                                  ▼
┌───────────────┐                    ┌───────────────┐
│  注册到调度器  │                    │  加载 Cron    │
│               │                    │  并注册       │
└───────────────┘                    └───────────────┘


事件传播方案选择：

方案 A: 基于 Spring Event (本地广播)
- 适用: 单节点 或 共享数据库轮询
- 缺点: 多节点时无法通知其他节点

方案 B: 基于 Redis Pub/Sub
- 适用: 已使用 Redis 作为分布式锁
- 优点: 轻量，无需额外组件
- 缺点: 消息不持久化

方案 C: 基于消息队列 (RabbitMQ/Kafka)
- 适用: 已有 MQ 基础设施
- 优点: 可靠投递，可重放
- 缺点: 引入额外复杂度

推荐: 方案 B (Redis Pub/Sub) 作为默认实现，
      提供接口可扩展为方案 C
```

### 3.4 心跳与故障检测

```
┌─────────────────────────────────────────────────────────────────┐
│                    节点心跳机制 (可选增强)                         │
└─────────────────────────────────────────────────────────────────┘

                    每个节点每 30 秒发送心跳
                    
    Node A               Redis (Hash)              Node B
      │                                          │
      │  HSET cron:heartbeat node-a timestamp    │
      │─────────────────────────────────────────▶│
      │                                          │
      │                                          │ 定时检查心跳
      │                                          │ 发现 node-a 心跳超时
      │  发现 node-b 心跳超时                     │ (超过 90 秒)
      │◀─────────────────────────────────────────│
      │                                          │
      ▼                                          ▼
  接管 node-b 的锁                            接管 node-a 的锁
  (可选：检查并清理)                           (可选：检查并清理)


心跳数据结构：

Key: cron:heartbeat (Redis Hash)
Field: node-id (节点唯一标识)
Value: {
  "heartbeatTime": 1700000000000,  // 最后心跳时间戳
  "ip": "192.168.1.100",          // 节点 IP
  "startedAt": 1699990000000,     // 启动时间
  "cronCount": 10                 // 当前加载的 Cron 数量
}

故障检测策略：
1. 心跳超时: 超过 3 个心跳周期 (默认 90 秒) 未收到心跳
2. 锁续期失败: 持有锁的节点无法续期 (网络分区)
3. 优雅退出: 节点关闭时主动释放锁和心跳

故障处理流程：
1. 其他节点检测到故障节点
2. 检查故障节点持有的锁
3. 若锁已过期，直接获取并执行
4. 若锁未过期但节点已死，等待锁自然过期 (避免脑裂)
5. 可选：管理员手动强制释放锁
```

---

## 4. 数据模型设计

### 4.1 数据库表结构 (JdbcStore)

```sql
-- ============================================
-- Cron 调度系统数据库表结构
-- 支持 MySQL/PostgreSQL/Oracle
-- ============================================

-- 1. Cron 定义主表
CREATE TABLE cron_definition (
    id                  VARCHAR(64) PRIMARY KEY COMMENT '唯一标识 (UUID)',
    name                VARCHAR(128) NOT NULL COMMENT '任务名称',
    description         VARCHAR(512) COMMENT '任务描述',
    cron_expression     VARCHAR(64) NOT NULL COMMENT 'Cron 表达式',
    timezone            VARCHAR(32) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    job_type            VARCHAR(32) NOT NULL COMMENT '任务类型: BEAN/CLASS/GROOVY/HTTP',
    job_handler         VARCHAR(256) NOT NULL COMMENT '处理器标识 (Bean名/类名/URL)',
    job_data            TEXT COMMENT '任务参数 (JSON格式)',
    status              VARCHAR(16) DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED/PAUSED',
    start_time          BIGINT COMMENT '生效开始时间 (时间戳)',
    end_time            BIGINT COMMENT '生效结束时间 (时间戳)',
    misfire_policy      VARCHAR(32) DEFAULT 'IGNORE' COMMENT '错过触发策略: IGNORE/FIRE_NOW/',
    max_concurrent      INT DEFAULT 1 COMMENT '最大并发执行数 (0=无限制)',
    timeout_ms          INT DEFAULT 0 COMMENT '任务超时时间 (0=无限制)',
    retry_count         INT DEFAULT 0 COMMENT '失败重试次数',
    retry_interval_ms   INT DEFAULT 0 COMMENT '重试间隔',
    created_by          VARCHAR(64) COMMENT '创建人',
    created_at          BIGINT NOT NULL COMMENT '创建时间',
    updated_at          BIGINT NOT NULL COMMENT '更新时间',
    version             INT DEFAULT 0 COMMENT '乐观锁版本号',
    
    INDEX idx_status (status),
    INDEX idx_job_type (job_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Cron 任务定义表';


-- 2. 任务执行历史表 (分表或分区 recommended)
CREATE TABLE cron_execution_history (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id        VARCHAR(64) NOT NULL COMMENT '本次执行唯一ID',
    cron_id             VARCHAR(64) NOT NULL COMMENT 'Cron定义ID',
    cron_name           VARCHAR(128) COMMENT 'Cron名称(快照)',
    trigger_time        BIGINT NOT NULL COMMENT '计划触发时间',
    actual_fire_time    BIGINT COMMENT '实际开始执行时间',
    complete_time       BIGINT COMMENT '执行完成时间',
    duration_ms         INT COMMENT '执行耗时',
    status              VARCHAR(32) NOT NULL COMMENT '状态: RUNNING/SUCCESS/FAIL/ TIMEOUT/CANCEL',
    execute_node        VARCHAR(64) COMMENT '执行节点标识',
    execute_thread      VARCHAR(64) COMMENT '执行线程名',
    result_data         TEXT COMMENT '执行结果数据',
    error_message       TEXT COMMENT '错误信息',
    stack_trace         TEXT COMMENT '异常堆栈',
    retry_count         INT DEFAULT 0 COMMENT '当前重试次数',
    parent_execution_id VARCHAR(64) COMMENT '父执行ID (重试关联)',
    
    INDEX idx_cron_id (cron_id),
    INDEX idx_execution_id (execution_id),
    INDEX idx_trigger_time (trigger_time),
    INDEX idx_status (status),
    INDEX idx_execute_node (execute_node)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Cron 执行历史表';


-- 3. 分布式锁记录表 (可选，用于 DBLock)
CREATE TABLE cron_distributed_lock (
    lock_key            VARCHAR(128) PRIMARY KEY COMMENT '锁标识',
    lock_value          VARCHAR(64) NOT NULL COMMENT '锁持有者标识 (节点ID+线程ID)',
    expire_time         BIGINT NOT NULL COMMENT '锁过期时间戳',
    acquired_time       BIGINT NOT NULL COMMENT '获取锁时间',
    reentrant_count     INT DEFAULT 1 COMMENT '重入次数',
    
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分布式锁表';


-- 4. 节点心跳表 (可选，用于故障检测)
CREATE TABLE cron_node_heartbeat (
    node_id             VARCHAR(64) PRIMARY KEY COMMENT '节点唯一标识',
    node_ip             VARCHAR(32) NOT NULL COMMENT '节点IP',
    node_port           INT COMMENT '服务端口',
    started_at          BIGINT NOT NULL COMMENT '启动时间',
    last_heartbeat      BIGINT NOT NULL COMMENT '最后心跳时间',
    cron_count          INT DEFAULT 0 COMMENT '当前加载的Cron数量',
    executing_count     INT DEFAULT 0 COMMENT '正在执行的任务数',
    status              VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ SUSPECT/DOWN',
    metadata            TEXT COMMENT '扩展元数据 (JSON)',
    
    INDEX idx_last_heartbeat (last_heartbeat),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点心跳表';

```

### 4.2 核心领域模型

```java
/**
 * ==================== 领域模型关系图 ====================
 * 
 * ┌──────────────────┐       ┌──────────────────┐
 * │  CronDefinition  │───────│   CronTrigger   │
 * │    (定义域)       │  1:1  │    (触发域)      │
 * └────────┬─────────┘       └────────┬─────────┘
 *          │                        │
 *          │ 1:N                    │ 1:N
 *          ▼                        ▼
 * ┌──────────────────┐       ┌──────────────────┐
 * │ ExecutionHistory │       │  TriggerEvent    │
 * │    (执行历史域)   │       │    (事件域)      │
 * └──────────────────┘       └──────────────────┘
 * 
 * =======================================================
 */

/**
 * Cron 定义 - 聚合根
 */
@Table(name = "cron_definition")
public class CronDefinition {
    
    // ==================== 标识 ====================
    @Id
    private String id;                    // 业务主键 (UUID)
    
    // ==================== 基本信息 ====================
    private String name;                  // 任务名称
    private String description;           // 任务描述
    private String createdBy;             // 创建人
    
    // ==================== 调度配置 ====================
    private String cronExpression;        // Cron 表达式
    private String timezone;              // 时区
    private Long startTime;               // 生效开始时间
    private Long endTime;                 // 生效结束时间
    
    // ==================== 任务配置 ====================
    private JobType jobType;              // 任务类型
    private String jobHandler;            // 处理器标识
    private Map<String, Object> jobData;  // 任务参数
    
    // ==================== 执行策略 ====================
    private MisfirePolicy misfirePolicy;  // 错过触发策略
    private Integer maxConcurrent;        // 最大并发数
    private Integer timeoutMs;            // 超时时间
    private Integer retryCount;           // 重试次数
    private Integer retryIntervalMs;      // 重试间隔
    
    // ==================== 状态管理 ====================
    private CronStatus status;            // 状态
    private Long createdAt;               // 创建时间
    private Long updatedAt;               // 更新时间
    private Integer version;              // 乐观锁版本
    
    // ==================== 领域方法 ====================
    
    /**
     * 验证 Cron 定义的有效性
     */
    public ValidationResult validate() {
        // 验证表达式合法性
        // 验证时间范围有效性
        // 验证处理器有效性
    }
    
    /**
     * 检查是否应在当前时间触发
     */
    public boolean shouldFireAt(long timestamp) {
        // 检查生效时间范围
        // 检查状态
        // 使用 CronExpression 计算
    }
    
    /**
     * 计算下次触发时间
     */
    public long nextFireTime(long afterTimestamp) {
        // 基于 CronExpression 计算
    }
    
    /**
     * 更新状态
     */
    public void updateStatus(CronStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * 创建执行上下文
     */
    public ExecutionContext createExecutionContext(long triggerTime) {
        // 构建执行上下文
    }
}


/**
 * 执行历史 - 值对象
 */
@Table(name = "cron_execution_history")
public class ExecutionHistory {
    
    @Id
    private Long id;                      // 数据库自增 ID
    
    private String executionId;           // 本次执行唯一 ID
    private String cronId;                // Cron 定义 ID
    private String cronName;              // Cron 名称 (快照)
    
    // 时间信息
    private Long triggerTime;             // 计划触发时间
    private Long actualFireTime;          // 实际开始时间
    private Long completeTime;            // 完成时间
    private Integer durationMs;           // 执行耗时
    
    // 执行信息
    private ExecutionStatus status;       // 执行状态
    private String executeNode;           // 执行节点
    private String executeThread;         // 执行线程
    
    // 结果信息
    private String resultData;            // 结果数据 (JSON)
    private String errorMessage;          // 错误信息
    private String stackTrace;            // 异常堆栈
    
    // 重试信息
    private Integer retryCount;           // 当前重试次数
    private String parentExecutionId;     // 父执行 ID
    
    // 扩展字段
    private String ext1;                  // 扩展字段 1
    private String ext2;                  // 扩展字段 2
}


/**
 * 分布式锁记录 - 实体
 */
@Table(name = "cron_distributed_lock")
public class DistributedLockRecord {
    
    @Id
    private String lockKey;               // 锁标识
    
    private String lockValue;             // 锁持有者 (节点ID+线程ID)
    private Long expireTime;              // 锁过期时间戳
    private Long acquiredTime;            // 获取锁时间
    private Integer reentrantCount;       // 重入次数
}
```

---

## 5. 关键算法设计

### 5.1 Cron 表达式解析算法

```java
/**
 * Cron 表达式解析器
 * 支持标准 Unix Cron 格式: 秒 分 时 日 月 周 年(可选)
 */
public class CronExpression {
    
    // Cron 字段 (秒 分 时 日 月 周)
    private final CronField seconds;      // 0-59
    private final CronField minutes;      // 0-59
    private final CronField hours;        // 0-23
    private final CronField daysOfMonth;  // 1-31
    private final CronField months;       // 1-12 or JAN-DEC
    private final CronField daysOfWeek;   // 0-7 or SUN-SAT (0,7=SUN)
    
    private final String expression;      // 原始表达式
    private final String timezone;        // 时区
    
    /**
     * 计算下次触发时间
     * @param afterTime 从此时间之后开始计算
     * @return 下次触发时间戳，如果没有则返回 null
     */
    public Long getNextValidTimeAfter(long afterTime) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        calendar.setTimeInMillis(afterTime);
        calendar.add(Calendar.SECOND, 1); // 从下一秒开始
        calendar.set(Calendar.MILLISECOND, 0);
        
        // 最大尝试次数 (防止无限循环)
        int maxAttempts = 366 * 24 * 60; // 一年的分钟数
        int attempts = 0;
        
        while (attempts++ < maxAttempts) {
            // 保存当前时间，用于回退
            long currentTime = calendar.getTimeInMillis();
            
            // 1. 匹配月份
            if (!months.matches(calendar.get(Calendar.MONTH) + 1)) {
                calendar.add(Calendar.MONTH, 1);
                setToFirstDayOfMonth(calendar);
                continue;
            }
            
            // 2. 匹配日期 (日-月-周 的复杂逻辑)
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0=SUN
            
            boolean dayMatches = daysOfMonth.matches(dayOfMonth) || daysOfMonth.isNoSpecificValue();
            boolean weekMatches = daysOfWeek.matches(dayOfWeek) || daysOfWeek.isNoSpecificValue();
            
            // 标准 Cron: 日和周不能同时指定，一个必须是 ?
            if (!dayMatches || !weekMatches) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                setToStartOfDay(calendar);
                continue;
            }
            
            // 3. 匹配小时
            if (!hours.matches(calendar.get(Calendar.HOUR_OF_DAY))) {
                calendar.add(Calendar.HOUR_OF_DAY, 1);
                setToStartOfHour(calendar);
                continue;
            }
            
            // 4. 匹配分钟
            if (!minutes.matches(calendar.get(Calendar.MINUTE))) {
                calendar.add(Calendar.MINUTE, 1);
                calendar.set(Calendar.SECOND, 0);
                continue;
            }
            
            // 5. 匹配秒
            if (!seconds.matches(calendar.get(Calendar.SECOND))) {
                calendar.add(Calendar.SECOND, 1);
                continue;
            }
            
            // 全部匹配成功，返回时间戳
            return calendar.getTimeInMillis();
        }
        
        // 超过最大尝试次数，返回 null (可能是不合法的 Cron 如 2月31日)
        return null;
    }
    
    /**
     * 计算最近的触发时间 (在 beforeTime 之前)
     */
    public Long getPreviousValidTimeBefore(long beforeTime) {
        // 类似 getNextValidTimeAfter，但反向计算
        // ...
    }
    
    /**
     * 检查给定时间是否匹配 Cron 表达式
     */
    public boolean isSatisfiedBy(long timestamp) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        calendar.setTimeInMillis(timestamp);
        
        return seconds.matches(calendar.get(Calendar.SECOND))
            && minutes.matches(calendar.get(Calendar.MINUTE))
            && hours.matches(calendar.get(Calendar.HOUR_OF_DAY))
            && daysOfMonth.matches(calendar.get(Calendar.DAY_OF_MONTH))
            && months.matches(calendar.get(Calendar.MONTH) + 1)
            && daysOfWeek.matches(calendar.get(Calendar.DAY_OF_WEEK) - 1);
    }
    
    // 辅助方法
    private void setToFirstDayOfMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        setToStartOfDay(calendar);
    }
    
    private void setToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
    
    private void setToStartOfHour(Calendar calendar) {
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}


/**
 * Cron 字段解析器
 */
public class CronField {
    
    private final Set<Integer> values = new TreeSet<>();
    private final int min;
    private final int max;
    private final boolean noSpecificValue; // 是否为 ? (不指定)
    
    public CronField(String expression, int min, int max) {
        this.min = min;
        this.max = max;
        this.noSpecificValue = "?".equals(expression);
        
        if (!noSpecificValue) {
            parse(expression);
        }
    }
    
    private void parse(String expression) {
        String[] parts = expression.split(",");
        for (String part : parts) {
            parsePart(part.trim());
        }
    }
    
    private void parsePart(String part) {
        // 处理 * (所有值)
        if ("*".equals(part)) {
            for (int i = min; i <= max; i++) {
                values.add(i);
            }
            return;
        }
        
        // 处理 */N (步长)
        if (part.startsWith("*/")) {
            int step = Integer.parseInt(part.substring(2));
            for (int i = min; i <= max; i += step) {
                values.add(i);
            }
            return;
        }
        
        // 处理 N-M (范围)
        if (part.contains("-")) {
            String[] range = part.split("-");
            int start = parseValue(range[0]);
            int end = parseValue(range[1]);
            for (int i = start; i <= end; i++) {
                values.add(i);
            }
            return;
        }
        
        // 处理 N/M (从N开始，步长M)
        if (part.contains("/")) {
            String[] rangeStep = part.split("/");
            int start = parseValue(rangeStep[0]);
            int step = Integer.parseInt(rangeStep[1]);
            for (int i = start; i <= max; i += step) {
                values.add(i);
            }
            return;
        }
        
        // 单个值
        values.add(parseValue(part));
    }
    
    private int parseValue(String value) {
        // 处理月份和星期的英文缩写
        Map<String, Integer> monthMap = Map.of(
            "JAN", 1, "FEB", 2, "MAR", 3, "APR", 4,
            "MAY", 5, "JUN", 6, "JUL", 7, "AUG", 8,
            "SEP", 9, "OCT", 10, "NOV", 11, "DEC", 12
        );
        Map<String, Integer> weekMap = Map.of(
            "SUN", 0, "MON", 1, "TUE", 2, "WED", 3,
            "THU", 4, "FRI", 5, "SAT", 6
        );
        
        String upper = value.toUpperCase();
        if (monthMap.containsKey(upper)) {
            return monthMap.get(upper);
        }
        if (weekMap.containsKey(upper)) {
            return weekMap.get(upper);
        }
        
        return Integer.parseInt(value);
    }
    
    public boolean matches(int value) {
        return noSpecificValue || values.contains(value);
    }
    
    public boolean isNoSpecificValue() {
        return noSpecificValue;
    }
    
    public Set<Integer> getValues() {
        return Collections.unmodifiableSet(values);
    }
}
```

---

## 5. 接口设计

### 5.1 CronScheduler 接口

```java
/**
 * Cron 调度器主接口
 */
public interface CronScheduler {
    
    // ==================== 生命周期 ====================
    
    /**
     * 启动调度器
     */
    void start();
    
    /**
     * 优雅停止调度器
     * @param waitForComplete 是否等待正在执行的任务完成
     */
    void shutdown(boolean waitForComplete);
    
    /**
     * 强制停止 (不等待)
     */
    void shutdownNow();
    
    /**
     * 检查是否已启动
     */
    boolean isStarted();
    
    /**
     * 检查是否已停止
     */
    boolean isShutdown();
    
    // ==================== Cron 管理 ====================
    
    /**
     * 注册一个新的 Cron 任务
     * @param definition Cron 定义
     * @return Cron ID
     * @throws CronException 注册失败时抛出
     */
    String registerCron(CronDefinition definition) throws CronException;
    
    /**
     * 批量注册
     */
    List<String> registerCrons(List<CronDefinition> definitions) throws CronException;
    
    /**
     * 更新已有的 Cron 任务
     */
    void updateCron(CronDefinition definition) throws CronException;
    
    /**
     * 删除 Cron 任务
     */
    void unregisterCron(String cronId) throws CronException;
    
    /**
     * 批量删除
     */
    void unregisterCrons(List<String> cronIds) throws CronException;
    
    /**
     * 暂停 Cron 任务
     */
    void pauseCron(String cronId) throws CronException;
    
    /**
     * 恢复 Cron 任务
     */
    void resumeCron(String cronId) throws CronException;
    
    /**
     * 触发一次立即执行 (不影响正常调度)
     */
    void triggerNow(String cronId) throws CronException;
    
    // ==================== 查询 ====================
    
    /**
     * 获取单个 Cron 定义
     */
    Optional<CronDefinition> getCron(String cronId);
    
    /**
     * 获取所有 Cron
     */
    List<CronDefinition> getAllCrons();
    
    /**
     * 按状态查询
     */
    List<CronDefinition> getCronsByStatus(CronStatus status);
    
    /**
     * 按类型查询
     */
    List<CronDefinition> getCronsByType(JobType type);
    
    /**
     * 分页查询
     */
    PageResult<CronDefinition> queryCrons(CronQuery query);
    
    /**
     * 获取执行历史
     */
    List<ExecutionHistory> getExecutionHistory(String cronId, int limit);
    
    /**
     * 获取最近一次执行
     */
    Optional<ExecutionHistory> getLastExecution(String cronId);
    
    /**
     * 获取下次触发时间
     */
    Optional<Long> getNextFireTime(String cronId);
    
    // ==================== 统计 ====================
    
    /**
     * 获取调度器统计信息
     */
    SchedulerStatistics getStatistics();
    
    /**
     * 获取节点信息 (集群模式)
     */
    List<NodeInfo> getClusterNodes();
}
```

### 5.2 JobHandler 接口

```java
/**
 * 任务处理器接口
 * 业务代码实现此接口来定义具体的任务逻辑
 */
public interface JobHandler {
    
    /**
     * 执行任务
     * @param context 执行上下文
     * @return 执行结果
     * @throws JobExecutionException 执行异常
     */
    JobResult execute(JobContext context) throws JobExecutionException;
    
    /**
     * 中断任务 (用于超时或手动取消)
     * @param context 执行上下文
     */
    default void interrupt(JobContext context) {
        // 默认空实现，子类可覆盖
    }
    
    /**
     * 获取处理器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}


/**
 * 任务执行结果
 */
public class JobResult {
    
    private final boolean success;        // 是否成功
    private final String message;         // 结果消息
    private final Object data;            // 结果数据
    private final Map<String, Object> metrics; // 执行指标
    
    // 构造器
    public static JobResult success() { ... }
    public static JobResult success(Object data) { ... }
    public static JobResult success(String message, Object data) { ... }
    public static JobResult failure(String message) { ... }
    public static JobResult failure(String message, Throwable error) { ... }
    
    // 链式操作
    public JobResult withMetric(String key, Object value) { ... }
}


/**
 * 执行异常
 */
public class JobExecutionException extends Exception {
    
    private final ErrorCode errorCode;
    private final boolean retryable;        // 是否可重试
    private final long retryDelayMs;        // 建议重试延迟
    
    public JobExecutionException(String message) { ... }
    public JobExecutionException(String message, Throwable cause) { ... }
    public JobExecutionException(ErrorCode errorCode, String message) { ... }
    
    // 快捷构造
    public static JobExecutionException timeout(long timeoutMs) { ... }
    public static JobExecutionException interrupted() { ... }
    public static JobExecutionException noRetry(String message) { ... }
}
```

### 5.3 Store 接口

```java
/**
 * Cron 存储接口
 */
public interface CronStore {
    
    // ==================== 基础 CRUD ====================
    
    /**
     * 保存 Cron 定义
     */
    void save(CronDefinition definition) throws StoreException;
    
    /**
     * 批量保存
     */
    void batchSave(List<CronDefinition> definitions) throws StoreException;
    
    /**
     * 更新
     */
    void update(CronDefinition definition) throws StoreException;
    
    /**
     * 删除
     */
    void delete(String id) throws StoreException;
    
    /**
     * 批量删除
     */
    void batchDelete(List<String> ids) throws StoreException;
    
    // ==================== 查询 ====================
    
    /**
     * 根据 ID 查询
     */
    Optional<CronDefinition> findById(String id) throws StoreException;
    
    /**
     * 查询全部
     */
    List<CronDefinition> findAll() throws StoreException;
    
    /**
     * 按状态查询
     */
    List<CronDefinition> findByStatus(CronStatus status) throws StoreException;
    
    /**
     * 条件查询
     */
    List<CronDefinition> findByCondition(StoreQuery query) throws StoreException;
    
    /**
     * 分页查询
     */
    PageResult<CronDefinition> findByPage(StoreQuery query, PageParam pageParam) throws StoreException;
    
    /**
     * 判断是否存在
     */
    boolean exists(String id) throws StoreException;
    
    /**
     * 计数
     */
    long count() throws StoreException;
    
    long countByCondition(StoreQuery query) throws StoreException;
    
    // ==================== 事务支持 ====================
    
    /**
     * 在事务中执行
     */
    void transactional(Runnable operation) throws StoreException;
    
    /**
     * 带返回值的事务
     */
    <T> T transactional(Supplier<T> operation) throws StoreException;
    
    // ==================== 监听机制 ====================
    
    /**
     * 注册存储变更监听器
     */
    void addChangeListener(StoreChangeListener listener);
    
    void removeChangeListener(StoreChangeListener listener);
}


/**
 * 存储查询条件
 */
public class StoreQuery {
    
    private String id;
    private String nameLike;              // 名称模糊匹配
    private CronStatus status;
    private JobType jobType;
    private Long createdAtStart;          // 创建时间范围
    private Long createdAtEnd;
    private Map<String, Object> extraConditions; // 扩展条件
    
    // Builder 模式
    public static Builder builder() { ... }
}


/**
 * 存储变更监听器
 */
public interface StoreChangeListener {
    
    /**
     * Cron 被创建
     */
    void onCronCreated(CronDefinition definition);
    
    /**
     * Cron 被更新
     */
    void onCronUpdated(CronDefinition oldDef, CronDefinition newDef);
    
    /**
     * Cron 被删除
     */
    void onCronDeleted(String cronId);
    
    /**
     * 批量变更
     */
    void onBatchChange(ChangeType type, List<String> cronIds);
    
    enum ChangeType {
        CREATED, UPDATED, DELETED, STATUS_CHANGED
    }
}
```

### 5.4 DistributedLock 接口

```java
/**
 * 分布式锁接口
 */
public interface DistributedLock {
    
    // ==================== 基础锁操作 ====================
    
    /**
     * 获取锁 (阻塞直到获取成功)
     * @param lockKey 锁标识
     * @return 锁对象 (用于后续解锁)
     * @throws LockException 获取失败
     */
    LockToken lock(String lockKey) throws LockException;
    
    /**
     * 获取锁 (带超时)
     * @param lockKey 锁标识
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 锁对象，超时返回 null
     */
    LockToken tryLock(String lockKey, long timeout, TimeUnit unit);
    
    /**
     * 立即尝试获取锁
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey);
    
    /**
     * 释放锁
     * @param token 锁对象
     */
    void unlock(LockToken token);
    
    /**
     * 根据 key 释放锁 (需要验证持有者)
     */
    void unlock(String lockKey, String lockValue);
    
    // ==================== 查询 ====================
    
    /**
     * 检查是否已锁定
     */
    boolean isLocked(String lockKey);
    
    /**
     * 获取锁的剩余过期时间
     */
    long getRemainingTime(String lockKey);
    
    /**
     * 获取锁持有者
     */
    Optional<String> getLockHolder(String lockKey);
    
    // ==================== 续期 ====================
    
    /**
     * 续期锁
     * @param token 锁对象
     * @param additionalTime 增加的时间
     * @param unit 时间单位
     * @return 是否续期成功
     */
    boolean renewLock(LockToken token, long additionalTime, TimeUnit unit);
    
    /**
     * 自动续期 (看门狗机制)
     * @param token 锁对象
     * @param interval 续期间隔
     * @param executor 执行器
     * @return 续期任务 Future
     */
    ScheduledFuture<?> autoRenew(LockToken token, long interval, TimeUnit unit, ScheduledExecutorService executor);
    
    // ==================== 批量操作 ====================
    
    /**
     * 获取多个锁 (全部获取成功才算成功)
     */
    MultiLockToken multiLock(List<String> lockKeys, long timeout, TimeUnit unit);
    
    /**
     * 获取多个锁 (获取尽可能多的锁)
     */
    MultiLockToken multiLockBestEffort(List<String> lockKeys, long timeout, TimeUnit unit);
    
    /**
     * 释放多个锁
     */
    void multiUnlock(MultiLockToken token);
    
    // ==================== 红锁 (RedLock) ====================
    
    /**
     * 使用 RedLock 算法获取锁 (多主节点)
     */
    LockToken redLock(String lockKey, long timeout, TimeUnit unit, List<LockInstance> instances);
}


/**
 * 锁令牌
 */
public class LockToken {
    
    private final String lockKey;
    private final String lockValue;
    private final long acquiredTime;
    private final long expireTime;
    
    // 构造器
    
    // Getters...
}


/**
 * 多锁令牌
 */
public class MultiLockToken {
    
    private final List<LockToken> tokens;
    private final int acquiredCount;
    private final int requestedCount;
    
    public boolean isAllAcquired() {
        return acquiredCount == requestedCount;
    }
    
    // Getters...
}


/**
 * 锁实例 (用于 RedLock)
 */
public class LockInstance {
    
    private final String host;
    private final int port;
    private final String password;
    private final int database;
    
    // Builder...
}
```

---

## 6. 事件设计

```java
/**
 * ==================== 事件体系 ====================
 * 
 * 所有事件都继承 CronEvent，包含基础信息：
 * - eventId: 事件唯一 ID
 * - eventTime: 事件发生时间
 * - source: 事件源
 * 
 * 事件类型分为：
 * 1. 定义事件 (Definition) - Cron 增删改
 * 2. 调度事件 (Schedule) - 触发、执行
 * 3. 集群事件 (Cluster) - 节点上下线
 * 4. 系统事件 (System) - 启动停止
 * 
 * =================================================
 */

/**
 * 基础事件
 */
public abstract class CronEvent extends ApplicationEvent {
    
    private final String eventId;
    private final long eventTime;
    private final String nodeId;
    
    public CronEvent(Object source) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.eventTime = System.currentTimeMillis();
        this.nodeId = NodeContext.getCurrentNodeId();
    }
    
    // Getters...
}


// ==================== 定义事件 ====================

/**
 * Cron 被创建
 */
public class CronCreatedEvent extends CronEvent {
    
    private final CronDefinition definition;
    
    public CronCreatedEvent(Object source, CronDefinition definition) {
        super(source);
        this.definition = definition;
    }
    
    // Getter...
}

/**
 * Cron 被更新
 */
public class CronUpdatedEvent extends CronEvent {
    
    private final CronDefinition oldDefinition;
    private final CronDefinition newDefinition;
    private final List<String> changedFields;
    
    public CronUpdatedEvent(Object source, CronDefinition oldDef, CronDefinition newDef) {
        super(source);
        this.oldDefinition = oldDef;
        this.newDefinition = newDef;
        this.changedFields = calculateChanges(oldDef, newDef);
    }
    
    // Getters...
}

/**
 * Cron 被删除
 */
public class CronDeletedEvent extends CronEvent {
    
    private final String cronId;
    private final CronDefinition definition; // 删除前的快照
    
    // Constructor...
    // Getters...
}

/**
 * Cron 状态变更
 */
public class CronStatusChangedEvent extends CronEvent {
    
    private final String cronId;
    private final CronStatus oldStatus;
    private final CronStatus newStatus;
    
    // Constructor...
    // Getters...
}


// ==================== 调度事件 ====================

/**
 * Cron 触发 (准备执行)
 */
public class CronTriggeredEvent extends CronEvent {
    
    private final String cronId;
    private final String executionId;
    private final long triggerTime;
    private final String acquireNode; // 获取到锁的节点
    
    // Constructor...
    // Getters...
}

/**
 * Cron 开始执行
 */
public class CronExecutingEvent extends CronEvent {
    
    private final String cronId;
    private final String executionId;
    private final long fireTime;
    private final String nodeId;
    private final String threadName;
    
    // Constructor...
    // Getters...
}

/**
 * Cron 执行完成
 */
public class CronExecutedEvent extends CronEvent {
    
    private final String cronId;
    private final String executionId;
    private final ExecutionStatus status; // SUCCESS / FAILURE / TIMEOUT
    private final long durationMs;
    private final JobResult result;
    private final Throwable error;
    
    // Constructor...
    // Getters...
}

/**
 * Cron 错过触发
 */
public class CronMisfiredEvent extends CronEvent {
    
    private final String cronId;
    private final long scheduledTime; // 计划触发时间
    private final long actualTime;    // 实际发现错过的时间
    private final MisfirePolicy policy;
    
    // Constructor...
    // Getters...
}


// ==================== 集群事件 ====================

/**
 * 节点上线
 */
public class NodeJoinedEvent extends CronEvent {
    
    private final String nodeId;
    private final String nodeIp;
    private final long joinTime;
    
    // Constructor...
    // Getters...
}

/**
 * 节点下线
 */
public class NodeLeftEvent extends CronEvent {
    
    private final String nodeId;
    private final long leaveTime;
    private final boolean graceful; // 是否优雅退出
    private final String reason;
    
    // Constructor...
    // Getters...
}

/**
 * 节点疑似故障
 */
public class NodeSuspectedEvent extends CronEvent {
    
    private final String nodeId;
    private final long lastHeartbeat;
    private final long suspectTime;
    private final long missedHeartbeats;
    
    // Constructor...
    // Getters...
}


// ==================== 系统事件 ====================

/**
 * 调度器启动完成
 */
public class SchedulerStartedEvent extends CronEvent {
    
    private final String nodeId;
    private final long startTime;
    private final int loadedCronCount;
    private final List<String> loadedCronIds;
    
    // Constructor...
    // Getters...
}

/**
 * 调度器停止
 */
public class SchedulerStoppedEvent extends CronEvent {
    
    private final String nodeId;
    private final long stopTime;
    private final long runningDuration;
    private final boolean graceful;
    
    // Constructor...
    // Getters...
}


// ==================== 事件监听器接口 ====================

/**
 * 通用事件监听器
 */
public interface CronEventListener<E extends CronEvent> {
    
    /**
     * 支持的事件类型
     */
    Class<E> getEventType();
    
    /**
     * 处理事件
     */
    void onEvent(E event);
    
    /**
     * 执行顺序 (越小越先执行)
     */
    default int getOrder() {
        return 0;
    }
    
    /**
     * 是否异步处理
     */
    default boolean isAsync() {
        return false;
    }
}


/**
 * 注解式监听器
 */
@Component
public class CronEventHandlers {
    
    @CronEventListener
    public void onCronCreated(CronCreatedEvent event) {
        // 发送通知
        // 记录审计日志
    }
    
    @CronEventListener(async = true)
    public void onCronExecuted(CronExecutedEvent event) {
        // 异步处理执行结果
        // 更新监控指标
    }
    
    @CronEventListener(order = 100)
    public void onNodeSuspected(NodeSuspectedEvent event) {
        // 发送告警
    }
}
```

---

## 6. 配置设计

```yaml
# ==================== OpenClaw Cron 配置 ====================

openclaw:
  cron:
    # 是否启用
    enabled: true
    
    # ==================== 调度器配置 ====================
    scheduler:
      # 线程池大小 (用于触发检查)
      trigger-threads: 5
      
      # 检查间隔 (毫秒)
      check-interval: 1000
      
      # 是否允许并发执行同一个 Cron
      allow-concurrent: false
      
      # 错过触发阈值 (毫秒)
      misfire-threshold: 60000
    
    # ==================== 执行器配置 ====================
    executor:
      # 核心线程数
      core-pool-size: 10
      
      # 最大线程数
      max-pool-size: 50
      
      # 队列容量
      queue-capacity: 1000
      
      # 线程存活时间 (秒)
      keep-alive-seconds: 60
      
      # 拒绝策略: abort, discard, caller_runs
      rejection-policy: abort
      
      # 线程名前缀
      thread-name-prefix: cron-executor-
    
    # ==================== 存储配置 ====================
    store:
      # 存储类型: jdbc, redis, file
      type: jdbc
      
      # 是否启用缓存
      cache-enabled: true
      
      # 缓存过期时间 (秒)
      cache-ttl: 300
      
      # JDBC 配置
      jdbc:
        # 是否自动建表
        auto-create-table: true
        
        # 表名前缀
        table-prefix: cron_
        
        # 批量操作大小
        batch-size: 100
        
        # 事务超时 (秒)
        transaction-timeout: 30
      
      # Redis 配置
      redis:
        # Hash key 前缀
        key-prefix: openclaw:cron
        
        # 默认过期时间 (秒), 0=永不过期
        default-ttl: 0
      
      # File 配置
      file:
        # 存储目录
        path: ./cron-data
        
        # 文件格式: json, yaml
        format: json
        
        # 是否启用文件监听 (热重载)
        watch-enabled: false
    
    # ==================== 分布式锁配置 ====================
    distributed-lock:
      # 锁实现: redis, zookeeper, db
      type: redis
      
      # 默认锁过期时间 (毫秒)
      default-ttl: 30000
      
      # 看门狗续期间隔 (毫秒)
      watch-dog-interval: 10000
      
      # 获取锁最大重试次数
      max-retry: 3
      
      # 重试间隔 (毫秒)
      retry-interval: 100
      
      # Redis 锁配置
      redis:
        # 锁 key 前缀
        key-prefix: openclaw:lock
        
        # 是否使用红锁 (多主节点)
        red-lock-enabled: false
        
        # 红锁节点 (当 red-lock-enabled=true 时)
        red-lock-instances:
          - host: redis1
            port: 6379
          - host: redis2
            port: 6379
          - host: redis3
            port: 6379
      
      # ZooKeeper 锁配置
      zookeeper:
        # ZK 连接串
        connect-string: localhost:2181
        
        # 锁节点路径前缀
        lock-path: /openclaw/locks
        
        # 会话超时 (毫秒)
        session-timeout: 60000
        
        # 连接超时 (毫秒)
        connection-timeout: 15000
    
    # ==================== 集群配置 ====================
    cluster:
      # 节点标识 (默认自动生成)
      node-id: ${random.uuid}
      
      # 节点 IP (默认自动获取)
      node-ip: ${spring.cloud.client.ip-address}
      
      # 心跳间隔 (秒)
      heartbeat-interval: 30
      
      # 心跳超时 (秒)
      heartbeat-timeout: 90
      
      # 故障检测间隔 (秒)
      failure-detection-interval: 60
      
      # 是否启用领导者选举
      leader-election-enabled: false
      
      # 事件传播方式: spring_event, redis_pubsub, mq
      event-propagate-type: redis_pubsub
      
      # Redis Pub/Sub 配置 (当 event-propagate-type=redis_pubsub)
      redis-pubsub:
        channel: openclaw:cron:events
        
    # ==================== 监控与告警 ====================
    monitor:
      # 是否启用指标收集
      metrics-enabled: true
      
      # 指标导出方式: micrometer, custom
      metrics-exporter: micrometer
      
      # 是否启用健康检查
      health-check-enabled: true
      
      # 健康检查端点
      health-check-path: /actuator/cron-health
      
      # 告警配置
      alert:
        # 是否启用
        enabled: false
        
        # 告警渠道: webhook, email, sms
        channels:
          - type: webhook
            url: https://example.com/alert
            
        # 告警规则
        rules:
          # 执行失败告警
          - name: execution-failure
            condition: "status == 'FAILURE'"
            cooldown: 300  # 冷却时间 (秒)
            
          # 执行超时告警
          - name: execution-timeout
            condition: "status == 'TIMEOUT'"
            cooldown: 300
            
          # 错过触发告警
          - name: misfire
            condition: "misfire == true"
            cooldown: 60
```

---

## 7. 部署架构建议

### 7.1 单节点部署

```
┌─────────────────────────────────────────────────────┐
│                    单节点部署                          │
│                    (开发/测试)                         │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │           Application Server              │   │
│  │                                             │   │
│  │  ┌─────────────┐  ┌─────────────┐          │   │
│  │  │   Cron      │  │   Store     │          │   │
│  │  │  Scheduler  │──│  (File/H2)  │          │   │
│  │  └─────────────┘  └─────────────┘          │   │
│  │         │                                  │   │
│  │         │ (本地锁)                          │   │
│  │         ▼                                  │   │
│  │  ┌─────────────┐                          │   │
│  │  │   Job       │                          │   │
│  │  │  Executor   │                          │   │
│  │  └─────────────┘                          │   │
│  │                                             │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  存储: 文件 或 H2 内存数据库                          │
│  锁: 本地 ReentrantLock                            │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 7.2 集群部署

```
┌─────────────────────────────────────────────────────────────────────┐
│                        集群部署 (生产环境)                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│   │   Node 1    │    │   Node 2    │    │   Node 3    │           │
│   │  (Master)   │    │  (Slave)    │    │  (Slave)    │           │
│   │             │    │             │    │             │           │
│   │ ┌─────────┐ │    │ ┌─────────┐ │    │ ┌─────────┐ │           │
│   │ │  Cron   │ │    │ │  Cron   │ │    │ │  Cron   │ │           │
│   │ │Scheduler│ │    │ │Scheduler│ │    │ │Scheduler│ │           │
│   │ └────┬────┘ │    │ └────┬────┘ │    │ └────┬────┘ │           │
│   │      │      │    │      │      │    │      │      │           │
│   │      ▼      │    │      ▼      │    │      ▼      │           │
│   │ ┌─────────┐ │    │ ┌─────────┐ │    │ ┌─────────┐ │           │
│   │ │Distributed│◀───────▶│Distributed│◀───────▶│Distributed│ │           │
│   │ │   Lock    │ │    │ │   Lock    │ │    │ │   Lock    │ │           │
│   │ └─────────┘ │    │ └─────────┘ │    │ └─────────┘ │           │
│   │      │      │    │      │      │    │      │      │           │
│   │      ▼      │    │      ▼      │    │      ▼      │           │
│   │ ┌─────────┐ │    │ ┌─────────┐ │    │ ┌─────────┐ │           │
│   │ │   Job   │ │    │ │   Job   │ │    │ │   Job   │ │           │
│   │ │Executor │ │    │ │Executor │ │    │ │Executor │ │           │
│   │ └─────────┘ │    │ └─────────┘ │    │ └─────────┘ │           │
│   │             │    │             │    │             │           │
│   │  持有锁时    │    │  竞争锁失败  │    │  竞争锁失败  │           │
│   │  执行任务    │    │  等待下次    │    │  等待下次    │           │
│   └─────────────┘    └─────────────┘    └─────────────┘           │
│                                                                     │
│  共享基础设施:                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                      Redis Cluster                           │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │   │
│  │  │   Master 1  │  │   Master 2  │  │   Master 3  │          │   │
│  │  │  (Lock/Store)│  │  (Lock/Store)│  │  (Lock/Store)│          │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   Database (Primary-Standby)                   │   │
│  │                    cron_definition                             │   │
│  │                    cron_execution_history                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

集群部署要点:

1. 节点无状态设计
   - 所有状态存储在 Redis/DB
   - 节点可随时上下线
   - 负载均衡器自动分发请求

2. 锁竞争策略
   - 所有节点竞争同一把锁
   - 只有获取锁的节点执行任务
   - 锁自动续期防止任务中断

3. 事件传播
   - 使用 Redis Pub/Sub 广播事件
   - 所有节点同步状态变更
   - 最终一致性保证

4. 故障恢复
   - 节点宕机时锁自动过期
   - 其他节点接管任务
   - 历史执行记录不丢失
```

### 7.3 容器化部署

```yaml
# docker-compose.yml - 开发环境
version: '3.8'

services:
  # Cron 调度服务
  cron-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - OPENCLAW_CRON_STORE_TYPE=redis
      - OPENCLAW_CRON_DISTRIBUTED_LOCK_TYPE=redis
      - SPRING_REDIS_HOST=redis
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/cron
    depends_on:
      - redis
      - mysql
    deploy:
      replicas: 3  # 3 个实例组成集群

  # Redis (锁 + 存储 + 事件)
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"

  # MySQL (持久化存储)
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=cron
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"

volumes:
  redis-data:
  mysql-data:
```

---

## 8. 总结

本架构文档完整描述了一个生产级分布式 Cron 调度系统的设计：

### 核心设计决策

| 决策点 | 选择 | 理由 |
|-------|------|------|
| 调度引擎 | 自研基于时间轮 | 精确控制，无 Quartz 依赖 |
| 分布式锁 | Redis (默认) | 性能与可靠性平衡 |
| 事件传播 | Redis Pub/Sub | 轻量，无需额外组件 |
| 存储 | 插件化 (JDBC/Redis/File) | 适应不同部署场景 |
| 心跳检测 | 可选 | 简单场景可关闭减少复杂度 |

### 模块职责边界

```
┌─────────────────────────────────────────────────────────┐
│  Core (cron-core)                                       │
│  - 调度算法                                             │
│  - 触发计算                                             │
│  - 内存状态管理                                         │
├─────────────────────────────────────────────────────────┤
│  API (cron-api)                                         │
│  - REST/gRPC 接口                                       │
│  - 参数校验                                             │
│  - 权限控制                                             │
├─────────────────────────────────────────────────────────┤
│  Store (cron-store-jdbc/redis/file)                     │
│  - 持久化实现                                           │
│  - 查询优化                                             │
│  - 缓存策略                                             │
├─────────────────────────────────────────────────────────┤
│  Lock (cron-lock-redis/zk/db)                         │
│  - 分布式锁实现                                         │
│  - 锁续期                                               │
│  - 故障恢复                                             │
├─────────────────────────────────────────────────────────┤
│  Cluster (cron-cluster)                                 │
│  - 节点发现                                             │
│  - 心跳管理                                             │
│  - 事件广播                                             │
├─────────────────────────────────────────────────────────┤
│  Monitor (cron-monitor)                                 │
│  - 指标收集                                             │
│  - 健康检查                                             │
│  - 告警触发                                             │
└─────────────────────────────────────────────────────────┘
```

### 演进路线

**Phase 1: MVP (4 周)**
- 基础 Cron 解析与调度
- JDBC 存储
- 本地锁 (单节点)
- REST API

**Phase 2: 分布式 (2 周)**
- Redis 分布式锁
- Redis 事件传播
- 集群心跳
- 多节点部署

**Phase 3: 生产优化 (2 周)**
- 执行历史分表
- 监控告警
- 动态配置
- 限流熔断

**Phase 4: 高级特性 (按需)**
- 任务依赖 DAG
- 分片执行
- 灰度发布
- 多租户隔离

---

**文档版本**: 1.0  
**创建日期**: 2024-01-15  
**作者**: AI Assistant

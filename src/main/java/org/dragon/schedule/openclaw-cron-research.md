# OpenClaw Cron 模块研究报告

**研究日期**: 2026-03-15  
**项目**: https://github.com/openclaw/openclaw  
**模块路径**: `src/cron/`  

---

## 1. 项目背景

OpenClaw 是一个个人 AI 助手平台，支持多通道通信（WhatsApp、Telegram、Slack、Discord、Google Chat、Signal、iMessage、Feishu 等）。其 **cron 模块** 是一个功能完整的 TypeScript 定时任务调度系统，专为 AI 任务场景设计。

---

## 2. 核心架构

### 2.1 服务层 (`service.ts`)

```typescript
export class CronService {
  constructor(deps: CronServiceDeps)  // 依赖注入模式
  async start() / stop()               // 生命周期管理
  async add/update/remove()            // CRUD 操作
  async run(id, mode?)                 // 手动触发
  wake(opts)                           // 唤醒定时器
}
```

### 2.2 调度核心 (`schedule.ts`)

- **使用 Croner 库** 解析 cron 表达式
- **三种调度类型**：
  - `at`: 绝对时间点执行
  - `every`: 周期性执行（毫秒间隔）
  - `cron`: 标准 cron 表达式
- **智能缓存**：cron 解析结果缓存（最大 512 条）
- **时区支持**：完整时区配置

### 2.3 任务执行

- **Session 目标**：
  - `main`: 主会话执行
  - `isolated`: 独立会话（默认）
  - `current`: 绑定当前会话
- **交付模式**：`announce`（消息通知） / `webhook` / `none`

---

## 3. 数据模型

### 3.1 CronJob 结构

```typescript
type CronJob = {
  id: string;
  name: string;
  enabled: boolean;
  agentId?: string;
  schedule: CronSchedule;     // at | every | cron
  payload: CronPayload;       // agentTurn | systemEvent
  delivery?: CronDelivery;
  sessionTarget: "main" | "isolated" | "current";
  wakeMode: "now" | "next-heartbeat";
  state: CronJobState;
}
```

### 3.2 运行时状态

```typescript
type CronJobState = {
  nextRunAtMs?: number;       // 下次执行时间
  runningAtMs?: number;       // 当前运行标记
  lastRunAtMs?: number;       // 上次执行时间
  lastRunStatus?: "ok" | "error" | "skipped";
  consecutiveErrors?: number;  // 连续错误计数（退避）
}
```

---

## 4. 关键特性

### 4.1 定时器机制
- **精确唤醒**：基于 `setTimeout` 的精确唤醒
- **错过任务处理**：启动时自动处理错过的任务
- **手动唤醒**：支持 `wakeNow` 立即检查

### 4.2 持久化
- **任务存储**：JSON 文件（`cron.json`）
- **运行日志**：JSONL 格式（`runs/{jobId}.jsonl`）
- **自动清理**：按大小（默认 2MB）和行数（默认 2000 行）限制

### 4.3 错误处理
- **连续错误退避**：自动增加重试间隔
- **失败通知**：支持通知渠道或 webhook
- **超时控制**：任务执行超时保护

---

## 5. 近期修复和新增功能

| Issue | 描述 | 状态 |
|-------|------|------|
| #10849, #13509 | 定时器不触发问题 | 已修复 |
| #10045, #33126 | `nextRunAtMs` 计算错误（闰年/时区） | 已修复 |
| #42997 | 手动触发不执行 | 已修复 |
| #43185 | isolated session 启动失败 | 已修复 |
| #45465 | **生命周期钩子**（`beforeRun`, `afterComplete`, `onFailure`, `afterRun`） | **新增** |

---

## 6. 与同类项目对比

| 特性 | OpenClaw Cron | node-cron | node-schedule |
|------|--------------|-----------|---------------|
| 语言 | TypeScript | JavaScript | JavaScript |
| AI 集成 | **原生支持** | 无 | 无 |
| Session 管理 | **支持** | 无 | 无 |
| 失败重试 | **支持** | 无 | 无 |
| 运行日志 | **JSONL 持久化** | 无 | 无 |
| 时区支持 | 完整 | 部分 | 部分 |
| 生命周期钩子 | **支持** | 无 | 无 |

---

## 7. 总结

OpenClaw 的 cron 模块是一个**为 AI 助手场景深度定制**的调度系统，相比通用 cron 库具有以下显著优势：

1. **AI 原生**：与 Agent 系统深度集成，支持 Agent Turn 和 System Event 两种任务类型
2. **Session 管理**：支持主会话、独立会话、当前会话三种执行目标
3. **可观测性**：完整的运行日志、事件系统、失败通知
4. **容错性**：连续错误退避、超时控制、错过任务处理
5. **灵活性**：多种调度类型、交付模式、生命周期钩子

该模块的设计充分考虑了 AI 任务的特殊需求（如会话隔离、模型调用、结果交付），是一个生产级的定时任务调度解决方案。

---

**参考文件**:
- `src/cron/service.ts` - 主服务层
- `src/cron/schedule.ts` - 调度核心
- `src/cron/delivery.ts` - 交付逻辑
- `src/cron/types.ts` - 类型定义
- `src/cron/normalize.ts` - 数据规范化
- `src/cron/run-log.ts` - 运行日志管理

package org.dragon.api.controller;
import lombok.RequiredArgsConstructor;
import org.dragon.schedule.core.CronService;
import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.CronStatus;
import org.dragon.schedule.entity.ExecutionHistory;
import org.dragon.schedule.entity.ExecutionStatus;
import org.dragon.schedule.store.ExecutionHistoryStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * ScheduleController 定时任务管理 API
 *
 * <p>职责范围：
 * <ul>
 *   <li>Cron 任务定义的 CRUD（创建/更新/删除）</li>
 *   <li>任务生命周期控制（暂停/恢复/立即触发）</li>
 *   <li>执行历史查询（按任务/按状态/运行中）</li>
 *   <li>Cron 表达式合法性校验（调试工具）</li>
 * </ul>
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>定期触发 Observer 的周期性评价任务</li>
 *   <li>定时向特定 Workspace 推送任务</li>
 *   <li>定时清理过期数据、刷新缓存等运维任务</li>
 * </ul>
 *
 * @author zhz
 * @version 1.0
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final CronService cronService;
    private final ExecutionHistoryStore executionHistoryStore;

    // ==================== Cron 定义管理 ====================

    /**
     * 创建 Cron 任务
     * POST /api/schedules/crons
     *
     * <p>Body 示例（每天 9 点触发 Observer 周期性评价）：
     * <pre>
     * {
     *   "name": "每日 Observer 评价",
     *   "description": "每天早上 9 点对所有活跃 Workspace 进行周期性评价",
     *   "cronType": "CRON",
     *   "cronExpression": "0 0 9 * * ?",
     *   "timezone": "Asia/Shanghai",
     *   "jobType": "SPRING_BEAN",
     *   "jobHandler": "observerPeriodicEvaluationJob",
     *   "jobData": { "observerId": "obs-global", "periodHours": 24 },
     *   "misfirePolicy": "FIRE_ONCE",
     *   "retryCount": 2,
     *   "retryIntervalMs": 60000
     * }
     * </pre>
     */
    @PostMapping("/crons")
    public ResponseEntity<String> createCron(@RequestBody CronDefinition definition) {
        String cronId = cronService.createCron(definition);
        return ResponseEntity.status(HttpStatus.CREATED).body(cronId);
    }

    /**
     * 查询所有 Cron 任务
     * GET /api/schedules/crons
     * GET /api/schedules/crons?status=ENABLED
     */
    @GetMapping("/crons")
    public ResponseEntity<List<CronDefinition>> listCrons(
            @RequestParam(required = false) CronStatus status) {
        List<CronDefinition> list = (status != null)
                ? cronService.listCronsByStatus(status)
                : cronService.listCrons();
        return ResponseEntity.ok(list);
    }

    /**
     * 查询指定 Cron 任务
     * GET /api/schedules/crons/{cronId}
     */
    @GetMapping("/crons/{cronId}")
    public ResponseEntity<CronDefinition> getCron(@PathVariable String cronId) {
        return cronService.getCron(cronId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新 Cron 任务（如修改表达式或 jobData）
     * PUT /api/schedules/crons/{cronId}
     */
    @PutMapping("/crons/{cronId}")
    public ResponseEntity<Void> updateCron(
            @PathVariable String cronId,
            @RequestBody CronDefinition definition) {
        definition.setId(cronId);
        cronService.updateCron(definition);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除 Cron 任务（同时从调度器取消注册）
     * DELETE /api/schedules/crons/{cronId}
     */
    @DeleteMapping("/crons/{cronId}")
    public ResponseEntity<Void> deleteCron(@PathVariable String cronId) {
        cronService.deleteCron(cronId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 任务生命周期控制 ====================

    /**
     * 暂停 Cron 任务（ENABLED → PAUSED）
     * POST /api/schedules/crons/{cronId}/pause
     */
    @PostMapping("/crons/{cronId}/pause")
    public ResponseEntity<Void> pauseCron(@PathVariable String cronId) {
        cronService.pauseCron(cronId);
        return ResponseEntity.ok().build();
    }

    /**
     * 恢复 Cron 任务（PAUSED → ENABLED）
     * POST /api/schedules/crons/{cronId}/resume
     */
    @PostMapping("/crons/{cronId}/resume")
    public ResponseEntity<Void> resumeCron(@PathVariable String cronId) {
        cronService.resumeCron(cronId);
        return ResponseEntity.ok().build();
    }

    /**
     * 立即触发一次执行（不影响原有调度周期）
     * POST /api/schedules/crons/{cronId}/trigger
     *
     * <p>适用场景：调试时手动触发、补偿错过的执行。
     */
    @PostMapping("/crons/{cronId}/trigger")
    public ResponseEntity<Void> triggerNow(@PathVariable String cronId) {
        cronService.triggerNow(cronId);
        return ResponseEntity.ok().build();
    }

    // ==================== 执行历史查询 ====================

    /**
     * 查询指定 Cron 任务的执行历史
     * GET /api/schedules/crons/{cronId}/history?limit=20
     */
    @GetMapping("/crons/{cronId}/history")
    public ResponseEntity<List<ExecutionHistory>> getExecutionHistory(
            @PathVariable String cronId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ExecutionHistory> history = executionHistoryStore.findByCronId(cronId, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * 查询当前正在运行的所有任务
     * GET /api/schedules/executions/running
     *
     * <p>用于监控大盘，快速发现长时间占用的任务。
     */
    @GetMapping("/executions/running")
    public ResponseEntity<List<ExecutionHistory>> getRunningJobs() {
        return ResponseEntity.ok(executionHistoryStore.findRunningJobs());
    }

    /**
     * 按执行状态查询历史记录
     * GET /api/schedules/executions?status=FAILED&limit=50
     */
    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionHistory>> getExecutionsByStatus(
            @RequestParam ExecutionStatus status,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(executionHistoryStore.findByStatus(status, limit));
    }

    /**
     * 查询指定执行记录详情（含错误堆栈、结果数据）
     * GET /api/schedules/executions/{executionId}
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ExecutionHistory> getExecution(@PathVariable String executionId) {
        Optional<ExecutionHistory> history = executionHistoryStore.findByExecutionId(executionId);
        return history.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * 清理指定时间点之前的历史记录（运维接口）
     * DELETE /api/schedules/executions?beforeTime=1700000000000
     *
     * @param beforeTime 时间戳（毫秒）
     */
    @DeleteMapping("/executions")
    public ResponseEntity<Integer> cleanupHistory(@RequestParam long beforeTime) {
        int deleted = executionHistoryStore.deleteBefore(beforeTime);
        return ResponseEntity.ok(deleted);
    }

}

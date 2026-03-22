package org.dragon.api.controller;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "创建Cron任务")
    @PostMapping("/crons")
    public ResponseEntity<String> createCron(@RequestBody CronDefinition definition) {
        String cronId = cronService.createCron(definition);
        return ResponseEntity.status(HttpStatus.CREATED).body(cronId);
    }

    @Operation(summary = "查询所有Cron任务")
    @GetMapping("/crons")
    public ResponseEntity<List<CronDefinition>> listCrons(
            @RequestParam(required = false) CronStatus status) {
        List<CronDefinition> list = (status != null)
                ? cronService.listCronsByStatus(status)
                : cronService.listCrons();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "查询指定Cron任务")
    @GetMapping("/crons/{cronId}")
    public ResponseEntity<CronDefinition> getCron(@PathVariable String cronId) {
        return cronService.getCron(cronId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "更新Cron任务（如修改表达式或 jobData）")
    @PutMapping("/crons/{cronId}")
    public ResponseEntity<Void> updateCron(
            @PathVariable String cronId,
            @RequestBody CronDefinition definition) {
        definition.setId(cronId);
        cronService.updateCron(definition);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "删除Cron任务（同时从调度器取消注册）")
    @DeleteMapping("/crons/{cronId}")
    public ResponseEntity<Void> deleteCron(@PathVariable String cronId) {
        cronService.deleteCron(cronId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 任务生命周期控制 ====================

    @Operation(summary = "暂停Cron任务（ENABLED → PAUSED）")
    @PostMapping("/crons/{cronId}/pause")
    public ResponseEntity<Void> pauseCron(@PathVariable String cronId) {
        cronService.pauseCron(cronId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "恢复Cron任务（PAUSED → ENABLED）")
    @PostMapping("/crons/{cronId}/resume")
    public ResponseEntity<Void> resumeCron(@PathVariable String cronId) {
        cronService.resumeCron(cronId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "立即触发一次执行（不影响原有调度周期）")
    @PostMapping("/crons/{cronId}/trigger")
    public ResponseEntity<Void> triggerNow(@PathVariable String cronId) {
        cronService.triggerNow(cronId);
        return ResponseEntity.ok().build();
    }

    // ==================== 执行历史查询 ====================

    @Operation(summary = "查询指定 Cron 任务的执行历史")
    @GetMapping("/crons/{cronId}/history")
    public ResponseEntity<List<ExecutionHistory>> getExecutionHistory(
            @PathVariable String cronId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ExecutionHistory> history = executionHistoryStore.findByCronId(cronId, limit);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "查询当前正在运行的所有任务")
    @GetMapping("/executions/running")
    public ResponseEntity<List<ExecutionHistory>> getRunningJobs() {
        return ResponseEntity.ok(executionHistoryStore.findRunningJobs());
    }

    @Operation(summary = "按执行状态查询历史记录")
    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionHistory>> getExecutionsByStatus(
            @RequestParam ExecutionStatus status,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(executionHistoryStore.findByStatus(status, limit));
    }

    @Operation(summary = "查询指定执行记录详情（含错误堆栈、结果数据）")
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ExecutionHistory> getExecution(@PathVariable String executionId) {
        Optional<ExecutionHistory> history = executionHistoryStore.findByExecutionId(executionId);
        return history.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "清理指定时间点之前的历史记录（运维接口）")
    @DeleteMapping("/executions")
    public ResponseEntity<Integer> cleanupHistory(@RequestParam long beforeTime) {
        int deleted = executionHistoryStore.deleteBefore(beforeTime);
        return ResponseEntity.ok(deleted);
    }

}

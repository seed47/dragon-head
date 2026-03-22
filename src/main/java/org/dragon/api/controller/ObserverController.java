package org.dragon.api.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.observer.Observer;
import org.dragon.observer.ObserverRegistry;
import org.dragon.observer.ObserverService;
import org.dragon.observer.commons.CommonSense;
import org.dragon.observer.commons.CommonSenseStore;
import org.dragon.observer.commons.CommonSenseValidator;
import org.dragon.observer.evaluation.EvaluationEngine;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.optimization.OptimizationAction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ObserverController 观察者管理 API
 *
 * <p>职责范围：
 * <ul>
 *   <li>Observer 生命周期（注册/注销/激活/暂停）</li>
 *   <li>评价记录查询（按目标/按阈值）</li>
 *   <li>手动触发评价</li>
 *   <li>优化动作管理（查询/手动执行/回滚）</li>
 *   <li>常识库（CommonSense）的 CRUD 管理</li>
 *   <li>统计仪表盘</li>
 * </ul>
 *
 * @author zhz
 * @version 1.0
 */
@RestController
@RequestMapping("/api/observers")
@RequiredArgsConstructor
public class ObserverController {

    private final ObserverRegistry observerRegistry;
    private final ObserverService observerService;
    private final CommonSenseStore commonSenseStore;

    // ==================== Observer 生命周期 ====================

    /**
     * 注册新 Observer
     * POST /api/observers
     *
     * <p>Body 示例：
     * <pre>
     * {
     *   "id": "obs-workspace-001",
     *   "name": "研发 Workspace 观察者",
     *   "workspaceId": "ws-dev-team",
     *   "evaluationMode": "RULE_BASED",
     *   "optimizationThreshold": 0.6,
     *   "autoOptimizationEnabled": true
     * }
     * </pre>
     */
    @PostMapping
    public ResponseEntity<Observer> registerObserver(@RequestBody Observer observer) {
        observerRegistry.register(observer);
        return ResponseEntity.status(HttpStatus.CREATED).body(observer);
    }

    /**
     * 查询所有 Observer
     * GET /api/observers
     * GET /api/observers?activeOnly=true
     */
    @GetMapping
    public ResponseEntity<List<Observer>> listObservers(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<Observer> list = activeOnly ? observerRegistry.listActive() : observerRegistry.listAll();
        return ResponseEntity.ok(list);
    }

    /**
     * 查询指定 Observer
     * GET /api/observers/{observerId}
     */
    @GetMapping("/{observerId}")
    public ResponseEntity<Observer> getObserver(@PathVariable String observerId) {
        return observerRegistry.get(observerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据绑定的 Workspace 查询对应 Observer
     * GET /api/observers/by-workspace/{workspaceId}
     */
    @GetMapping("/by-workspace/{workspaceId}")
    public ResponseEntity<Observer> getObserverByWorkspace(@PathVariable String workspaceId) {
        return observerRegistry.getByWorkspace(workspaceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新 Observer 配置（如修改评价阈值、开关自动优化）
     * PUT /api/observers/{observerId}
     */
    @PutMapping("/{observerId}")
    public ResponseEntity<Observer> updateObserver(
            @PathVariable String observerId,
            @RequestBody Observer observer) {
        observer.setId(observerId);
        observerRegistry.update(observer);
        return ResponseEntity.ok(observer);
    }

    /**
     * 注销 Observer
     * DELETE /api/observers/{observerId}
     */
    @DeleteMapping("/{observerId}")
    public ResponseEntity<Void> unregisterObserver(@PathVariable String observerId) {
        observerRegistry.unregister(observerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 激活 Observer（INACTIVE/PAUSED → ACTIVE）
     * POST /api/observers/{observerId}/activate
     */
    @PostMapping("/{observerId}/activate")
    public ResponseEntity<Void> activateObserver(@PathVariable String observerId) {
        observerRegistry.activate(observerId);
        return ResponseEntity.ok().build();
    }

    /**
     * 暂停 Observer（ACTIVE → PAUSED）
     * POST /api/observers/{observerId}/pause
     */
    @PostMapping("/{observerId}/pause")
    public ResponseEntity<Void> pauseObserver(@PathVariable String observerId) {
        observerRegistry.pause(observerId);
        return ResponseEntity.ok().build();
    }

    /**
     * 设置默认 Observer
     * POST /api/observers/{observerId}/set-default
     */
    @PostMapping("/{observerId}/set-default")
    public ResponseEntity<Void> setDefaultObserver(@PathVariable String observerId) {
        observerRegistry.setDefaultObserver(observerId);
        return ResponseEntity.ok().build();
    }

    // ==================== 统计仪表盘 ====================

    /**
     * 查询 Observer 运行统计（评价数/优化数/常识数等）
     * GET /api/observers/{observerId}/stats
     */
    @GetMapping("/{observerId}/stats")
    public ResponseEntity<ObserverService.ObserverStats> getStats(@PathVariable String observerId) {
        ObserverService.ObserverStats stats = observerService.getStats(observerId);
        return ResponseEntity.ok(stats);
    }

    // ==================== 评价记录 ====================

    /**
     * 查询某个目标（Character/Workspace）的所有评价记录
     * GET /api/observers/{observerId}/evaluations?targetType=CHARACTER&targetId=c-001
     */
    @GetMapping("/{observerId}/evaluations")
    public ResponseEntity<List<EvaluationRecord>> getEvaluations(
            @PathVariable String observerId,
            @RequestParam EvaluationRecord.TargetType targetType,
            @RequestParam String targetId) {
        List<EvaluationRecord> records = observerService.getEvaluationsByTarget(targetType, targetId);
        return ResponseEntity.ok(records);
    }

    /**
     * 查询综合评分低于阈值的评价记录（需要关注的问题项）
     * GET /api/observers/{observerId}/evaluations/below-threshold
     */
    @GetMapping("/{observerId}/evaluations/below-threshold")
    public ResponseEntity<List<EvaluationRecord>> getBelowThresholdEvaluations(
            @PathVariable String observerId) {
        List<EvaluationRecord> records = observerService.getBelowThresholdEvaluations(observerId);
        return ResponseEntity.ok(records);
    }

    /**
     * 查询指定评价记录详情
     * GET /api/observers/{observerId}/evaluations/{evaluationId}
     */
    @GetMapping("/{observerId}/evaluations/{evaluationId}")
    public ResponseEntity<EvaluationRecord> getEvaluation(
            @PathVariable String observerId,
            @PathVariable String evaluationId) {
        EvaluationRecord record = observerService.getEvaluation(evaluationId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    /**
     * 手动触发单次任务评价
     * POST /api/observers/{observerId}/evaluations/trigger
     *
     * <p>Body 示例：
     * <pre>
     * {
     *   "taskId": "task-001",
     *   "targetId": "c-coder-001",
     *   "targetType": "CHARACTER",
     *   "userInput": "帮我写一个排序算法",
     *   "agentOutput": "以下是快速排序的实现..."
     * }
     * </pre>
     */
    @PostMapping("/{observerId}/evaluations/trigger")
    public ResponseEntity<EvaluationRecord> triggerEvaluation(
            @PathVariable String observerId,
            @RequestBody EvaluationEngine.TaskData taskData) {
        EvaluationRecord record = observerService.evaluateTask(observerId, taskData);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(record);
    }

    /**
     * 手动触发周期性评价
     * POST /api/observers/{observerId}/evaluations/periodic
     * Body: { "targetType": "CHARACTER", "targetId": "c-001", "periodHours": 24 }
     */
    @PostMapping("/{observerId}/evaluations/periodic")
    public ResponseEntity<EvaluationRecord> triggerPeriodicEvaluation(
            @PathVariable String observerId,
            @RequestBody PeriodicEvaluationRequest body) {
        EvaluationRecord record = observerService.evaluatePeriodically(
                observerId, body.getTargetType(), body.getTargetId(), body.getPeriodHours());
        if (record == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(record);
    }

    // ==================== 优化动作管理 ====================

    /**
     * 查询目标的优化历史
     * GET /api/observers/{observerId}/optimizations?targetType=CHARACTER&targetId=c-001
     */
    @GetMapping("/{observerId}/optimizations")
    public ResponseEntity<List<OptimizationAction>> getOptimizationHistory(
            @PathVariable String observerId,
            @RequestParam EvaluationRecord.TargetType targetType,
            @RequestParam String targetId) {
        List<OptimizationAction> actions = observerService.getOptimizationHistory(targetType, targetId);
        return ResponseEntity.ok(actions);
    }

    /**
     * 查询指定优化动作详情
     * GET /api/observers/{observerId}/optimizations/{actionId}
     */
    @GetMapping("/{observerId}/optimizations/{actionId}")
    public ResponseEntity<OptimizationAction> getOptimizationAction(
            @PathVariable String observerId,
            @PathVariable String actionId) {
        OptimizationAction action = observerService.getOptimizationAction(actionId);
        if (action == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(action);
    }

    /**
     * 手动执行所有待处理的优化动作（autoOptimizationEnabled=false 时依赖此接口）
     * POST /api/observers/{observerId}/optimizations/execute-pending
     */
    @PostMapping("/{observerId}/optimizations/execute-pending")
    public ResponseEntity<List<OptimizationAction>> executePendingOptimizations(
            @PathVariable String observerId) {
        List<OptimizationAction> executed = observerService.executePendingOptimizations(observerId);
        return ResponseEntity.ok(executed);
    }

    /**
     * 回滚已执行的优化动作
     * POST /api/observers/{observerId}/optimizations/{actionId}/rollback
     */
    @PostMapping("/{observerId}/optimizations/{actionId}/rollback")
    public ResponseEntity<OptimizationAction> rollbackOptimization(
            @PathVariable String observerId,
            @PathVariable String actionId) {
        OptimizationAction action = observerService.rollbackOptimization(actionId);
        return ResponseEntity.ok(action);
    }

    // ==================== 常识库（CommonSense）管理 ====================

    /**
     * 新增常识规则
     * POST /api/observers/common-senses
     *
     * <p>Body 示例：
     * <pre>
     * {
     *   "id": "cs-no-delete-user-data",
     *   "name": "禁止删除用户数据",
     *   "category": "PRIVACY",
     *   "severity": "CRITICAL",
     *   "rule": "action != DELETE && target != USER_DATA"
     * }
     * </pre>
     */
    @PostMapping("/common-senses")
    public ResponseEntity<CommonSense> createCommonSense(@RequestBody CommonSense commonSense) {
        CommonSense saved = commonSenseStore.save(commonSense);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * 查询所有常识规则
     * GET /api/observers/common-senses
     * GET /api/observers/common-senses?category=SAFETY
     * GET /api/observers/common-senses?severity=CRITICAL
     * GET /api/observers/common-senses?enabledOnly=true
     */
    @GetMapping("/common-senses")
    public ResponseEntity<List<CommonSense>> listCommonSenses(
            @RequestParam(required = false) CommonSense.Category category,
            @RequestParam(required = false) CommonSense.Severity severity,
            @RequestParam(required = false, defaultValue = "false") boolean enabledOnly) {
        List<CommonSense> list;
        if (category != null) {
            list = commonSenseStore.findByCategory(category);
        } else if (severity != null) {
            list = commonSenseStore.findBySeverity(severity);
        } else if (enabledOnly) {
            list = commonSenseStore.findEnabled();
        } else {
            list = commonSenseStore.findAll();
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 查询指定常识规则
     * GET /api/observers/common-senses/{commonSenseId}
     */
    @GetMapping("/common-senses/{commonSenseId}")
    public ResponseEntity<CommonSense> getCommonSense(@PathVariable String commonSenseId) {
        return commonSenseStore.findById(commonSenseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新常识规则
     * PUT /api/observers/common-senses/{commonSenseId}
     *
     * <p>注意：CRITICAL 级别常识不可禁用。
     */
    @PutMapping("/common-senses/{commonSenseId}")
    public ResponseEntity<CommonSense> updateCommonSense(
            @PathVariable String commonSenseId,
            @RequestBody CommonSense commonSense) {
        commonSense.setId(commonSenseId);
        // 关键级别常识不允许被禁用
        CommonSense existing = commonSenseStore.findById(commonSenseId)
                .orElseThrow(() -> new IllegalArgumentException("CommonSense not found: " + commonSenseId));
        if (existing.isCritical() && !commonSense.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CommonSense saved = commonSenseStore.save(commonSense);
        return ResponseEntity.ok(saved);
    }

    /**
     * 删除常识规则（CRITICAL 级别不允许删除）
     * DELETE /api/observers/common-senses/{commonSenseId}
     */
    @DeleteMapping("/common-senses/{commonSenseId}")
    public ResponseEntity<Void> deleteCommonSense(@PathVariable String commonSenseId) {
        CommonSense existing = commonSenseStore.findById(commonSenseId)
                .orElseThrow(() -> new IllegalArgumentException("CommonSense not found: " + commonSenseId));
        if (existing.isCritical()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        commonSenseStore.delete(commonSenseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 校验某个优化动作是否会触犯常识规则（调试接口）
     * POST /api/observers/common-senses/validate
     *
     * <p>Body 示例：
     * <pre>
     * {
     *   "actionTargetType": "CHARACTER",
     *   "actionType": "UPDATE_MIND",
     *   "parameters": { "field": "personality", "value": "..." }
     * }
     * </pre>
     */
    @PostMapping("/common-senses/validate")
    public ResponseEntity<CommonSenseValidator.ValidationResult> validateAction(
            @RequestBody ValidateActionRequest body) {
        CommonSenseValidator.ValidationResult result = observerService.validateAgainstCommonSense(
                body.getActionTargetType(), body.getActionType(), body.getParameters());
        return ResponseEntity.ok(result);
    }

    // ==================== 请求体 DTO ====================

    @Data
    public static class PeriodicEvaluationRequest {
        private EvaluationRecord.TargetType targetType;
        private String targetId;
        /** 评价时间窗口（小时） */
        private int periodHours;
    }

    @Data
    public static class ValidateActionRequest {
        private String actionTargetType;
        private String actionType;
        private Map<String, Object> parameters;
    }
}
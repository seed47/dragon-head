package org.dragon.observer;

import java.time.LocalDateTime;
import java.util.List;

import org.dragon.character.CharacterRegistry;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.commons.CommonSenseStore;
import org.dragon.observer.commons.CommonSenseValidator;
import org.dragon.observer.evaluation.EvaluationEngine;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.EvaluationRecordStore;
import org.dragon.observer.optimization.OptimizationAction;
import org.dragon.observer.optimization.OptimizationActionStore;
import org.dragon.observer.optimization.OptimizationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * ObserverService 观察者服务
 * Observer 模块的核心协调器，负责协调各个组件完成观察、评价、优化闭环
 *
 * @author wyj
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ObserverService {

    private static final Logger log = LoggerFactory.getLogger(ObserverService.class);

    private final ObserverRegistry observerRegistry;
    private final EvaluationRecordStore evaluationRecordStore;
    private final OptimizationActionStore optimizationActionStore;
    private final DataCollector dataCollector;
    private final EvaluationEngine evaluationEngine;
    private final OptimizationExecutor optimizationExecutor;
    private final CommonSenseValidator commonSenseValidator;
    private final CommonSenseStore commonSenseStore;
    private final CharacterRegistry characterRegistry;

    /**
     * 评价任务
     *
     * @param observerId Observer ID
     * @param taskData   任务数据
     * @return 评价记录
     */
    public EvaluationRecord evaluateTask(String observerId, EvaluationEngine.TaskData taskData) {
        Observer observer = observerRegistry.get(observerId)
                .orElseThrow(() -> new IllegalArgumentException("Observer not found: " + observerId));

        if (!observer.isActive()) {
            log.warn("[ObserverService] Observer is not active: {}", observerId);
            return null;
        }

        log.info("[ObserverService] Evaluating task: {} for target: {}", taskData.getTaskId(), taskData.getTargetId());

        // 使用评价引擎进行评价
        EvaluationRecord record = evaluationEngine.evaluateByRules(taskData);

        // 检查是否需要触发优化
        if (record != null && observer.isAutoOptimizationEnabled()) {
            if (record.needsOptimization(observer.getOptimizationThreshold())) {
                log.info("[ObserverService] Task evaluation below threshold, triggering optimization");
                triggerOptimization(observer, record);
            }
        }

        return record;
    }

    /**
     * 周期性评价
     *
     * @param observerId  Observer ID
     * @param targetType  目标类型
     * @param targetId    目标 ID
     * @param periodHours 评价周期（小时）
     * @return 评价记录
     */
    public EvaluationRecord evaluatePeriodically(String observerId, EvaluationRecord.TargetType targetType,
                                                 String targetId, int periodHours) {
        Observer observer = observerRegistry.get(observerId)
                .orElseThrow(() -> new IllegalArgumentException("Observer not found: " + observerId));

        if (!observer.isActive()) {
            log.warn("[ObserverService] Observer is not active: {}", observerId);
            return null;
        }

        log.info("[ObserverService] Periodic evaluation for {}: {}", targetType, targetId);

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(periodHours);

        // 采集该时间段的数据
        List<EvaluationEngine.TaskData> taskDataList;
        if (targetType == EvaluationRecord.TargetType.CHARACTER) {
            taskDataList = dataCollector.collectCharacterTaskData(targetId, startTime, endTime);
        } else {
            // WORKSPACE 类型的数据采集
            taskDataList = dataCollector.collectWorkspaceTaskData(targetId, startTime, endTime);
        }

        if (taskDataList.isEmpty()) {
            log.warn("[ObserverService] No task data found for periodic evaluation");
            return null;
        }

        // 对每个任务进行评价
        for (EvaluationEngine.TaskData taskData : taskDataList) {
            evaluationEngine.evaluateByRules(taskData);
        }

        // 执行周期性评价
        EvaluationRecord record = evaluationEngine.evaluatePeriodically(targetType, targetId, startTime, endTime);

        // 检查是否需要触发优化
        if (record != null && observer.isAutoOptimizationEnabled()) {
            if (record.needsOptimization(observer.getOptimizationThreshold())) {
                log.info("[ObserverService] Periodic evaluation below threshold, triggering optimization");
                triggerOptimization(observer, record);
            }
        }

        return record;
    }

    /**
     * 触发优化
     *
     * @param observer   Observer
     * @param evaluation 评价记录
     * @return 生成的优化动作列表
     */
    public List<OptimizationAction> triggerOptimization(Observer observer, EvaluationRecord evaluation) {
        log.info("[ObserverService] Triggering optimization for evaluation: {}", evaluation.getId());

        // 从评价生成优化动作
        List<OptimizationAction> actions = optimizationExecutor.generateActionsFromEvaluation(evaluation.getId());

        // 对每个动作进行常识校验
        for (OptimizationAction action : actions) {
            var validationResult = commonSenseValidator.validate(
                    action.getTargetType().name(),
                    action.getActionType().name(),
                    action.getParameters());

            if (!validationResult.isValid()) {
                log.warn("[ObserverService] Action rejected by common sense: {}", action.getId());
                action.markRejected("Violated common sense: " + validationResult.getViolations());
                optimizationActionStore.save(action);
            } else if (observer.isAutoOptimizationEnabled()) {
                // 执行优化
                optimizationExecutor.execute(action);
            }
        }

        return actions;
    }

    /**
     * 执行待处理的优化动作
     *
     * @param observerId Observer ID
     * @return 执行结果列表
     */
    public List<OptimizationAction> executePendingOptimizations(String observerId) {
        Observer observer = observerRegistry.get(observerId)
                .orElseThrow(() -> new IllegalArgumentException("Observer not found: " + observerId));

        if (!observer.isAutoOptimizationEnabled()) {
            log.info("[ObserverService] Auto optimization is disabled");
            return List.of();
        }

        List<OptimizationAction> pendingActions = optimizationActionStore.findPendingOrdered(10);
        log.info("[ObserverService] Executing {} pending optimization actions", pendingActions.size());

        return optimizationExecutor.executeBatch(pendingActions.stream().map(OptimizationAction::getId).toList());
    }

    /**
     * 回滚优化动作
     *
     * @param actionId 优化动作 ID
     * @return 回滚后的动作
     */
    public OptimizationAction rollbackOptimization(String actionId) {
        return optimizationExecutor.rollback(actionId);
    }

    /**
     * 获取评价记录
     *
     * @param evaluationId 评价记录 ID
     * @return 评价记录
     */
    public EvaluationRecord getEvaluation(String evaluationId) {
        return evaluationRecordStore.findById(evaluationId).orElse(null);
    }

    /**
     * 获取目标的所有评价记录
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 评价记录列表
     */
    public List<EvaluationRecord> getEvaluationsByTarget(EvaluationRecord.TargetType targetType, String targetId) {
        return evaluationRecordStore.findByTarget(targetType, targetId);
    }

    /**
     * 获取低于阈值的评价记录
     *
     * @param observerId Observer ID
     * @return 评价记录列表
     */
    public List<EvaluationRecord> getBelowThresholdEvaluations(String observerId) {
        Observer observer = observerRegistry.get(observerId)
                .orElseThrow(() -> new IllegalArgumentException("Observer not found: " + observerId));

        return evaluationRecordStore.findBelowThreshold(observer.getOptimizationThreshold());
    }

    /**
     * 获取优化动作状态
     *
     * @param actionId 动作 ID
     * @return 优化动作
     */
    public OptimizationAction getOptimizationAction(String actionId) {
        return optimizationActionStore.findById(actionId).orElse(null);
    }

    /**
     * 获取目标的优化历史
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 优化动作列表
     */
    public List<OptimizationAction> getOptimizationHistory(EvaluationRecord.TargetType targetType, String targetId) {
        OptimizationAction.TargetType type = targetType == EvaluationRecord.TargetType.CHARACTER
                ? OptimizationAction.TargetType.CHARACTER
                : OptimizationAction.TargetType.WORKSPACE;
        return optimizationActionStore.findByTarget(type, targetId);
    }

    /**
     * 验证优化动作是否符合常识
     *
     * @param actionTargetType 目标类型
     * @param actionType      动作类型
     * @param parameters      参数
     * @return 校验结果
     */
    public CommonSenseValidator.ValidationResult validateAgainstCommonSense(
            String actionTargetType, String actionType, java.util.Map<String, Object> parameters) {
        return commonSenseValidator.validate(actionTargetType, actionType, parameters);
    }

    /**
     * 获取 Observer 统计信息
     *
     * @param observerId Observer ID
     * @return 统计信息
     */
    public ObserverStats getStats(String observerId) {
        // 验证 Observer 存在
        observerRegistry.get(observerId)
                .orElseThrow(() -> new IllegalArgumentException("Observer not found: " + observerId));

        ObserverStats stats = new ObserverStats();
        stats.setObserverId(observerId);
        stats.setTotalEvaluations(evaluationRecordStore.count());
        stats.setTotalOptimizations(optimizationActionStore.count());
        stats.setPendingOptimizations(optimizationActionStore.findPending().size());
        stats.setCommonSenseCount(commonSenseStore.count());
        stats.setCharacterCount(characterRegistry.size());

        return stats;
    }

    /**
     * Observer 统计信息
     */
    public static class ObserverStats {
        private String observerId;
        private int totalEvaluations;
        private int totalOptimizations;
        private int pendingOptimizations;
        private int commonSenseCount;
        private int characterCount;

        // Getters and setters
        public String getObserverId() { return observerId; }
        public void setObserverId(String observerId) { this.observerId = observerId; }
        public int getTotalEvaluations() { return totalEvaluations; }
        public void setTotalEvaluations(int totalEvaluations) { this.totalEvaluations = totalEvaluations; }
        public int getTotalOptimizations() { return totalOptimizations; }
        public void setTotalOptimizations(int totalOptimizations) { this.totalOptimizations = totalOptimizations; }
        public int getPendingOptimizations() { return pendingOptimizations; }
        public void setPendingOptimizations(int pendingOptimizations) { this.pendingOptimizations = pendingOptimizations; }
        public int getCommonSenseCount() { return commonSenseCount; }
        public void setCommonSenseCount(int commonSenseCount) { this.commonSenseCount = commonSenseCount; }
        public int getCharacterCount() { return characterCount; }
        public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }
    }
}

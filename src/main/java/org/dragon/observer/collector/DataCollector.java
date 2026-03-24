package org.dragon.observer.collector;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.observer.evaluation.EvaluationEngine;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * DataCollector 数据采集器
 * 从 Workspace、Organization、Character 采集执行数据
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class DataCollector {

    private static final Logger log = LoggerFactory.getLogger(DataCollector.class);

    private final WorkspaceRegistry workspaceRegistry;
    private final CharacterRegistry characterRegistry;
    private final TaskStore taskStore;

    /**
     * 采集任务数据
     *
     * @param workspaceId Workspace ID（可选，为 null 表示全局）
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 任务数据列表
     */
    public List<EvaluationEngine.TaskData> collectTaskData(String workspaceId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("[DataCollector] Collecting task data for workspace: {}, time range: {} - {}",
                workspaceId, startTime, endTime);

        List<EvaluationEngine.TaskData> taskDataList = new ArrayList<>();

        List<Task> tasks;
        if (workspaceId != null) {
            tasks = taskStore.findByWorkspaceId(workspaceId);
        } else {
            // 全局采集：从所有 Character 关联的任务获取
            tasks = new ArrayList<>();
            for (Character character : characterRegistry.listAll()) {
                tasks.addAll(taskStore.findByCharacterId(character.getId()));
            }
        }

        for (Task task : tasks) {
            LocalDateTime taskTime = task.getCreatedAt();
            if (taskTime != null && !taskTime.isBefore(startTime) && !taskTime.isAfter(endTime)) {
                EvaluationEngine.TaskData taskData = convertToTaskData(task);
                taskDataList.add(taskData);
            }
        }

        log.info("[DataCollector] Collected {} task data", taskDataList.size());
        return taskDataList;
    }

    /**
     * 采集单个 Character 的任务数据
     *
     * @param characterId Character ID
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 任务数据列表
     */
    public List<EvaluationEngine.TaskData> collectCharacterTaskData(String characterId, LocalDateTime startTime, LocalDateTime endTime) {
        List<EvaluationEngine.TaskData> taskDataList = new ArrayList<>();

        List<Task> tasks = taskStore.findByCharacterId(characterId);
        for (Task task : tasks) {
            LocalDateTime taskTime = task.getCreatedAt();
            if (taskTime != null && !taskTime.isBefore(startTime) && !taskTime.isAfter(endTime)) {
                EvaluationEngine.TaskData taskData = convertToTaskData(task);
                taskDataList.add(taskData);
            }
        }

        return taskDataList;
    }

    /**
     * 采集单个 Organization 的任务数据
     * Organization 下的所有 Character 的任务汇总
     *
     * @param orgId     Organization ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 任务数据列表
     */
    public List<EvaluationEngine.TaskData> collectOrganizationTaskData(String orgId, LocalDateTime startTime, LocalDateTime endTime) {
        List<EvaluationEngine.TaskData> taskDataList = new ArrayList<>();

        // TODO: 需要 Organization 和 Character 的关联关系
        // 暂时返回空列表

        return taskDataList;
    }

    /**
     * 采集 Workspace 的任务数据
     *
     * @param workspaceId Workspace ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 任务数据列表
     */
    public List<EvaluationEngine.TaskData> collectWorkspaceTaskData(String workspaceId, LocalDateTime startTime, LocalDateTime endTime) {
        List<EvaluationEngine.TaskData> taskDataList = new ArrayList<>();

        List<Task> tasks = taskStore.findByWorkspaceId(workspaceId);
        for (Task task : tasks) {
            LocalDateTime taskTime = task.getCreatedAt();
            if (taskTime != null && !taskTime.isBefore(startTime) && !taskTime.isAfter(endTime)) {
                EvaluationEngine.TaskData taskData = convertToTaskData(task);
                taskDataList.add(taskData);
            }
        }

        return taskDataList;
    }

    /**
     * 采集性能指标
     *
     * @param workspaceId Workspace ID
     * @return 性能指标
     */
    public MetricsData collectMetrics(String workspaceId) {
        log.info("[DataCollector] Collecting metrics for workspace: {}", workspaceId);

        MetricsData metrics = new MetricsData();
        metrics.setWorkspaceId(workspaceId);
        metrics.setTimestamp(LocalDateTime.now());

        // 统计 Character 数量
        int characterCount = characterRegistry.size();
        metrics.setCharacterCount(characterCount);

        // 统计任务总数和状态分布
        List<Task> tasks = workspaceId != null
                ? taskStore.findByWorkspaceId(workspaceId)
                : taskStore.findByStatus(null).stream().collect(Collectors.toList());

        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();
        int failedTasks = (int) tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.FAILED)
                .count();

        metrics.setTotalTasks(totalTasks);
        metrics.setCompletedTasks(completedTasks);
        metrics.setFailedTasks(failedTasks);
        metrics.setSuccessRate(totalTasks > 0 ? (double) completedTasks / totalTasks : 0.0);

        return metrics;
    }

    /**
     * 将 Task 转换为 TaskData
     */
    private EvaluationEngine.TaskData convertToTaskData(Task task) {
        EvaluationEngine.TaskData taskData = new EvaluationEngine.TaskData();
        taskData.setTaskId(task.getId());
        taskData.setTargetId(task.getCharacterId());
        taskData.setTargetType(org.dragon.observer.evaluation.EvaluationRecord.TargetType.CHARACTER);
        taskData.setTaskInput(task.getInput() != null ? task.getInput().toString() : null);
        taskData.setTaskOutput(task.getResult());
        taskData.setSuccess(task.getStatus() == TaskStatus.COMPLETED);
        taskData.setErrorMessage(task.getErrorMessage());
        taskData.setStartTime(task.getCreatedAt());
        taskData.setEndTime(task.getUpdatedAt());

        // 计算耗时
        if (task.getCreatedAt() != null && task.getUpdatedAt() != null) {
            taskData.setDurationMs(
                    java.time.Duration.between(task.getCreatedAt(), task.getUpdatedAt()).toMillis());
        }

        return taskData;
    }

    /**
     * 性能指标数据
     */
    public static class MetricsData {
        private String workspaceId;
        private LocalDateTime timestamp;
        private int characterCount;
        private int memberCount;
        private int totalTasks;
        private int completedTasks;
        private int failedTasks;
        private double successRate;
        private Map<String, Object> customMetrics;

        // Getters and setters
        public String getWorkspaceId() { return workspaceId; }
        public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public int getCharacterCount() { return characterCount; }
        public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }
        public int getMemberCount() { return memberCount; }
        public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
        public int getTotalTasks() { return totalTasks; }
        public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
        public int getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }
        public int getFailedTasks() { return failedTasks; }
        public void setFailedTasks(int failedTasks) { this.failedTasks = failedTasks; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public Map<String, Object> getCustomMetrics() {
            if (customMetrics == null) customMetrics = new HashMap<>();
            return customMetrics;
        }
        public void setCustomMetrics(Map<String, Object> customMetrics) { this.customMetrics = customMetrics; }
    }
}
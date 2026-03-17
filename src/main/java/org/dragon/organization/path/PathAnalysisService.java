package org.dragon.organization.path;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PathAnalysisService 路径分析服务
 * 识别高频、高效的工作路径，标记为最佳实践
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PathAnalysisService {

    private final PathStore pathStore;

    /**
     * 路径执行记录
     */
    private final Map<String, PathExecution> executions = new HashMap<>();

    /**
     * 记录路径执行
     *
     * @param execution 执行记录
     */
    public void recordExecution(PathExecution execution) {
        executions.put(execution.getExecutionId(), execution);
        log.info("[PathAnalysisService] Recorded execution {} for path {}", execution.getExecutionId(), execution.getPathId());
    }

    /**
     * 分析并标记最佳实践
     *
     * @param organizationId 组织 ID
     * @return 标记为最佳实践的路径列表
     */
    public List<WorkflowPath> analyzeAndMarkBestPractices(String organizationId) {
        List<WorkflowPath> paths = pathStore.findByOrganizationId(organizationId);
        List<WorkflowPath> bestPractices = new ArrayList<>();

        for (WorkflowPath path : paths) {
            List<PathExecution> pathExecutions = executions.values().stream()
                    .filter(e -> e.getPathId().equals(path.getId()))
                    .toList();

            if (pathExecutions.size() >= 3) { // 至少执行3次才参与分析
                // 计算成功率
                long successCount = pathExecutions.stream()
                        .filter(PathExecution::isSuccess)
                        .count();
                double successRate = (double) successCount / pathExecutions.size();

                // 计算平均执行时长
                long avgDuration = pathExecutions.stream()
                        .mapToLong(PathExecution::getDurationMs)
                        .sum() / pathExecutions.size();

                // 更新路径指标
                WorkflowPath.PathMetrics metrics = WorkflowPath.PathMetrics.builder()
                        .successRate(successRate)
                        .avgDurationMs(avgDuration)
                        .usageCount(pathExecutions.size())
                        .build();

                path.setMetrics(metrics);

                // 标记成功率>=80%且执行>=3次的路径为最佳实践
                if (successRate >= 0.8) {
                    path.setBestPractice(true);
                    bestPractices.add(path);
                }

                path.setUpdatedAt(LocalDateTime.now());
                pathStore.update(path);
            }
        }

        log.info("[PathAnalysisService] Marked {} paths as best practices in organization {}", bestPractices.size(), organizationId);
        return bestPractices;
    }

    /**
     * PathExecution 路径执行记录
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PathExecution {
        private String executionId;
        private String pathId;
        private String organizationId;
        private boolean success;
        private long durationMs;
        private int tokenConsumption;
        private LocalDateTime executedAt;
        private List<String> steps;
    }
}

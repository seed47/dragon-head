package org.dragon.organization.path;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PathMatchingService 路径匹配服务
 * 当新任务到达时，匹配相似历史路径，推荐或自动复用已有流程
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PathMatchingService {

    private final PathStore pathStore;

    /**
     * 匹配相似路径
     *
     * @param organizationId 组织 ID
     * @param taskType 任务类型
     * @param taskInput 任务输入（用于关键词匹配）
     * @return 匹配的路径
     */
    public Optional<WorkflowPath> match(String organizationId, String taskType, Object taskInput) {
        // 1. 先尝试匹配最佳实践
        List<WorkflowPath> bestPractices = pathStore.findBestPractices(organizationId);
        if (!bestPractices.isEmpty()) {
            // 简单匹配：优先返回成功率最高的
            bestPractices.sort((a, b) -> {
                double aRate = a.getMetrics() != null ? a.getMetrics().getSuccessRate() : 0;
                double bRate = b.getMetrics() != null ? b.getMetrics().getSuccessRate() : 0;
                return Double.compare(bRate, aRate);
            });
            log.info("[PathMatchingService] Matched best practice path {} for task type {} in org {}",
                    bestPractices.get(0).getId(), taskType, organizationId);
            return Optional.of(bestPractices.get(0));
        }

        // 2. 没有最佳实践时，匹配同任务类型的路径
        List<WorkflowPath> paths = pathStore.findByOrganizationIdAndTaskType(organizationId, taskType);
        if (!paths.isEmpty()) {
            paths.sort((a, b) -> {
                int aCount = a.getMetrics() != null ? a.getMetrics().getUsageCount() : 0;
                int bCount = b.getMetrics() != null ? b.getMetrics().getUsageCount() : 0;
                return Integer.compare(bCount, aCount);
            });
            log.info("[PathMatchingService] Matched path {} for task type {} in org {}",
                    paths.get(0).getId(), taskType, organizationId);
            return Optional.of(paths.get(0));
        }

        log.info("[PathMatchingService] No matching path found for task type {} in org {}",
                taskType, organizationId);
        return Optional.empty();
    }

    /**
     * 获取推荐路径列表
     *
     * @param organizationId 组织 ID
     * @param taskType 任务类型
     * @param limit 返回数量限制
     * @return 推荐的路径列表
     */
    public List<WorkflowPath> getRecommendedPaths(String organizationId, String taskType, int limit) {
        List<WorkflowPath> paths = pathStore.findByOrganizationIdAndTaskType(organizationId, taskType);

        // 按使用次数和成功率排序
        paths.sort((a, b) -> {
            int aScore = (a.getMetrics() != null ? a.getMetrics().getUsageCount() : 0) *
                    (a.getMetrics() != null ? (int)(a.getMetrics().getSuccessRate() * 100) : 0);
            int bScore = (b.getMetrics() != null ? b.getMetrics().getUsageCount() : 0) *
                    (b.getMetrics() != null ? (int)(b.getMetrics().getSuccessRate() * 100) : 0);
            return Integer.compare(bScore, aScore);
        });

        return paths.stream().limit(limit).toList();
    }
}

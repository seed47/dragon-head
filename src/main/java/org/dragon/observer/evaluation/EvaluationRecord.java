package org.dragon.observer.evaluation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EvaluationRecord 评价记录实体
 * 记录 Observer 对任务或周期性工作的质量评估结果
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRecord {

    /**
     * 目标类型
     */
    public enum TargetType {
        CHARACTER,     // Character
        WORKSPACE      // Workspace
    }

    /**
     * 评价类型
     */
    public enum EvaluationType {
        TASK,         // 任务完成后评价
        PERIODIC,     // 周期性综合评价
        MANUAL        // 人工审核评价
    }

    /**
     * 评价记录唯一标识
     */
    private String id;

    /**
     * 目标类型
     */
    private TargetType targetType;

    /**
     * 目标 ID
     */
    private String targetId;

    /**
     * 关联的任务 ID（任务评价时）
     */
    private String taskId;

    /**
     * 评价类型
     */
    private EvaluationType evaluationType;

    /**
     * 评分 - 任务完成度 (0-1)
     */
    private Double taskCompletionScore;

    /**
     * 评分 - 效率 (0-1)
     */
    private Double efficiencyScore;

    /**
     * 评分 - 合规性 (0-1)
     */
    private Double complianceScore;

    /**
     * 评分 - 协作表现 (0-1)
     */
    private Double collaborationScore;

    /**
     * 评分 - 用户满意度 (0-1)
     */
    private Double satisfactionScore;

    /**
     * 综合评分 (0-1)
     */
    private Double overallScore;

    /**
     * 分析内容
     */
    private String analysis;

    /**
     * 改进建议列表
     */
    private List<String> suggestions;

    /**
     * 置信度 (0-1)
     */
    @Builder.Default
    private Double confidence = 0.8;

    /**
     * 评价依据数据
     */
    private Map<String, Object> evidence;

    /**
     * 评价时间
     */
    private LocalDateTime timestamp;

    /**
     * 评价者 (OBSERVER, ADMIN, SYSTEM)
     */
    private String evaluator;

    /**
     * 扩展字段
     */
    private Map<String, Object> extensions;

    /**
     * 计算综合评分
     */
    public void calculateOverallScore() {
        double total = 0;
        int count = 0;

        if (taskCompletionScore != null) {
            total += taskCompletionScore;
            count++;
        }
        if (efficiencyScore != null) {
            total += efficiencyScore;
            count++;
        }
        if (complianceScore != null) {
            total += complianceScore;
            count++;
        }
        if (collaborationScore != null) {
            total += collaborationScore;
            count++;
        }
        if (satisfactionScore != null) {
            total += satisfactionScore;
            count++;
        }

        this.overallScore = count > 0 ? total / count : 0.0;
    }

    /**
     * 检查是否需要触发优化
     *
     * @param threshold 阈值
     * @return 是否低于阈值
     */
    public boolean needsOptimization(double threshold) {
        calculateOverallScore();
        return overallScore != null && overallScore < threshold;
    }
}

package org.dragon.organization.recruitment;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CandidateEvaluation 候选人评估
 * 对候选 Character 进行能力测试、性格匹配等评估
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateEvaluation {

    /**
     * 评估状态
     */
    public enum Status {
        PENDING,    // 待评估
        EVALUATING, // 评估中
        COMPLETED,  // 已完成
        REJECTED    // 已拒绝
    }

    /**
     * 评估 ID
     */
    private String id;

    /**
     * 招聘需求 ID
     */
    private String recruitmentRequestId;

    /**
     * 组织 ID
     */
    private String organizationId;

    /**
     * 候选 Character ID
     */
    private String candidateCharacterId;

    /**
     * 评估状态
     */
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * 能力匹配评分
     */
    private double capabilityScore;

    /**
     * 性格匹配评分
     */
    private double personalityScore;

    /**
     * 历史表现评分
     */
    private double historyScore;

    /**
     * 综合评分
     */
    private double overallScore;

    /**
     * 评估详情
     */
    private Map<String, Object> evaluationDetails;

    /**
     * 评估建议
     */
    private String recommendation;

    /**
     * 评估时间
     */
    private LocalDateTime evaluatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

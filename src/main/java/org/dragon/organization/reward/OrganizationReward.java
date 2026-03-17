package org.dragon.organization.reward;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OrganizationReward 组织间奖惩
 * 当多个组织协作完成任务时，参与组织可获得奖励
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationReward {

    /**
     * 奖励类型
     */
    public enum RewardType {
        COLLABORATION_BONUS,     // 协作奖励
        RESOURCE_SHARING,        // 资源共享奖励
        KNOWLEDGE_CONTRIBUTION,  // 知识贡献
        PENALTY                  // 惩罚
    }

    /**
     * 货币类型
     */
    public enum Currency {
        TOKEN,       // Token
        CREDITS,     // 积分
        REPUTATION   // 声誉
    }

    /**
     * 状态
     */
    public enum Status {
        PENDING,   // 待处理
        APPLIED,   // 已应用
        REJECTED   // 已拒绝
    }

    /**
     * 奖励 ID
     */
    private String id;

    /**
     * 奖励来源组织 ID
     */
    private String sourceOrganizationId;

    /**
     * 被奖励组织 ID
     */
    private String targetOrganizationId;

    /**
     * 关联任务 ID
     */
    private String taskId;

    /**
     * 奖励类型
     */
    private RewardType rewardType;

    /**
     * 奖励数量
     */
    private double amount;

    /**
     * 货币类型
     */
    private Currency currency;

    /**
     * 原因描述
     */
    private String reason;

    /**
     * 状态
     */
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
}

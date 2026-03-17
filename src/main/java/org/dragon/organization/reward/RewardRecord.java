package org.dragon.organization.reward;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RewardRecord 奖惩记录
 * 记录每次奖惩的详细信息，用于审计
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardRecord {

    /**
     * 目标类型
     */
    public enum TargetType {
        MEMBER,        // 成员
        ORGANIZATION   // 组织
    }

    /**
     * 记录 ID
     */
    private String id;

    /**
     * 规则 ID
     */
    private String ruleId;

    /**
     * 组织 ID
     */
    private String organizationId;

    /**
     * 目标类型
     */
    private TargetType targetType;

    /**
     * 目标 ID (characterId 或 organizationId)
     */
    private String targetId;

    /**
     * 触发事件
     */
    private String triggerEvent;

    /**
     * 执行的奖惩动作
     */
    private List<RewardRule.Action> actions;

    /**
     * 执行前的值
     */
    private Map<String, Object> previousValues;

    /**
     * 执行后的值
     */
    private Map<String, Object> newValues;

    /**
     * 执行者 ID
     */
    private String appliedBy;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
}

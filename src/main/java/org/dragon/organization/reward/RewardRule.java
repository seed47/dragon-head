package org.dragon.organization.reward;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RewardRule 奖惩规则
 * 定义组织对成员的奖惩规则
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardRule {

    /**
     * 规则范围
     */
    public enum Scope {
        SCOPE_MEMBER  // 组织对成员
    }

    /**
     * 触发类型
     */
    public enum TriggerType {
        TASK_COMPLETE,              // 任务完成
        TASK_FAIL,                  // 任务失败
        HIGH_QUALITY_CONTRIBUTION,  // 高质量贡献
        COLLABORATION_EXCELLENCE,   // 协作优秀
        REPEAT_FAILURE,             // 连续失败
        CUSTOM                      // 自定义事件
    }

    /**
     * 动作类型
     */
    public enum Action {
        WEIGHT_ADJUST,       // 权重调整
        PRIORITY_ADJUST,    // 优先级调整
        QUOTA_ADJUST,       // 资源配额调整
        REPUTATION_ADJUST,  // 声誉积分调整
        ROLE_PROMOTE,       // 角色晋升
        ROLE_DEMOTE         // 角色降级
    }

    /**
     * 规则 ID
     */
    private String id;

    /**
     * 组织 ID
     */
    private String organizationId;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 规则范围
     */
    @Builder.Default
    private Scope scope = Scope.SCOPE_MEMBER;

    /**
     * 触发类型
     */
    private TriggerType triggerType;

    /**
     * 触发条件
     */
    private List<RewardCondition> conditions;

    /**
     * 奖励/惩罚动作
     */
    private List<Action> actions;

    /**
     * 动作值映射
     */
    private Map<String, Object> actionValues;

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 优先级
     */
    @Builder.Default
    private int priority = 0;

    /**
     * 规则有效期
     */
    private Duration validityPeriod;

    /**
     * RewardCondition 奖励条件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardCondition {
        /**
         * 连续成功次数
         */
        private Integer consecutiveSuccessCount;

        /**
         * 任务复杂度
         */
        private Integer taskComplexity;

        /**
         * 质量分数阈值
         */
        private Double qualityScoreThreshold;

        /**
         * 自定义表达式
         */
        private String customExpression;
    }
}

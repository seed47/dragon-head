package org.dragon.organization.personality;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OrganizationPersonality 组织性格
 * 类比 Character Mind，用于描述组织的文化、价值观、工作风格等
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationPersonality {

    /**
     * 工作风格
     */
    public enum WorkingStyle {
        AGGRESSIVE,     // 激进型 - 快速迭代、勇于冒险
        CONSERVATIVE,   // 保守型 - 稳定优先、风险规避
        COLLABORATIVE,  // 协作型 - 团队合作、共识决策
        INNOVATIVE,     // 创新型 - 探索新方案、突破常规
        ANALYTICAL      // 分析型 - 数据驱动、理性决策
    }

    /**
     * 决策模式
     */
    public enum DecisionPattern {
        DEMOCRATIC,     // 民主决策 - 多数投票
        AUTOCRATIC,     // 集中决策 - 领导决定
        CONSENSUS,      // 共识决策 - 全员同意
        CONSULTATIVE    // 咨询决策 - 听取意见后决定
    }

    /**
     * 文化描述文件路径
     * 指向一个描述组织文化的配置文件
     */
    private String cultureDescriptorPath;

    /**
     * 工作风格
     */
    @Builder.Default
    private WorkingStyle workingStyle = WorkingStyle.COLLABORATIVE;

    /**
     * 决策模式
     */
    @Builder.Default
    private DecisionPattern decisionPattern = DecisionPattern.CONSULTATIVE;

    /**
     * 风险容忍度 (0-1)
     * 0 表示完全风险规避，1 表示完全风险偏好
     */
    @Builder.Default
    private double riskTolerance = 0.5;

    /**
     * 协作偏好
     * 描述组织偏好的协作方式
     */
    private String collaborationPreference;

    /**
     * 核心价值观
     * 组织最重要的价值观念列表
     */
    private String[] coreValues;

    /**
     * 行为准则
     * 组织成员应遵守的行为规范
     */
    private String[] behaviorGuidelines;

    /**
     * 自定义属性
     */
    private Map<String, Object> customAttributes;
}

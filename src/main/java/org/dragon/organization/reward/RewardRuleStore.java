package org.dragon.organization.reward;

import java.util.List;
import java.util.Optional;

/**
 * RewardRuleStore 奖惩规则存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface RewardRuleStore {

    /**
     * 保存规则
     *
     * @param rule 规则
     */
    void save(RewardRule rule);

    /**
     * 根据 ID 获取规则
     *
     * @param ruleId 规则 ID
     * @return Optional 规则
     */
    Optional<RewardRule> findById(String ruleId);

    /**
     * 根据组织 ID 获取所有规则
     *
     * @param organizationId 组织 ID
     * @return 规则列表
     */
    List<RewardRule> findByOrganizationId(String organizationId);

    /**
     * 根据组织 ID 和触发类型获取规则
     *
     * @param organizationId 组织 ID
     * @param triggerType 触发类型
     * @return 规则列表
     */
    List<RewardRule> findByOrganizationIdAndTriggerType(
            String organizationId, RewardRule.TriggerType triggerType);

    /**
     * 获取组织启用的规则
     *
     * @param organizationId 组织 ID
     * @return 启用的规则列表
     */
    List<RewardRule> findEnabledByOrganizationId(String organizationId);

    /**
     * 更新规则
     *
     * @param rule 规则
     */
    void update(RewardRule rule);

    /**
     * 删除规则
     *
     * @param ruleId 规则 ID
     */
    void delete(String ruleId);

    /**
     * 根据组织 ID 删除所有规则
     *
     * @param organizationId 组织 ID
     */
    void deleteByOrganizationId(String organizationId);
}

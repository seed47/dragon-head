package org.dragon.organization.reward;

import java.util.List;
import java.util.Optional;

/**
 * RewardRecordStore 奖惩记录存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface RewardRecordStore {

    /**
     * 保存记录
     *
     * @param record 记录
     */
    void save(RewardRecord record);

    /**
     * 根据 ID 获取记录
     *
     * @param recordId 记录 ID
     * @return Optional 记录
     */
    Optional<RewardRecord> findById(String recordId);

    /**
     * 根据组织 ID 获取记录
     *
     * @param organizationId 组织 ID
     * @param limit 限制数量
     * @return 记录列表
     */
    List<RewardRecord> findByOrganizationId(String organizationId, int limit);

    /**
     * 根据组织和目标 ID 获取记录
     *
     * @param organizationId 组织 ID
     * @param targetId 目标 ID
     * @param limit 限制数量
     * @return 记录列表
     */
    List<RewardRecord> findByOrganizationIdAndTargetId(
            String organizationId, String targetId, int limit);

    /**
     * 根据规则 ID 获取记录
     *
     * @param ruleId 规则 ID
     * @param limit 限制数量
     * @return 记录列表
     */
    List<RewardRecord> findByRuleId(String ruleId, int limit);

    /**
     * 删除组织所有记录
     *
     * @param organizationId 组织 ID
     */
    void deleteByOrganizationId(String organizationId);
}

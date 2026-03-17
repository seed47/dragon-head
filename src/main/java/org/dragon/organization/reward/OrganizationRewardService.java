package org.dragon.organization.reward;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * OrganizationRewardService 组织间奖惩服务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
public class OrganizationRewardService {

    private final Map<String, OrganizationReward> rewards = new ConcurrentHashMap<>();

    /**
     * 授予奖励
     *
     * @param reward 奖励
     * @return 含 ID 的奖励
     */
    public OrganizationReward grantReward(OrganizationReward reward) {
        if (reward.getId() == null || reward.getId().isEmpty()) {
            reward.setId(UUID.randomUUID().toString());
        }
        if (reward.getTimestamp() == null) {
            reward.setTimestamp(LocalDateTime.now());
        }
        if (reward.getStatus() == null) {
            reward.setStatus(OrganizationReward.Status.PENDING);
        }

        rewards.put(reward.getId(), reward);
        log.info("[OrganizationRewardService] Granted reward: {} from {} to {}",
                reward.getId(), reward.getSourceOrganizationId(), reward.getTargetOrganizationId());

        return reward;
    }

    /**
     * 应用奖励
     *
     * @param rewardId 奖励 ID
     */
    public void applyReward(String rewardId) {
        OrganizationReward reward = rewards.get(rewardId);
        if (reward == null) {
            throw new IllegalArgumentException("Reward not found: " + rewardId);
        }

        reward.setStatus(OrganizationReward.Status.APPLIED);
        rewards.put(rewardId, reward);

        log.info("[OrganizationRewardService] Applied reward: {}", rewardId);
    }

    /**
     * 拒绝奖励
     *
     * @param rewardId 奖励 ID
     */
    public void rejectReward(String rewardId) {
        OrganizationReward reward = rewards.get(rewardId);
        if (reward == null) {
            throw new IllegalArgumentException("Reward not found: " + rewardId);
        }

        reward.setStatus(OrganizationReward.Status.REJECTED);
        rewards.put(rewardId, reward);

        log.info("[OrganizationRewardService] Rejected reward: {}", rewardId);
    }

    /**
     * 获取待处理奖励
     *
     * @param organizationId 组织 ID
     * @return 奖励列表
     */
    public List<OrganizationReward> getPendingRewards(String organizationId) {
        return rewards.values().stream()
                .filter(r -> r.getTargetOrganizationId().equals(organizationId)
                        && r.getStatus() == OrganizationReward.Status.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * 获取奖励历史
     *
     * @param organizationId 组织 ID
     * @param limit 限制数量
     * @return 奖励列表
     */
    public List<OrganizationReward> getRewardHistory(String organizationId, int limit) {
        return rewards.values().stream()
                .filter(r -> r.getSourceOrganizationId().equals(organizationId)
                        || r.getTargetOrganizationId().equals(organizationId))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}

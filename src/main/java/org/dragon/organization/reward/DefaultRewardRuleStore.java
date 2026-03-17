package org.dragon.organization.reward;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * DefaultRewardRuleStore 默认奖惩规则存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class DefaultRewardRuleStore implements RewardRuleStore {

    private final Map<String, RewardRule> rules = new ConcurrentHashMap<>();

    @Override
    public void save(RewardRule rule) {
        if (rule == null || rule.getId() == null) {
            throw new IllegalArgumentException("Rule or Rule id cannot be null");
        }
        rules.put(rule.getId(), rule);
    }

    @Override
    public Optional<RewardRule> findById(String ruleId) {
        return Optional.ofNullable(rules.get(ruleId));
    }

    @Override
    public List<RewardRule> findByOrganizationId(String organizationId) {
        return rules.values().stream()
                .filter(r -> r.getOrganizationId().equals(organizationId))
                .collect(Collectors.toList());
    }

    @Override
    public List<RewardRule> findByOrganizationIdAndTriggerType(
            String organizationId, RewardRule.TriggerType triggerType) {
        return rules.values().stream()
                .filter(r -> r.getOrganizationId().equals(organizationId)
                        && r.getTriggerType() == triggerType)
                .collect(Collectors.toList());
    }

    @Override
    public List<RewardRule> findEnabledByOrganizationId(String organizationId) {
        return rules.values().stream()
                .filter(r -> r.getOrganizationId().equals(organizationId)
                        && r.isEnabled())
                .collect(Collectors.toList());
    }

    @Override
    public void update(RewardRule rule) {
        if (rule == null || rule.getId() == null) {
            throw new IllegalArgumentException("Rule or Rule id cannot be null");
        }
        if (!rules.containsKey(rule.getId())) {
            throw new IllegalArgumentException("Rule not found: " + rule.getId());
        }
        rules.put(rule.getId(), rule);
    }

    @Override
    public void delete(String ruleId) {
        rules.remove(ruleId);
    }

    @Override
    public void deleteByOrganizationId(String organizationId) {
        List<String> keysToRemove = rules.keySet().stream()
                .filter(key -> rules.get(key).getOrganizationId().equals(organizationId))
                .collect(Collectors.toList());
        keysToRemove.forEach(rules::remove);
    }
}

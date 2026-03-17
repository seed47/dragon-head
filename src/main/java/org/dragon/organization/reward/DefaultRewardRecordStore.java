package org.dragon.organization.reward;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * DefaultRewardRecordStore 默认奖惩记录存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class DefaultRewardRecordStore implements RewardRecordStore {

    private final Map<String, RewardRecord> records = new ConcurrentHashMap<>();

    @Override
    public void save(RewardRecord record) {
        if (record == null || record.getId() == null) {
            throw new IllegalArgumentException("Record or Record id cannot be null");
        }
        records.put(record.getId(), record);
    }

    @Override
    public Optional<RewardRecord> findById(String recordId) {
        return Optional.ofNullable(records.get(recordId));
    }

    @Override
    public List<RewardRecord> findByOrganizationId(String organizationId, int limit) {
        return records.values().stream()
                .filter(r -> r.getOrganizationId().equals(organizationId))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<RewardRecord> findByOrganizationIdAndTargetId(
            String organizationId, String targetId, int limit) {
        return records.values().stream()
                .filter(r -> r.getOrganizationId().equals(organizationId)
                        && r.getTargetId().equals(targetId))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<RewardRecord> findByRuleId(String ruleId, int limit) {
        return records.values().stream()
                .filter(r -> r.getRuleId().equals(ruleId))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByOrganizationId(String organizationId) {
        List<String> keysToRemove = records.keySet().stream()
                .filter(key -> records.get(key).getOrganizationId().equals(organizationId))
                .collect(Collectors.toList());
        keysToRemove.forEach(records::remove);
    }
}

package org.dragon.organization.reward;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dragon.organization.member.MemberManagementService;
import org.dragon.organization.member.OrganizationMember;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RewardEngine 奖惩引擎
 * 负责评估规则并执行奖惩动作
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RewardEngine {

    private final RewardRuleStore ruleStore;
    private final RewardRecordStore recordStore;
    private final MemberManagementService memberService;

    /**
     * 触发事件
     */
    public static class TriggerEvent {
        private final String eventType;
        private final Map<String, Object> data;

        public TriggerEvent(String eventType, Map<String, Object> data) {
            this.eventType = eventType;
            this.data = data;
        }

        public String getEventType() {
            return eventType;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public static TriggerEvent taskComplete(Map<String, Object> data) {
            return new TriggerEvent("TASK_COMPLETE", data);
        }

        public static TriggerEvent taskFail(Map<String, Object> data) {
            return new TriggerEvent("TASK_FAIL", data);
        }

        public static TriggerEvent highQualityContribution(Map<String, Object> data) {
            return new TriggerEvent("HIGH_QUALITY_CONTRIBUTION", data);
        }

        public static TriggerEvent collaborationExcellence(Map<String, Object> data) {
            return new TriggerEvent("COLLABORATION_EXCELLENCE", data);
        }

        public static TriggerEvent custom(String eventType, Map<String, Object> data) {
            return new TriggerEvent(eventType, data);
        }
    }

    /**
     * 评估并应用奖惩规则
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param event 触发事件
     */
    public void evaluateAndApply(String organizationId, String characterId, TriggerEvent event) {
        // 获取该组织下对应触发类型的规则
        List<RewardRule> rules = ruleStore.findByOrganizationIdAndTriggerType(
                organizationId, mapEventToTriggerType(event.getEventType()));

        // 按优先级排序
        rules.sort((a, b) -> b.getPriority() - a.getPriority());

        for (RewardRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

            if (evaluateConditions(rule, event)) {
                applyRule(organizationId, characterId, rule, event);
                break; // 只应用匹配的第一个规则
            }
        }
    }

    /**
     * 应用单个奖惩动作
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param action 动作类型
     * @param value 动作值
     */
    public void applyReward(String organizationId, String characterId,
            RewardRule.Action action, Object value) {
        OrganizationMember member = memberService.getMember(organizationId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        Map<String, Object> previousValues = new HashMap<>();
        Map<String, Object> newValues = new HashMap<>();

        switch (action) {
            case WEIGHT_ADJUST -> {
                double oldWeight = member.getWeight();
                double adjust = ((Number) value).doubleValue();
                member.setWeight(Math.max(0.1, Math.min(10.0, oldWeight + adjust)));
                previousValues.put("weight", oldWeight);
                newValues.put("weight", member.getWeight());
            }
            case PRIORITY_ADJUST -> {
                int oldPriority = member.getPriority();
                int adjust = ((Number) value).intValue();
                member.setPriority(oldPriority + adjust);
                previousValues.put("priority", oldPriority);
                newValues.put("priority", member.getPriority());
            }
            case REPUTATION_ADJUST -> {
                int oldReputation = member.getReputation();
                int adjust = ((Number) value).intValue();
                member.setReputation(Math.max(0, oldReputation + adjust));
                previousValues.put("reputation", oldReputation);
                newValues.put("reputation", member.getReputation());
            }
            default -> log.warn("Unsupported action: {}", action);
        }

        // 记录审计日志
        TriggerEvent event = new TriggerEvent("MANUAL_ADJUST", new HashMap<>());
        saveRewardRecord(organizationId, null, characterId, event, List.of(action), previousValues, newValues);
    }

    /**
     * 创建规则
     *
     * @param rule 规则
     * @return 创建的规则（含 ID）
     */
    public RewardRule createRule(RewardRule rule) {
        if (rule.getId() == null || rule.getId().isEmpty()) {
            rule.setId(UUID.randomUUID().toString());
        }
        ruleStore.save(rule);
        log.info("[RewardEngine] Created rule: {} in organization: {}",
                rule.getId(), rule.getOrganizationId());
        return rule;
    }

    /**
     * 更新规则
     *
     * @param rule 规则
     */
    public void updateRule(RewardRule rule) {
        ruleStore.update(rule);
        log.info("[RewardEngine] Updated rule: {}", rule.getId());
    }

    /**
     * 删除规则
     *
     * @param ruleId 规则 ID
     */
    public void deleteRule(String ruleId) {
        ruleStore.delete(ruleId);
        log.info("[RewardEngine] Deleted rule: {}", ruleId);
    }

    /**
     * 获取组织规则列表
     *
     * @param organizationId 组织 ID
     * @return 规则列表
     */
    public List<RewardRule> listRules(String organizationId) {
        return ruleStore.findByOrganizationId(organizationId);
    }

    /**
     * 获取奖惩记录
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param limit 限制数量
     * @return 记录列表
     */
    public List<RewardRecord> getRecords(String organizationId, String characterId, int limit) {
        return recordStore.findByOrganizationIdAndTargetId(organizationId, characterId, limit);
    }

    /**
     * 评估条件是否满足
     */
    private boolean evaluateConditions(RewardRule rule, TriggerEvent event) {
        List<RewardRule.RewardCondition> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return true; // 无条件则默认满足
        }

        for (RewardRule.RewardCondition condition : conditions) {
            if (!evaluateCondition(condition, event)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 评估单个条件
     */
    private boolean evaluateCondition(RewardRule.RewardCondition condition, TriggerEvent event) {
        Map<String, Object> data = event.getData();

        // 检查连续成功次数
        if (condition.getConsecutiveSuccessCount() != null) {
            Integer consecutive = (Integer) data.get("consecutiveSuccessCount");
            if (consecutive == null || consecutive < condition.getConsecutiveSuccessCount()) {
                return false;
            }
        }

        // 检查任务复杂度
        if (condition.getTaskComplexity() != null) {
            Integer complexity = (Integer) data.get("taskComplexity");
            if (complexity == null || complexity < condition.getTaskComplexity()) {
                return false;
            }
        }

        // 检查质量分数
        if (condition.getQualityScoreThreshold() != null) {
            Double qualityScore = (Double) data.get("qualityScore");
            if (qualityScore == null || qualityScore < condition.getQualityScoreThreshold()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 应用规则
     */
    private void applyRule(String organizationId, String characterId,
            RewardRule rule, TriggerEvent event) {
        Map<String, Object> previousValues = new HashMap<>();
        Map<String, Object> newValues = new HashMap<>();

        OrganizationMember member = memberService.getMember(organizationId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        for (RewardRule.Action action : rule.getActions()) {
            Object value = rule.getActionValues() != null
                    ? rule.getActionValues().get(action.name())
                    : null;

            switch (action) {
                case WEIGHT_ADJUST -> {
                    double oldWeight = member.getWeight();
                    double adjust = value != null ? ((Number) value).doubleValue() : 0.1;
                    member.setWeight(Math.max(0.1, Math.min(10.0, oldWeight + adjust)));
                    previousValues.put("weight", oldWeight);
                    newValues.put("weight", member.getWeight());
                }
                case PRIORITY_ADJUST -> {
                    int oldPriority = member.getPriority();
                    int adjust = value != null ? ((Number) value).intValue() : 1;
                    member.setPriority(oldPriority + adjust);
                    previousValues.put("priority", oldPriority);
                    newValues.put("priority", member.getPriority());
                }
                case REPUTATION_ADJUST -> {
                    int oldReputation = member.getReputation();
                    int adjust = value != null ? ((Number) value).intValue() : 10;
                    member.setReputation(Math.max(0, oldReputation + adjust));
                    previousValues.put("reputation", oldReputation);
                    newValues.put("reputation", member.getReputation());
                }
                case ROLE_PROMOTE -> {
                    // 角色晋升逻辑
                    String oldRole = member.getRole();
                    previousValues.put("role", oldRole);
                    // TODO: 实现晋升逻辑
                    newValues.put("role", member.getRole());
                }
                case ROLE_DEMOTE -> {
                    // 角色降级逻辑
                    String oldRole = member.getRole();
                    previousValues.put("role", oldRole);
                    // TODO: 实现降级逻辑
                    newValues.put("role", member.getRole());
                }
                default -> log.warn("Unsupported action: {}", action);
            }
        }

        // 更新成员
        memberService.updateMemberWeight(organizationId, characterId, member.getWeight());
        memberService.updateMemberPriority(organizationId, characterId, member.getPriority());
        memberService.updateMemberReputation(organizationId, characterId,
                (Integer) newValues.getOrDefault("reputation", 0));

        // 保存审计记录
        saveRewardRecord(organizationId, rule.getId(), characterId, event,
                rule.getActions(), previousValues, newValues);

        log.info("[RewardEngine] Applied rule {} to member {} in org {}",
                rule.getId(), characterId, organizationId);
    }

    /**
     * 保存奖惩记录
     */
    private void saveRewardRecord(String organizationId, String ruleId, String characterId,
            TriggerEvent event, List<RewardRule.Action> actions,
            Map<String, Object> previousValues, Map<String, Object> newValues) {
        RewardRecord record = RewardRecord.builder()
                .id(UUID.randomUUID().toString())
                .ruleId(ruleId)
                .organizationId(organizationId)
                .targetType(RewardRecord.TargetType.MEMBER)
                .targetId(characterId)
                .triggerEvent(event.getEventType())
                .actions(actions)
                .previousValues(previousValues)
                .newValues(newValues)
                .appliedBy("system")
                .timestamp(LocalDateTime.now())
                .build();

        recordStore.save(record);
    }

    /**
     * 将事件类型映射到触发类型
     */
    private RewardRule.TriggerType mapEventToTriggerType(String eventType) {
        return switch (eventType) {
            case "TASK_COMPLETE" -> RewardRule.TriggerType.TASK_COMPLETE;
            case "TASK_FAIL" -> RewardRule.TriggerType.TASK_FAIL;
            case "HIGH_QUALITY_CONTRIBUTION" -> RewardRule.TriggerType.HIGH_QUALITY_CONTRIBUTION;
            case "COLLABORATION_EXCELLENCE" -> RewardRule.TriggerType.COLLABORATION_EXCELLENCE;
            case "REPEAT_FAILURE" -> RewardRule.TriggerType.REPEAT_FAILURE;
            default -> RewardRule.TriggerType.CUSTOM;
        };
    }
}

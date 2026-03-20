package org.dragon.observer.optimization;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.mind.Mind;
import org.dragon.character.mind.PersonalityDescriptor;
import org.dragon.character.mind.tag.Tag;
import org.dragon.observer.commons.CommonSenseValidator;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.EvaluationRecordStore;
import org.dragon.observer.log.ModificationLog;
import org.dragon.observer.log.ModificationLogStore;
import org.dragon.organization.Organization;
import org.dragon.organization.OrganizationEnhancer;
import org.dragon.organization.OrganizationRegistry;
import org.dragon.organization.personality.OrganizationPersonality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * OptimizationExecutor 优化执行器
 * 负责执行优化动作，更新 Character Mind 或 Organization Personality
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class OptimizationExecutor {

    private static final Logger log = LoggerFactory.getLogger(OptimizationExecutor.class);

    private final OptimizationActionStore optimizationActionStore;
    private final ModificationLogStore modificationLogStore;
    private final CommonSenseValidator commonSenseValidator;
    private final EvaluationRecordStore evaluationRecordStore;
    private final CharacterRegistry characterRegistry;
    private final OrganizationRegistry organizationRegistry;
    private final LLMSuggestionGenerator suggestionGenerator;
    private final OrganizationEnhancer organizationEnhancer;

    /**
     * 执行优化动作
     *
     * @param actionId 优化动作 ID
     * @return 执行结果
     */
    public OptimizationAction execute(String actionId) {
        OptimizationAction action = optimizationActionStore.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Optimization action not found: " + actionId));

        return execute(action);
    }

    /**
     * 执行优化动作
     *
     * @param action 优化动作
     * @return 执行后的动作
     */
    public OptimizationAction execute(OptimizationAction action) {
        if (!action.canExecute()) {
            log.warn("[OptimizationExecutor] Action cannot execute: {}", action.getId());
            action.markRejected("Action is not in PENDING state");
            optimizationActionStore.save(action);
            return action;
        }

        // 常识校验
        var validationResult = commonSenseValidator.validate(
                action.getTargetType().name(),
                action.getActionType().name(),
                action.getParameters());

        if (!validationResult.isValid()) {
            String reason = "Violated common sense: " + validationResult.getViolations();
            log.warn("[OptimizationExecutor] Action rejected by common sense: {}", reason);
            action.markRejected(reason);
            optimizationActionStore.save(action);
            return action;
        }

        try {
            // 保存修改前的快照
            String beforeSnapshot = captureSnapshot(action.getTargetType(), action.getTargetId());

            // 执行修改
            applyModification(action);

            // 保存修改后的快照
            String afterSnapshot = captureSnapshot(action.getTargetType(), action.getTargetId());

            // 记录修改日志
            ModificationLog modLog = ModificationLog.builder()
                    .id(UUID.randomUUID().toString())
                    .targetType(ModificationLog.TargetType.valueOf(action.getTargetType().name()))
                    .targetId(action.getTargetId())
                    .beforeSnapshot(beforeSnapshot)
                    .afterSnapshot(afterSnapshot)
                    .triggerSource(ModificationLog.TriggerSource.OBSERVER)
                    .evaluationId(action.getEvaluationId())
                    .reason(generateReason(action))
                    .operator("OBSERVER")
                    .timestamp(LocalDateTime.now())
                    .build();
            modificationLogStore.save(modLog);

            // 标记为已执行
            action.markExecuted("Successfully executed");
            optimizationActionStore.save(action);

            log.info("[OptimizationExecutor] Action executed successfully: {}", action.getId());

        } catch (Exception e) {
            log.error("[OptimizationExecutor] Action execution failed: {}", action.getId(), e);
            action.setStatus(OptimizationAction.Status.FAILED);
            action.setResult("Execution failed: " + e.getMessage());
            optimizationActionStore.save(action);
        }

        return action;
    }

    /**
     * 批量执行优化动作
     *
     * @param actionIds 优化动作 ID 列表
     * @return 执行结果列表
     */
    public List<OptimizationAction> executeBatch(List<String> actionIds) {
        List<OptimizationAction> results = new ArrayList<>();
        for (String actionId : actionIds) {
            results.add(execute(actionId));
        }
        return results;
    }

    /**
     * 回滚优化动作
     *
     * @param actionId 优化动作 ID
     * @return 回滚后的动作
     */
    public OptimizationAction rollback(String actionId) {
        OptimizationAction action = optimizationActionStore.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Optimization action not found: " + actionId));

        if (!action.canRollback()) {
            log.warn("[OptimizationExecutor] Action cannot rollback: {}", action.getId());
            return action;
        }

        try {
            // 获取修改前的快照并恢复
            if (action.getBeforeSnapshot() != null) {
                restoreFromSnapshot(action.getTargetType(), action.getTargetId(), action.getBeforeSnapshot());

                // 记录回滚日志
                ModificationLog rollbackLog = ModificationLog.builder()
                        .id(UUID.randomUUID().toString())
                        .targetType(ModificationLog.TargetType.valueOf(action.getTargetType().name()))
                        .targetId(action.getTargetId())
                        .beforeSnapshot(action.getAfterSnapshot())
                        .afterSnapshot(action.getBeforeSnapshot())
                        .triggerSource(ModificationLog.TriggerSource.OBSERVER)
                        .evaluationId(action.getEvaluationId())
                        .reason("Rollback of action: " + action.getId())
                        .operator("OBSERVER")
                        .timestamp(LocalDateTime.now())
                        .build();
                modificationLogStore.save(rollbackLog);
            }

            action.markRolledBack();
            optimizationActionStore.save(action);

            log.info("[OptimizationExecutor] Action rolled back successfully: {}", action.getId());

        } catch (Exception e) {
            log.error("[OptimizationExecutor] Action rollback failed: {}", action.getId(), e);
            action.setResult("Rollback failed: " + e.getMessage());
            optimizationActionStore.save(action);
        }

        return action;
    }

    /**
     * 根据评价生成优化动作
     *
     * @param evaluationId 评价 ID
     * @return 生成的优化动作列表
     */
    public List<OptimizationAction> generateActionsFromEvaluation(String evaluationId) {
        List<OptimizationAction> actions = new ArrayList<>();

        EvaluationRecord evaluation = evaluationRecordStore().findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found: " + evaluationId));

        // 根据评分和优化建议生成动作
        if (evaluation.getSuggestions() != null) {
            for (String suggestion : evaluation.getSuggestions()) {
                OptimizationAction action = generateAction(evaluation, suggestion);
                if (action != null) {
                    actions.add(action);
                    optimizationActionStore.save(action);
                }
            }
        }

        return actions;
    }

    private EvaluationRecordStore evaluationRecordStore() {
        // 通过 Spring 获取
        return null; // TODO: 注入
    }

    /**
     * 从建议生成优化动作
     */
    private OptimizationAction generateAction(EvaluationRecord evaluation, String suggestion) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("suggestion", suggestion);

        OptimizationAction.ActionType actionType;
        if (suggestion.contains("任务执行")) {
            actionType = OptimizationAction.ActionType.UPDATE_MIND;
        } else if (suggestion.contains("效率")) {
            actionType = OptimizationAction.ActionType.UPDATE_CONFIG;
        } else if (suggestion.contains("技能")) {
            actionType = OptimizationAction.ActionType.ADD_SKILL;
        } else {
            actionType = OptimizationAction.ActionType.UPDATE_PERSONALITY;
        }

        return OptimizationAction.builder()
                .id(UUID.randomUUID().toString())
                .evaluationId(evaluation.getId())
                .targetType(evaluation.getTargetType() == EvaluationRecord.TargetType.CHARACTER
                        ? OptimizationAction.TargetType.CHARACTER
                        : OptimizationAction.TargetType.ORGANIZATION)
                .targetId(evaluation.getTargetId())
                .actionType(actionType)
                .parameters(params)
                .priority(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 应用修改
     */
    private void applyModification(OptimizationAction action) {
        switch (action.getTargetType()) {
            case CHARACTER:
                applyCharacterModification(action);
                break;
            case ORGANIZATION:
                applyOrganizationModification(action);
                break;
        }
    }

    /**
     * 使用 LLM 生成的建议执行优化
     * 这是 Observer 优化功能的核心方法，通过 LLM 分析任务执行数据，
     * 生成优化建议，然后应用到 Character 或 Organization
     *
     * @param action 优化动作
     * @return 执行后的动作
     */
    public OptimizationAction executeWithLLM(OptimizationAction action) {
        if (!action.canExecute()) {
            log.warn("[OptimizationExecutor] Action cannot execute: {}", action.getId());
            action.markRejected("Action is not in PENDING state");
            optimizationActionStore.save(action);
            return action;
        }

        try {
            // 1. 通过 LLM 生成优化建议
            // TODO: 需要从 action 或 context 中获取 workspace 和 organizationId
            String workspace = (String) action.getParameters().get("workspace");
            String organizationId = (String) action.getParameters().get("organizationId");

            List<String> suggestions = suggestionGenerator.generateSuggestions(
                    workspace,
                    organizationId,
                    action.getTargetType(),
                    action.getTargetId(),
                    10); // 考虑最近10个任务

            if (suggestions.isEmpty()) {
                log.info("[OptimizationExecutor] No suggestions generated, skipping optimization");
                action.markExecuted("No suggestions generated");
                optimizationActionStore.save(action);
                return action;
            }

            // 2. 保存修改前的快照
            String beforeSnapshot = captureSnapshot(action.getTargetType(), action.getTargetId());

            // 3. 根据目标类型应用 LLM 增强
            switch (action.getTargetType()) {
                case CHARACTER:
                    applyCharacterModificationWithLLM(action.getTargetId(), suggestions);
                    break;
                case ORGANIZATION:
                    applyOrganizationModificationWithLLM(action.getTargetId(), suggestions);
                    break;
            }

            // 4. 保存修改后的快照
            String afterSnapshot = captureSnapshot(action.getTargetType(), action.getTargetId());

            // 5. 记录修改日志
            ModificationLog modLog = ModificationLog.builder()
                    .id(UUID.randomUUID().toString())
                    .targetType(ModificationLog.TargetType.valueOf(action.getTargetType().name()))
                    .targetId(action.getTargetId())
                    .beforeSnapshot(beforeSnapshot)
                    .afterSnapshot(afterSnapshot)
                    .triggerSource(ModificationLog.TriggerSource.OBSERVER)
                    .evaluationId(action.getEvaluationId())
                    .reason("LLM-driven optimization with suggestions: " + suggestions)
                    .operator("OBSERVER_LLM")
                    .timestamp(LocalDateTime.now())
                    .build();
            modificationLogStore.save(modLog);

            // 6. 标记为已执行
            action.markExecuted("LLM-driven optimization executed with " + suggestions.size() + " suggestions");
            action.getParameters().put("suggestions", suggestions);
            optimizationActionStore.save(action);

            log.info("[OptimizationExecutor] LLM-driven optimization executed successfully: {} suggestions", suggestions.size());

        } catch (Exception e) {
            log.error("[OptimizationExecutor] LLM-driven optimization failed: {}", action.getId(), e);
            action.setStatus(OptimizationAction.Status.FAILED);
            action.setResult("LLM optimization failed: " + e.getMessage());
            optimizationActionStore.save(action);
        }

        return action;
    }

    /**
     * 使用 LLM 建议修改 Character
     */
    private void applyCharacterModificationWithLLM(String characterId, List<String> suggestions) {
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        Mind mind = character.getMind();
        if (mind == null) {
            throw new IllegalStateException("Character mind is not initialized");
        }

        // 1. 通过 LLM 增强 personality
        PersonalityDescriptor updatedDescriptor = mind.enhanceByLLM(suggestions);
        if (updatedDescriptor != null) {
            mind.updatePersonality(updatedDescriptor);
            log.info("[OptimizationExecutor] Updated Character personality via LLM: {}", characterId);
        }

        // 2. 通过 LLM 增强 tags
        Map<String, Tag> tagUpdates = mind.enhanceTagsByLLM(suggestions);
        if (tagUpdates != null && !tagUpdates.isEmpty()) {
            // 应用 tag 更新
            for (Map.Entry<String, Tag> entry : tagUpdates.entrySet()) {
                String targetCharacterId = entry.getKey();
                Tag tag = entry.getValue();
                if (mind.getTagRepository() != null) {
                    mind.getTagRepository().addTag(targetCharacterId, tag);
                }
            }
            log.info("[OptimizationExecutor] Updated {} tags via LLM for Character: {}", tagUpdates.size(), characterId);
        }

        characterRegistry.update(character);
    }

    /**
     * 使用 LLM 建议修改 Organization
     */
    private void applyOrganizationModificationWithLLM(String orgId, List<String> suggestions) {
        Organization organization = organizationRegistry.get(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + orgId));

        // 通过 OrganizationEnhancer 增强 personality
        OrganizationPersonality updatedPersonality = organizationEnhancer.enhanceByLLM(organization, suggestions);
        if (updatedPersonality != null) {
            organization.setPersonality(updatedPersonality);
            log.info("[OptimizationExecutor] Updated Organization personality via LLM: {}", orgId);
        }

        organizationRegistry.update(organization);
    }

    /**
     * 应用 Character 修改
     */
    private void applyCharacterModification(OptimizationAction action) {
        Character character = characterRegistry.get(action.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + action.getTargetId()));

        Mind mind = character.getMind();
        if (mind == null) {
            throw new IllegalStateException("Character mind is not initialized");
        }

        switch (action.getActionType()) {
            case UPDATE_MIND:
            case UPDATE_PERSONALITY:
                // 更新 personality
                if (action.getParameters().containsKey("personality")) {
                    PersonalityDescriptor descriptor = (PersonalityDescriptor) action.getParameters().get("personality");
                    mind.updatePersonality(descriptor);
                }
                break;
            case UPDATE_TAG:
                // 更新标签
                if (action.getParameters().containsKey("tags")) {
                    // TODO: 实现标签更新
                }
                break;
            case ADD_SKILL:
                // 添加技能
                if (action.getParameters().containsKey("skill")) {
                    // TODO: 实现技能添加
                }
                break;
            default:
                log.warn("[OptimizationExecutor] Unsupported action type for Character: {}", action.getActionType());
        }

        characterRegistry.update(character);
    }

    /**
     * 应用 Organization 修改
     */
    private void applyOrganizationModification(OptimizationAction action) {
        Organization organization = organizationRegistry.get(action.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + action.getTargetId()));

        OrganizationPersonality personality = organization.getPersonality();
        if (personality == null) {
            personality = OrganizationPersonality.builder().build();
        }

        switch (action.getActionType()) {
            case UPDATE_PERSONALITY:
                // 更新 personality
                if (action.getParameters().containsKey("workingStyle")) {
                    personality.setWorkingStyle(
                            OrganizationPersonality.WorkingStyle.valueOf(
                                    action.getParameters().get("workingStyle").toString()));
                }
                if (action.getParameters().containsKey("riskTolerance")) {
                    personality.setRiskTolerance(
                            Double.parseDouble(action.getParameters().get("riskTolerance").toString()));
                }
                break;
            case ADJUST_WEIGHT:
                // 调整权重
                if (action.getParameters().containsKey("defaultWeight")) {
                    organization.setDefaultWeight(
                            Double.parseDouble(action.getParameters().get("defaultWeight").toString()));
                }
                break;
            default:
                log.warn("[OptimizationExecutor] Unsupported action type for Organization: {}", action.getActionType());
        }

        organization.setPersonality(personality);
        organizationRegistry.update(organization);
    }

    /**
     * 捕获快照
     */
    private String captureSnapshot(OptimizationAction.TargetType targetType, String targetId) {
        switch (targetType) {
            case CHARACTER:
                return captureCharacterSnapshot(targetId);
            case ORGANIZATION:
                return captureOrganizationSnapshot(targetId);
            default:
                return "{}";
        }
    }

    private String captureCharacterSnapshot(String characterId) {
        return characterRegistry.get(characterId)
                .map(c -> {
                    Mind mind = c.getMind();
                    if (mind != null && mind.getPersonality() != null) {
                        return mind.getPersonality().toString();
                    }
                    return "{}";
                })
                .orElse("{}");
    }

    private String captureOrganizationSnapshot(String orgId) {
        return organizationRegistry.get(orgId)
                .map(o -> o.getPersonality() != null ? o.getPersonality().toString() : "{}")
                .orElse("{}");
    }

    /**
     * 从快照恢复
     */
    private void restoreFromSnapshot(OptimizationAction.TargetType targetType, String targetId, String snapshot) {
        // 简化实现：直接重新执行反向操作
        // 实际实现需要解析快照并恢复
        log.info("[OptimizationExecutor] Restoring from snapshot for {}: {}", targetType, targetId);
    }

    /**
     * 生成修改原因
     */
    private String generateReason(OptimizationAction action) {
        return String.format("Optimization triggered by evaluation %s, action type: %s",
                action.getEvaluationId(), action.getActionType());
    }
}

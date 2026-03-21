package org.dragon.workspace.built_ins.character.hr;

import java.util.List;
import java.util.function.Consumer;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.config.PromptKeys;
import org.dragon.config.PromptManager;
import org.dragon.character.Character;
import org.dragon.workspace.member.CharacterDuty;
import org.dragon.workspace.member.CharacterDutyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * HR 雇佣执行器
 * 实现 AUTO 模式的雇佣/解雇逻辑，通过 HR Character 执行决策
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class HrHiringExecutor {

    private static final Logger log = LoggerFactory.getLogger(HrHiringExecutor.class);

    private final HrCharacterFactory hrCharacterFactory;
    private final CharacterDutyStore characterDutyStore;
    private final CharacterCaller characterCaller;
    private final PromptManager promptManager;

    /**
     * 执行自动模式雇佣（指定 Character）
     * 调用 HR Character 进行决策，然后执行雇佣操作
     *
     * @param workspaceId Workspace ID
     * @param characterId 要雇佣的 Character ID
     * @param availableCharacters 可用的 Character 列表
     * @param hireAction 执行雇佣的操作接口，参数为被雇佣的 Character
     */
    public void hireAuto(String workspaceId, String characterId, List<Character> availableCharacters,
            Consumer<Character> hireAction) {
        // 1. 获取 HR Character
        Character hrCharacter = hrCharacterFactory.getOrCreateHrCharacter(workspaceId);

        Character characterToHire;
        String hrDecision;

        if (characterId != null && !characterId.isEmpty()) {
            // 指定了 Character ID，直接使用
            characterToHire = availableCharacters.stream()
                    .filter(c -> c.getId().equals(characterId))
                    .findFirst()
                    .orElse(null);

            if (characterToHire == null) {
                log.warn("[HrHiringExecutor] Character {} not found in available characters", characterId);
                return;
            }

            // 构建决策提示词
            String decisionPrompt = buildHirePrompt(workspaceId, characterToHire);

            // 调用 HR Character 进行决策
            log.info("[HrHiringExecutor] HR Character {} evaluating hire for {} in workspace {}",
                    hrCharacter.getId(), characterToHire.getId(), workspaceId);

            hrDecision = characterCaller.call(hrCharacter, decisionPrompt, "APPROVE");
            log.info("[HrHiringExecutor] HR decision: {}", hrDecision);
        } else {
            // 未指定 Character ID，让 HR Character 从候选列表中选择
            characterToHire = selectCharacterByHr(workspaceId, hrCharacter, availableCharacters);

            if (characterToHire == null) {
                log.warn("[HrHiringExecutor] HR Character did not select any character");
                return;
            }

            hrDecision = "APPROVE"; // HR 已选择，默认批准
        }

        // 如果 HR 批准，执行雇佣
        if (isApproved(hrDecision)) {
            hireAction.accept(characterToHire);

            // 自动生成 Character Duty
            generateCharacterDutyAuto(workspaceId, characterToHire);
        } else {
            log.info("[HrHiringExecutor] HR denied hiring character {} in workspace {}",
                    characterToHire.getId(), workspaceId);
        }
    }

    /**
     * 让 HR Character 从候选列表中选择 Character
     */
    private Character selectCharacterByHr(String workspaceId, Character hrCharacter, List<Character> availableCharacters) {
        if (availableCharacters == null || availableCharacters.isEmpty()) {
            return null;
        }

        String selectionPrompt = promptManager.getWorkspacePrompt(workspaceId, PromptKeys.HR_HIRE_SELECT);
        if (selectionPrompt == null) {
            throw new IllegalStateException("Prompt not configured: " + PromptKeys.HR_HIRE_SELECT);
        }

        log.info("[HrHiringExecutor] Asking HR Character to select from {} candidates", availableCharacters.size());

        // 使用 CharacterCaller 调用选择（通过 ID 匹配）
        return characterCaller.callForSelectionCharacter(hrCharacter, selectionPrompt, availableCharacters);
    }

    /**
     * 执行自动模式雇佣（旧方法，保留兼容）
     * @deprecated 使用 hireAuto(String, String, List, Consumer) 代替
     */
    @Deprecated
    public void hireAuto(String workspaceId, Character character, Runnable hireAction) {
        // 1. 获取 HR Character
        Character hrCharacter = hrCharacterFactory.getOrCreateHrCharacter(workspaceId);

        // 2. 构建决策提示词
        String decisionPrompt = buildHirePrompt(workspaceId, character);

        // 3. 调用 HR Character 进行决策
        log.info("[HrHiringExecutor] HR Character {} evaluating hire for {} in workspace {}",
                hrCharacter.getId(), character.getId(), workspaceId);

        String hrDecision;
        try {
            hrDecision = hrCharacter.run(decisionPrompt);
            log.info("[HrHiringExecutor] HR decision: {}", hrDecision);
        } catch (Exception e) {
            log.warn("[HrHiringExecutor] HR Character execution failed, using default decision: {}", e.getMessage());
            hrDecision = "APPROVE"; // 默认批准
        }

        // 4. 如果 HR 批准，执行雇佣
        if (isApproved(hrDecision)) {
            hireAction.run();

            // 5. 自动生成 Character Duty
            generateCharacterDutyAuto(workspaceId, character);
        } else {
            log.info("[HrHiringExecutor] HR denied hiring character {} in workspace {}",
                    character.getId(), workspaceId);
        }
    }

    /**
     * 执行自动模式解雇
     * 调用 HR Character 进行决策，然后执行解雇操作
     *
     * @param workspaceId Workspace ID
     * @param characterId 要解雇的 Character ID
     * @param fireAction 执行解雇的操作接口
     */
    public void fireAuto(String workspaceId, String characterId, Runnable fireAction) {
        // 1. 获取 HR Character
        Character hrCharacter = hrCharacterFactory.getOrCreateHrCharacter(workspaceId);

        // 2. 构建决策提示词
        String decisionPrompt = buildFirePrompt(workspaceId, characterId);

        // 3. 调用 HR Character 进行决策
        log.info("[HrHiringExecutor] HR Character {} evaluating fire for {} in workspace {}",
                hrCharacter.getId(), characterId, workspaceId);

        String hrDecision = characterCaller.call(hrCharacter, decisionPrompt, "APPROVE");
        log.info("[HrHiringExecutor] HR decision: {}", hrDecision);

        // 4. 如果 HR 批准，执行解雇
        if (isApproved(hrDecision)) {
            fireAction.run();
        } else {
            log.info("[HrHiringExecutor] HR denied firing character {} in workspace {}",
                    characterId, workspaceId);
        }
    }

    /**
     * 构建雇佣决策提示词
     */
    private String buildHirePrompt(String workspaceId, Character character) {
        String promptTemplate = promptManager.getWorkspacePrompt(workspaceId, PromptKeys.HR_HIRE_DECISION);
        if (promptTemplate == null) {
            throw new IllegalStateException("Prompt not configured: " + PromptKeys.HR_HIRE_DECISION);
        }
        return String.format(promptTemplate,
                character.getName(),
                character.getDescription());
    }

    /**
     * 构建解雇决策提示词
     */
    private String buildFirePrompt(String workspaceId, String characterId) {
        String promptTemplate = promptManager.getWorkspacePrompt(workspaceId, PromptKeys.HR_FIRE_DECISION);
        if (promptTemplate == null) {
            throw new IllegalStateException("Prompt not configured: " + PromptKeys.HR_FIRE_DECISION);
        }
        return String.format(promptTemplate, characterId);
    }

    /**
     * 判断 HR 决策是否批准
     */
    private boolean isApproved(String decision) {
        if (decision == null) {
            return false;
        }
        String upperDecision = decision.toUpperCase();
        return upperDecision.contains("APPROVE") || upperDecision.contains("批准");
    }

    /**
     * 自动生成 Character Duty
     */
    private void generateCharacterDutyAuto(String workspaceId, Character character) {
        // 构建职责生成提示词
        String promptTemplate = promptManager.getWorkspacePrompt(workspaceId, PromptKeys.HR_DUTY_GENERATE);
        if (promptTemplate == null) {
            throw new IllegalStateException("Prompt not configured: " + PromptKeys.HR_DUTY_GENERATE);
        }
        String dutyPrompt = String.format(promptTemplate,
                character.getName(),
                character.getDescription());

        // 调用 HR Character 生成职责描述
        Character hrCharacter = hrCharacterFactory.getOrCreateHrCharacter(workspaceId);
        String dutyDescription = characterCaller.call(hrCharacter, dutyPrompt);
        // 清理结果，只保留职责描述部分
        dutyDescription = cleanDutyDescription(dutyDescription);

        // 保存职责描述
        String dutyId = CharacterDuty.createId(workspaceId, character.getId());
        var existing = characterDutyStore.findById(dutyId);

        if (existing.isPresent()) {
            var duty = existing.get();
            duty.setDutyDescription(dutyDescription);
            duty.setAutoGenerated(true);
            duty.setUpdatedAt(java.time.LocalDateTime.now());
            characterDutyStore.update(duty);
        } else {
            var duty = CharacterDuty.builder()
                    .id(dutyId)
                    .workspaceId(workspaceId)
                    .characterId(character.getId())
                    .dutyDescription(dutyDescription)
                    .autoGenerated(true)
                    .createdAt(java.time.LocalDateTime.now())
                    .updatedAt(java.time.LocalDateTime.now())
                    .build();
            characterDutyStore.save(duty);
        }

        log.info("[HrHiringExecutor] Auto-generated duty for character {}: {}",
                character.getId(), dutyDescription);
    }

    /**
     * 清理职责描述结果
     */
    private String cleanDutyDescription(String result) {
        if (result == null) {
            return "";
        }
        // 移除可能的标记和多余空白
        String cleaned = result.trim();
        // 如果包含换行，取第一行
        if (cleaned.contains("\n")) {
            cleaned = cleaned.split("\n")[0].trim();
        }
        return cleaned;
    }

    /**
     * 生成默认职责描述
     */
    private String generateDefaultDuty() {
        return "协助工作空间完成各项任务";
    }

}

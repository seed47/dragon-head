package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.actionlog.WorkspaceActionLog;
import org.dragon.workspace.built_ins.character.hr.HrCharacterFactory;
import org.dragon.workspace.built_ins.character.hr.HrHiringExecutor;
import org.dragon.workspace.hiring.HireMode;
import org.dragon.workspace.member.CharacterDuty;
import org.dragon.workspace.member.CharacterDutyStore;
import org.dragon.workspace.member.WorkspaceMember;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceHiringService 雇佣管理服务
 * 管理 Workspace 内 Character 的雇佣、解雇、职责分配等
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceHiringService {

    private final WorkspaceRegistry workspaceRegistry;
    private final CharacterRegistry characterRegistry;
    private final CharacterDutyStore characterDutyStore;
    private final WorkspaceActionLogService actionLogService;
    private final HrCharacterFactory hrCharacterFactory;
    private final HrHiringExecutor hrHiringExecutor;
    private final WorkspaceMemberManagementService memberManagementService;

    /**
     * 雇佣 Character 到 Workspace
     *
     * @param workspaceId 工作空间 ID
     * @param characterId 要雇佣的 Character ID
     * @param mode 雇佣模式
     */
    public void hire(String workspaceId, String characterId, HireMode mode) {
        hire(workspaceId, characterId, mode, null);
    }

    /**
     * 雇佣 Character 到 Workspace (指定默认 Character 池)
     *
     * @param workspaceId 工作空间 ID
     * @param characterId 要雇佣的 Character ID（AUTO 模式下可为 null，表示自动选择）
     * @param mode 雇佣模式
     * @param defaultCharacterIds 默认 Character 池
     */
    public void hire(String workspaceId, String characterId, HireMode mode, List<String> defaultCharacterIds) {
        // 验证 workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        switch (mode) {
            case DEFAULT:
            case MANUAL:
                // DEFAULT 和 MANUAL 模式需要指定 characterId
                if (characterId == null) {
                    throw new IllegalArgumentException("characterId is required for DEFAULT and MANUAL mode");
                }
                Character character = characterRegistry.get(characterId)
                        .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));
                if (mode == HireMode.DEFAULT) {
                    hireDefault(workspaceId, character, defaultCharacterIds);
                } else {
                    hireManual(workspaceId, character);
                }
                break;
            case AUTO:
                // AUTO 模式支持自动选择 Character
                hireAuto(workspaceId, characterId);
                break;
            default:
                throw new IllegalArgumentException("Unknown hire mode: " + mode);
        }
    }

    /**
     * 从 Workspace 解雇 Character
     *
     * @param workspaceId 工作空间 ID
     * @param characterId 要解雇的 Character ID
     * @param mode 雇佣模式
     */
    public void fire(String workspaceId, String characterId, HireMode mode) {
        // 验证 workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        switch (mode) {
            case DEFAULT:
            case MANUAL:
                fireManual(workspaceId, characterId);
                break;
            case AUTO:
                fireAuto(workspaceId, characterId);
                break;
            default:
                throw new IllegalArgumentException("Unknown hire mode: " + mode);
        }
    }

    /**
     * 获取 Workspace 的 HR Character
     *
     * @param workspaceId Workspace ID
     * @return HR Character (如果存在)
     */
    public Optional<Character> getHrCharacter(String workspaceId) {
        if (hrCharacterFactory.hasHrCharacter(workspaceId)) {
            return Optional.of(hrCharacterFactory.getOrCreateHrCharacter(workspaceId));
        }
        return Optional.empty();
    }

    /**
     * 设置 Character 的职责描述
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param dutyDescription 职责描述
     */
    public void setCharacterDuty(String workspaceId, String characterId, String dutyDescription) {
        // 验证 workspace 和 character 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        String dutyId = CharacterDuty.createId(workspaceId, characterId);
        Optional<CharacterDuty> existing = characterDutyStore.findById(dutyId);

        LocalDateTime now = LocalDateTime.now();
        CharacterDuty duty;
        if (existing.isPresent()) {
            duty = existing.get();
            duty.setDutyDescription(dutyDescription);
            duty.setAutoGenerated(false);
            duty.setUpdatedAt(now);
            characterDutyStore.update(duty);
        } else {
            duty = CharacterDuty.builder()
                    .id(dutyId)
                    .workspaceId(workspaceId)
                    .characterId(characterId)
                    .dutyDescription(dutyDescription)
                    .autoGenerated(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            characterDutyStore.save(duty);
        }

        // 记录动作日志
        actionLogService.logAction(workspaceId, WorkspaceActionLog.ActionType.UPDATE_DUTY, characterId, "user",
                "Updated duty description: " + dutyDescription);

        log.info("[WorkspaceHiringService] Set duty for character {} in workspace {}", characterId, workspaceId);
    }

    /**
     * 获取 Character 的职责描述
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @return Character Duty
     */
    public Optional<CharacterDuty> getCharacterDuty(String workspaceId, String characterId) {
        return characterDutyStore.findByWorkspaceIdAndCharacterId(workspaceId, characterId);
    }

    // ==================== 私有方法：雇佣实现 ====================

    /**
     * 默认模式雇佣
     */
    private void hireDefault(String workspaceId, Character character, List<String> defaultCharacterIds) {
        // 如果指定了默认 Character 池，验证目标是否在池中
        if (defaultCharacterIds != null && !defaultCharacterIds.isEmpty()) {
            if (!defaultCharacterIds.contains(character.getId())) {
                throw new IllegalArgumentException("Character not in default pool: " + character.getId());
            }
        }

        // 添加成员
        memberManagementService.addMember(workspaceId, character.getId(), "MEMBER",
                WorkspaceMember.Layer.NORMAL);

        // 记录动作日志
        actionLogService.logAction(workspaceId, WorkspaceActionLog.ActionType.HIRE, character.getId(), "user",
                "Hired via DEFAULT mode");

        log.info("[WorkspaceHiringService] Hired character {} to workspace {} via DEFAULT mode",
                character.getId(), workspaceId);
    }

    /**
     * 手动模式雇佣
     */
    private void hireManual(String workspaceId, Character character) {
        // 添加成员
        memberManagementService.addMember(workspaceId, character.getId(), "MEMBER",
                WorkspaceMember.Layer.NORMAL);

        // 记录动作日志
        actionLogService.logAction(workspaceId, WorkspaceActionLog.ActionType.HIRE, character.getId(), "user",
                "Hired via MANUAL mode");

        log.info("[WorkspaceHiringService] Hired character {} to workspace {} via MANUAL mode",
                character.getId(), workspaceId);
    }

    /**
     * 自动模式雇佣 (HR Character)
     * @param workspaceId Workspace ID
     * @param characterId 要雇佣的 Character ID，如果为 null 则自动选择
     */
    private void hireAuto(String workspaceId, String characterId) {
        // 获取当前 workspace 的成员，排除已在其中的 Character
        List<String> currentMemberIds = memberManagementService.listMembers(workspaceId)
                .stream()
                .map(m -> m.getCharacterId())
                .toList();

        // 获取所有可用 Character
        List<Character> availableCharacters = characterRegistry.listAll().stream()
                .filter(c -> !currentMemberIds.contains(c.getId()))
                .filter(c -> c.getStatus() == Character.Status.RUNNING)
                .toList();

        if (availableCharacters.isEmpty()) {
            log.warn("[WorkspaceHiringService] No available characters to hire in workspace {}", workspaceId);
            return;
        }

        // 调用 HrHiringExecutor 执行自动雇佣
        hrHiringExecutor.hireAuto(workspaceId, characterId, availableCharacters, (hiredCharacter) -> {
            // 执行实际的雇佣操作
            memberManagementService.addMember(workspaceId, hiredCharacter.getId(), "MEMBER",
                    WorkspaceMember.Layer.NORMAL);
            actionLogService.logAction(workspaceId, WorkspaceActionLog.ActionType.HIRE, hiredCharacter.getId(), "hr",
                    "Hired via AUTO mode with auto-selection");
            log.info("[WorkspaceHiringService] Hired character {} to workspace {} via AUTO mode",
                    hiredCharacter.getId(), workspaceId);
        });
    }

    /**
     * 手动模式解雇
     */
    private void fireManual(String workspaceId, String characterId) {
        // 移除成员
        memberManagementService.removeMember(workspaceId, characterId);

        // 记录动作日志
        actionLogService.logAction(workspaceId, WorkspaceActionLog.ActionType.FIRE, characterId, "user",
                "Fired via MANUAL mode");

        log.info("[WorkspaceHiringService] Fired character {} from workspace {} via MANUAL mode",
                characterId, workspaceId);
    }

    /**
     * 自动模式解雇 (HR Character)
     */
    private void fireAuto(String workspaceId, String characterId) {
        hrHiringExecutor.fireAuto(workspaceId, characterId, () -> {
            // 执行实际的解雇操作
            memberManagementService.removeMember(workspaceId, characterId);
            actionLogService.logAction(workspaceId, WorkspaceActionLog.ActionType.FIRE, characterId, "hr",
                    "Fired via AUTO mode");
        });

        log.info("[WorkspaceHiringService] Fired character {} from workspace {} via AUTO mode",
                characterId, workspaceId);
    }
}

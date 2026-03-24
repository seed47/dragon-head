package org.dragon.workspace.built_ins.character.hr;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.agent.tool.ToolRegistry;
import org.dragon.character.Character;
import org.dragon.character.CharacterFactory;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HR Character 工厂
 * 实现 CharacterFactory 接口，为 Workspace 创建和管理 HR 功能的 Character 实例
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrCharacterFactory implements CharacterFactory<Character> {

    private static final String HR_CHARACTER_PREFIX = "hr_";
    private static final String CHARACTER_TYPE = "hr";

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final ToolRegistry toolRegistry;
    private final CharacterRuntimeBinder characterRuntimeBinder;

    @Override
    public String getCharacterType() {
        return CHARACTER_TYPE;
    }

    @Override
    public Character createCharacter(String workspaceId) {
        String hrCharacterId = getCharacterId(workspaceId);

        // 验证 Workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 创建 HR Character
        Character hrCharacter = Character.builder()
                .id(hrCharacterId)
                .name("HR Manager")
                .description("负责 Workspace 的人力资源管理，包括招聘、解雇、职责分配等")
                .status(Character.Status.RUNNING)
                .workspaceIds(List.of(workspaceId))
                .version(1)
                .extensions(new ConcurrentHashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 绑定运行时依赖
        characterRuntimeBinder.bind(hrCharacter, workspaceId);

        // 注册到 CharacterRegistry
        characterRegistry.register(hrCharacter);

        log.info("[HrCharacterFactory] Created HR character {} for workspace {}", hrCharacterId, workspaceId);

        return hrCharacter;
    }

    @Override
    public Character getOrCreateCharacter(String workspaceId) {
        String hrCharacterId = getCharacterId(workspaceId);

        // 检查是否已存在
        return characterRegistry.get(hrCharacterId)
                .orElseGet(() -> createCharacter(workspaceId));
    }

    @Override
    public boolean hasCharacter(String workspaceId) {
        String hrCharacterId = getCharacterId(workspaceId);
        return characterRegistry.exists(hrCharacterId);
    }

    @Override
    public List<ToolConnector> getAvailableTools() {
        return List.of(
                toolRegistry.get("hire_character").orElse(null),
                toolRegistry.get("fire_character").orElse(null),
                toolRegistry.get("assign_duty").orElse(null),
                toolRegistry.get("list_candidates").orElse(null),
                toolRegistry.get("evaluate_character").orElse(null)
        );
    }

    /**
     * 获取 HR Character ID
     */
    public String getCharacterId(String workspaceId) {
        return HR_CHARACTER_PREFIX + workspaceId;
    }

    /**
     * 获取 Workspace 的 HR Character (兼容旧接口)
     *
     * @param workspaceId Workspace ID
     * @return HR Character 实例
     */
    public Character getOrCreateHrCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    /**
     * 检查 Workspace 是否有 HR Character (兼容旧接口)
     */
    public boolean hasHrCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}

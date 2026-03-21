package org.dragon.workspace.built_ins.character.project_manager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.agent.tool.ToolRegistry;
import org.dragon.character.Character;
import org.dragon.character.CharacterFactory;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ProjectManager Character 工厂
 * 实现 CharacterFactory 接口，为 Workspace 创建和管理项目经理功能的 Character 实例
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectManagerCharacterFactory implements CharacterFactory<Character> {

    private static final String PROJECT_MANAGER_CHARACTER_PREFIX = "project_manager_";
    private static final String CHARACTER_TYPE = "project_manager";

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final ToolRegistry toolRegistry;
    private final ProjectManagerCharacterTools projectManagerCharacterTools;

    @Override
    public String getCharacterType() {
        return CHARACTER_TYPE;
    }

    @Override
    public Character createCharacter(String workspaceId) {
        String characterId = getCharacterId(workspaceId);

        // 验证 Workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 创建 ProjectManager Character
        Character character = Character.builder()
                .id(characterId)
                .name("Project Manager")
                .description("负责将复杂任务拆解为可执行的子任务，并管理任务执行进度")
                .status(Character.Status.RUNNING)
                .workspaceIds(List.of(workspaceId))
                .version(1)
                .extensions(new ConcurrentHashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 注册到 CharacterRegistry
        characterRegistry.register(character);

        log.info("[ProjectManagerCharacterFactory] Created ProjectManager character {} for workspace {}", characterId, workspaceId);

        return character;
    }

    @Override
    public Character getOrCreateCharacter(String workspaceId) {
        String characterId = getCharacterId(workspaceId);

        // 检查是否已存在
        return characterRegistry.get(characterId)
                .orElseGet(() -> createCharacter(workspaceId));
    }

    @Override
    public boolean hasCharacter(String workspaceId) {
        String characterId = getCharacterId(workspaceId);
        return characterRegistry.exists(characterId);
    }

    @Override
    public List<ToolConnector> getAvailableTools() {
        return projectManagerCharacterTools.getAvailableTools();
    }

    /**
     * 获取 ProjectManager Character ID
     *
     * @param workspaceId Workspace ID
     * @return Character ID
     */
    public String getCharacterId(String workspaceId) {
        return PROJECT_MANAGER_CHARACTER_PREFIX + workspaceId;
    }

    /**
     * 获取 Workspace 的 ProjectManager Character
     *
     * @param workspaceId Workspace ID
     * @return ProjectManager Character 实例
     */
    public Character getOrCreateProjectManagerCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    /**
     * 检查 Workspace 是否有 ProjectManager Character
     *
     * @param workspaceId Workspace ID
     * @return 是否存在
     */
    public boolean hasProjectManagerCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}

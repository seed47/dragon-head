package org.dragon.workspace.built_ins.character.member_selector;

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
 * MemberSelector Character 工厂
 * 实现 CharacterFactory 接口，为 Workspace 创建和管理成员选择功能的 Character 实例
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberSelectorCharacterFactory implements CharacterFactory<Character> {

    private static final String MEMBER_SELECTOR_CHARACTER_PREFIX = "member_selector_";
    private static final String CHARACTER_TYPE = "member_selector";

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final ToolRegistry toolRegistry;
    private final MemberSelectorCharacterTools memberSelectorCharacterTools;
    private final CharacterRuntimeBinder characterRuntimeBinder;

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

        // 创建 MemberSelector Character
        Character character = Character.builder()
                .id(characterId)
                .name("Member Selector")
                .description("负责从 Workspace 中已雇佣的 Character 中选择最合适的执行者来完成特定任务")
                .status(Character.Status.RUNNING)
                .workspaceIds(List.of(workspaceId))
                .version(1)
                .extensions(new ConcurrentHashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 绑定运行时依赖
        characterRuntimeBinder.bind(character, workspaceId);

        // 注册到 CharacterRegistry
        characterRegistry.register(character);

        log.info("[MemberSelectorCharacterFactory] Created MemberSelector character {} for workspace {}", characterId, workspaceId);

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
        return memberSelectorCharacterTools.getAvailableTools();
    }

    /**
     * 获取 MemberSelector Character ID
     *
     * @param workspaceId Workspace ID
     * @return Character ID
     */
    public String getCharacterId(String workspaceId) {
        return MEMBER_SELECTOR_CHARACTER_PREFIX + workspaceId;
    }

    /**
     * 获取 Workspace 的 MemberSelector Character
     *
     * @param workspaceId Workspace ID
     * @return MemberSelector Character 实例
     */
    public Character getOrCreateMemberSelectorCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    /**
     * 检查 Workspace 是否有 MemberSelector Character
     *
     * @param workspaceId Workspace ID
     * @return 是否存在
     */
    public boolean hasMemberSelectorCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}

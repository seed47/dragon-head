package org.dragon.workspace.built_ins.character.commonsense_writer;

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
 * CommonSenseWriter Character 工厂
 * 实现 CharacterFactory 接口，为 Workspace 创建和管理 CommonSense 编写功能的 Character 实例
 * CommonSenseWriter 负责将 CommonSense 转换成 prompt
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonSenseWriterCharacterFactory implements CharacterFactory<Character> {

    private static final String COMMON_SENSE_WRITER_CHARACTER_PREFIX = "commonsense_writer_";
    private static final String CHARACTER_TYPE = "commonsense_writer";

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final ToolRegistry toolRegistry;
    private final CommonSenseWriterCharacterTools commonSenseWriterCharacterTools;

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

        // 创建 CommonSenseWriter Character
        Character character = Character.builder()
                .id(characterId)
                .name("Common Sense Writer")
                .description("负责将 CommonSense 常识转换成 prompt 格式")
                .status(Character.Status.RUNNING)
                .workspaceIds(List.of(workspaceId))
                .version(1)
                .extensions(new ConcurrentHashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 注册到 CharacterRegistry
        characterRegistry.register(character);

        log.info("[CommonSenseWriterCharacterFactory] Created CommonSenseWriter character {} for workspace {}", characterId, workspaceId);

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
        return commonSenseWriterCharacterTools.getAvailableTools();
    }

    /**
     * 获取 CommonSenseWriter Character ID
     *
     * @param workspaceId Workspace ID
     * @return Character ID
     */
    public String getCharacterId(String workspaceId) {
        return COMMON_SENSE_WRITER_CHARACTER_PREFIX + workspaceId;
    }

    /**
     * 获取 Workspace 的 CommonSenseWriter Character
     *
     * @param workspaceId Workspace ID
     * @return CommonSenseWriter Character 实例
     */
    public Character getOrCreateCommonSenseWriterCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    /**
     * 检查 Workspace 是否有 CommonSenseWriter Character
     *
     * @param workspaceId Workspace ID
     * @return 是否存在
     */
    public boolean hasCommonSenseWriterCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}

package org.dragon.workspace.built_ins.character.prompt_writer;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.agent.tool.ToolRegistry;
import org.dragon.character.Character;
import org.dragon.character.CharacterFactory;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.config.PromptManager;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PromptWriter Character 工厂
 * 实现 CharacterFactory 接口，为 Workspace 创建和管理 Prompt 编写功能的 Character 实例
 * PromptWriter 负责将 PromptManager 返回的模板与动态数据拼接成完整的 prompt
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptWriterCharacterFactory implements CharacterFactory<Character> {

    private static final String PROMPT_WRITER_CHARACTER_PREFIX = "prompt_writer_";
    private static final String CHARACTER_TYPE = "prompt_writer";

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final ToolRegistry toolRegistry;
    private final PromptWriterCharacterTools promptWriterCharacterTools;
    private final PromptManager promptManager;
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

        // 获取可用工具列表并提取工具名称
        List<ToolConnector> availableTools = getAvailableTools();
        Set<String> allowedToolNames = new HashSet<>();
        for (ToolConnector tool : availableTools) {
            if (tool != null && tool.getName() != null) {
                allowedToolNames.add(tool.getName());
            }
        }

        // 创建 PromptWriter Character
        Character character = Character.builder()
                .id(characterId)
                .name("Prompt Writer")
                .description("负责将 prompt 模板与动态数据拼接成完整的 prompt")
                .status(Character.Status.RUNNING)
                .workspaceIds(List.of(workspaceId))
                .version(1)
                .extensions(new ConcurrentHashMap<>())
                .allowedTools(allowedToolNames)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 绑定运行时依赖
        characterRuntimeBinder.bind(character, workspaceId);

        // 注册到 CharacterRegistry
        characterRegistry.register(character);

        // // 写入默认 system prompt（如果不存在）
        // String existingPrompt = promptManager.getPrompt(workspaceId, characterId, PromptKeys.CHARACTER_SYSTEM);
        // if (existingPrompt == null || existingPrompt.isEmpty()) {
        //     promptManager.setCharacterPrompt(workspaceId, characterId, PromptKeys.CHARACTER_SYSTEM,
        //             "你是一个专业的 Prompt Writer，负责将任务信息、成员信息和模板拼接成完整的 prompt。\n" +
        //             "1. 先判断当前任务是否需要工作空间规则、术语、角色边界、协作约束\n" +
        //             "2. 需要时调用 get_workspace_common_sense 工具获取 CommonSense\n" +
        //             "3. 不需要时直接生成完整 prompt\n" +
        //             "4. 最终只返回完整 prompt，不要解释，不要包含 ReAct 思考过程");
        //     log.info("[PromptWriterCharacterFactory] Set default system prompt for character {}", characterId);
        // }

        log.info("[PromptWriterCharacterFactory] Created PromptWriter character {} for workspace {} with tools: {}",
                characterId, workspaceId, allowedToolNames);

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
        return promptWriterCharacterTools.getAvailableTools();
    }

    /**
     * 获取 PromptWriter Character ID
     *
     * @param workspaceId Workspace ID
     * @return Character ID
     */
    public String getCharacterId(String workspaceId) {
        return PROMPT_WRITER_CHARACTER_PREFIX + workspaceId;
    }

    /**
     * 获取 Workspace 的 PromptWriter Character
     *
     * @param workspaceId Workspace ID
     * @return PromptWriter Character 实例
     */
    public Character getOrCreatePromptWriterCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    /**
     * 检查 Workspace 是否有 PromptWriter Character
     *
     * @param workspaceId Workspace ID
     * @return 是否存在
     */
    public boolean hasPromptWriterCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}

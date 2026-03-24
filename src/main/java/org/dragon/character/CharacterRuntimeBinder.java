package org.dragon.character;

import java.util.HashSet;

import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.orchestration.OrchestrationService;
import org.dragon.agent.react.ReActExecutor;
import org.dragon.agent.workflow.WorkflowExecutor;
import org.dragon.config.PromptManager;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Character 运行时依赖绑定器
 * 负责在 Character 创建后注入运行时依赖
 * 包括 PromptManager、ReActExecutor、WorkflowExecutor、ModelRegistry、OrchestrationService
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterRuntimeBinder {

    private final PromptManager promptManager;
    private final ReActExecutor reActExecutor;
    private final WorkflowExecutor workflowExecutor;
    private final ModelRegistry modelRegistry;
    private final OrchestrationService orchestrationService;

    /**
     * 绑定 Character 运行时依赖
     *
     * @param character Character 实例
     * @param workspaceId Workspace ID
     */
    public void bind(Character character, String workspaceId) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }

        // 设置 PromptManager
        character.setPromptManager(promptManager);

        // 设置 ReActExecutor
        character.setReActExecutor(reActExecutor);

        // 设置 WorkflowExecutor
        character.setWorkflowExecutor(workflowExecutor);

        // 设置 ModelRegistry
        character.setModelRegistry(modelRegistry);

        // 设置 OrchestrationService
        character.setOrchestrationService(orchestrationService);

        // 如果没有 allowedTools，初始化为空集合
        if (character.getAllowedTools() == null) {
            character.setAllowedTools(new HashSet<>());
        }

        log.info("[CharacterRuntimeBinder] Bound runtime dependencies for character {} in workspace {}",
                character.getId(), workspaceId);
    }
}

package org.dragon.agent.orchestration;

import java.util.UUID;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 编排服务实现
 * 只负责决策（决定使用哪种执行策略）
 * 返回策略信息，由 Character 执行具体流程
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
public class OrchestrationServiceImpl implements OrchestrationService {

    private final CharacterRegistry characterRegistry;

    public OrchestrationServiceImpl(CharacterRegistry characterRegistry) {
        this.characterRegistry = characterRegistry;
    }

    @Override
    public OrchestrationResult orchestrate(OrchestrationRequest request) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 获取 Character
            Character character = characterRegistry.get(request.getCharacterId())
                    .orElseThrow(() -> new IllegalArgumentException("Character not found: " + request.getCharacterId()));

            // 根据模式决定执行策略
            Mode mode = request.getMode();
            String workflowId = request.getWorkflowId();

            // 如果没有指定模式，由 Character 配置决定
            if (mode == null) {
                mode = decideMode(character);
            }

            // 如果是 WORKFLOW 模式但没有指定 workflowId，从 Character 配置获取
            if (mode == Mode.WORKFLOW && (workflowId == null || workflowId.isEmpty())) {
                workflowId = getDefaultWorkflowId(character);
            }

            log.info("[Orchestration] Decision: mode={}, workflowId={}", mode, workflowId);

            // 返回编排结果，包含执行策略信息（不包含执行结果，执行结果由 Character.run() 返回）
            return new OrchestrationResult.Builder()
                    .executionId(executionId)
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .mode(mode)
                    .workflowId(workflowId)
                    .build();

        } catch (Exception e) {
            log.error("[Orchestration] Orchestration error: {}", executionId, e);
            return new OrchestrationResult.Builder()
                    .executionId(executionId)
                    .success(false)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 决定执行模式
     * 根据 Character 的配置决定使用 WORKFLOW 还是 REACT
     */
    private Mode decideMode(Character character) {
        if (character.getAgentEngineConfig() != null
                && character.getAgentEngineConfig().getWorkflowConfig() != null
                && character.getAgentEngineConfig().getWorkflowConfig().getDefaultWorkflowId() != null) {
            return Mode.WORKFLOW;
        }
        return Mode.REACT;
    }

    /**
     * 获取默认 workflow ID
     */
    private String getDefaultWorkflowId(Character character) {
        if (character.getAgentEngineConfig() != null
                && character.getAgentEngineConfig().getWorkflowConfig() != null) {
            return character.getAgentEngineConfig().getWorkflowConfig().getDefaultWorkflowId();
        }
        return null;
    }
}

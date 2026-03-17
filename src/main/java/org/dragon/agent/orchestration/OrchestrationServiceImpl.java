package org.dragon.agent.orchestration;

import java.util.UUID;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.agent.workflow.WorkflowResult;
import org.dragon.agent.react.ReActResult;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 编排服务实现
 * 只负责编排决策，实际执行由 Character 完成
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

            // 根据模式选择执行方式
            switch (request.getMode()) {
                case WORKFLOW -> {
                    String workflowId = request.getWorkflowId();
                    WorkflowResult result = character.runWorkflow(workflowId);
                    return OrchestrationResult.builder()
                            .executionId(executionId)
                            .success(result.isSuccess())
                            .response(result.getErrorMessage() != null ? result.getErrorMessage() : "Workflow completed")
                            .durationMs(System.currentTimeMillis() - startTime)
                            .build();
                }

                case REACT -> {
                    String message = request.getMessage();
                    ReActResult result = character.runReAct(message);
                    return OrchestrationResult.builder()
                            .executionId(executionId)
                            .success(result.isSuccess())
                            .response(result.getResponse())
                            .durationMs(System.currentTimeMillis() - startTime)
                            .build();
                }

                default -> {
                    return OrchestrationResult.builder()
                            .executionId(executionId)
                            .success(false)
                            .response("Unknown mode: " + request.getMode())
                            .durationMs(System.currentTimeMillis() - startTime)
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("[Orchestration] Execution error: {}", executionId, e);
            return OrchestrationResult.builder()
                    .executionId(executionId)
                    .success(false)
                    .response(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}

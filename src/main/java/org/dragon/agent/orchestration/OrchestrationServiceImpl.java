package org.dragon.agent.orchestration;

import java.util.Optional;
import java.util.UUID;

import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.react.ReActContext;
import org.dragon.agent.react.ReActExecutor;
import org.dragon.agent.react.ReActResult;
import org.dragon.agent.workflow.WorkflowExecutor;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 编排服务实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
public class OrchestrationServiceImpl implements OrchestrationService {

    private final WorkflowExecutor workflowExecutor;
    private final ReActExecutor reActExecutor;
    private final ModelRegistry modelRegistry;
    private final CharacterRegistry characterRegistry;

    public OrchestrationServiceImpl(WorkflowExecutor workflowExecutor,
                                   ReActExecutor reActExecutor,
                                   ModelRegistry modelRegistry,
                                   CharacterRegistry characterRegistry) {
        this.workflowExecutor = workflowExecutor;
        this.reActExecutor = reActExecutor;
        this.modelRegistry = modelRegistry;
        this.characterRegistry = characterRegistry;
    }

    @Override
    public OrchestrationResult orchestrate(OrchestrationRequest request) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            switch (request.getMode()) {
                case WORKFLOW:
                    // TODO: 实现工作流编排
                    return OrchestrationResult.builder()
                            .executionId(executionId)
                            .success(true)
                            .response("Workflow execution not implemented yet")
                            .durationMs(System.currentTimeMillis() - startTime)
                            .build();

                case REACT:
                    ReActResult result = runReAct(ReActRequest.builder()
                            .characterId(request.getCharacterId())
                            .userInput(request.getMessage())
                            .defaultModelId(modelRegistry.getDefault().map(m -> m.getId()).orElse(null))
                            .build());

                    return OrchestrationResult.builder()
                            .executionId(executionId)
                            .success(result.isSuccess())
                            .response(result.getResponse())
                            .durationMs(System.currentTimeMillis() - startTime)
                            .build();

                default:
                    return OrchestrationResult.builder()
                            .executionId(executionId)
                            .success(false)
                            .response("Unknown mode: " + request.getMode())
                            .durationMs(System.currentTimeMillis() - startTime)
                            .build();
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

    @Override
    public ReActResult runReAct(ReActRequest request) {
        // 获取 Character 的 Mind
        Optional<Character> characterOpt = characterRegistry.get(request.getCharacterId());
        Character character = characterOpt.orElseThrow(() -> new IllegalArgumentException("Character not found: " + request.getCharacterId()));

        // 构建系统 prompt
        String systemPrompt = "";
        if (character.getMindConfig() != null && character.getMindConfig().getPersonalityDescriptorPath() != null) {
            // TODO: 加载 Mind 的性格描述并转换为 prompt
        }

        // 构建 ReAct 上下文
        ReActContext context = ReActContext.builder()
                .executionId(UUID.randomUUID().toString())
                .characterId(request.getCharacterId())
                .defaultModelId(request.getDefaultModelId())
                .currentModelId(request.getDefaultModelId())
                .userInput(request.getUserInput())
                .systemPrompt(systemPrompt)
                .maxIterations(request.getMaxIterations())
                .build();

        return reActExecutor.execute(context);
    }
}

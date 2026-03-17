package org.dragon.agent.react;

import java.util.Optional;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.tool.ToolConnector;
import org.dragon.agent.tool.ToolRegistry;
import org.dragon.character.mind.memory.MemoryAccess;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * ReAct 执行器
 * 实现 ReAct 循环框架，允许 LLM 自主决定下一步行动
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ReActExecutor {

    private final LLMCaller llmCaller;
    private final ToolRegistry toolRegistry;
    private final MemoryAccess memoryAccess;
    private final ModelRegistry modelRegistry;

    public ReActExecutor(LLMCaller llmCaller,
                         ToolRegistry toolRegistry,
                         MemoryAccess memoryAccess,
                         ModelRegistry modelRegistry) {
        this.llmCaller = llmCaller;
        this.toolRegistry = toolRegistry;
        this.memoryAccess = memoryAccess;
        this.modelRegistry = modelRegistry;
    }

    /**
     * 执行 ReAct 循环
     *
     * @param context 执行上下文
     * @return 执行结果
     */
    public ReActResult execute(ReActContext context) {
        // 循环: Thought -> Action -> Observation
        while (!context.isComplete() && context.incrementIteration() <= context.getMaxIterations()) {
            try {
                // 支持为每个步骤指定不同模型，默认使用 context 中的模型
                String modelId = context.getCurrentModelId();

                // 构建 prompt
                String prompt = buildPrompt(context);

                // 调用 LLM
                LLMRequest request = LLMRequest.builder()
                        .modelId(modelId)
                        .messages(java.util.Collections.singletonList(
                                LLMRequest.LLMMessage.builder()
                                        .role(LLMRequest.LLMMessage.Role.USER)
                                        .content(prompt)
                                        .build()
                        ))
                        .systemPrompt(context.getSystemPrompt())
                        .build();

                LLMResponse response = llmCaller.call(request);
                String thought = response.getContent();

                context.addThought(thought);

                // 解析动作
                Action action = parseAction(thought);
                if (action == null) {
                    // 无法解析动作，直接返回当前思考作为响应
                    context.complete(thought);
                    break;
                }

                context.addAction(action);

                // 执行动作
                String result;
                if (action.getModelId() != null) {
                    // 动作执行也可指定模型
                    modelId = action.getModelId();
                    result = executeAction(action, modelId);
                } else {
                    result = executeAction(action, modelId);
                }

                context.addObservation(result);

                // 检查是否需要切换模型
                if (context.hasModelSwitch()) {
                    modelId = context.getNextModelId();
                }

                // 检查是否应该结束
                if (action.getType() == Action.ActionType.FINISH) {
                    context.complete(result);
                    break;
                }

            } catch (Exception e) {
                log.error("[ReAct] Execution error at iteration: {}", context.getCurrentIteration(), e);
                context.addObservation("Error: " + e.getMessage());

                if (context.getCurrentIteration() >= context.getMaxIterations()) {
                    context.complete("执行达到最大迭代次数");
                }
            }
        }

        return ReActResult.builder()
                .executionId(context.getExecutionId())
                .success(context.isComplete())
                .response(context.getFinalResponse())
                .iterations(context.getCurrentIteration())
                .thoughts(context.getThoughts())
                .actions(context.getActions())
                .observations(context.getObservations())
                .errorMessage(context.getErrorMessage())
                .build();
    }

    /**
     * 构建 Prompt
     *
     * @param context 上下文
     * @return Prompt
     */
    private String buildPrompt(ReActContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("用户输入: ").append(context.getUserInput()).append("\n\n");

        if (!context.getThoughts().isEmpty()) {
            prompt.append("之前的思考:\n");
            for (int i = 0; i < context.getThoughts().size(); i++) {
                prompt.append(i + 1).append(". ").append(context.getThoughts().get(i)).append("\n");
            }
            prompt.append("\n");
        }

        if (!context.getActions().isEmpty()) {
            prompt.append("之前的动作:\n");
            for (int i = 0; i < context.getActions().size(); i++) {
                Action a = context.getActions().get(i);
                prompt.append(i + 1).append(". ").append(a.getType()).append(": ");
                if (a.getToolName() != null) {
                    prompt.append(a.getToolName());
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        if (!context.getObservations().isEmpty()) {
            prompt.append("观察结果:\n");
            for (int i = 0; i < context.getObservations().size(); i++) {
                prompt.append(i + 1).append(". ").append(context.getObservations().get(i)).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("请根据上述信息，给出下一步的行动和思考。\n");

        return prompt.toString();
    }

    /**
     * 解析动作
     *
     * @param thought LLM 响应
     * @return 动作
     */
    private Action parseAction(String thought) {
        // TODO: 实现更复杂的动作解析逻辑
        // 这里简单模拟：如果包含特定关键词则执行对应动作

        if (thought.contains("FINISH") || thought.contains("完成") || thought.contains("回复")) {
            return Action.builder()
                    .type(Action.ActionType.FINISH)
                    .build();
        }

        // 默认为响应动作
        return Action.builder()
                .type(Action.ActionType.RESPOND)
                .build();
    }

    /**
     * 执行动作
     *
     * @param action  动作
     * @param modelId 模型 ID
     * @return 执行结果
     */
    private String executeAction(Action action, String modelId) {
        switch (action.getType()) {
            case TOOL -> {
                Optional<ToolConnector> connector = toolRegistry.get(action.getToolName());
                if (connector != null && connector.isPresent()) {
                    return connector.get().execute(action.getParameters()).getContent();
                }
                return "Tool not found: " + action.getToolName();
            }

            case MEMORY -> {
                return memoryAccess.semanticSearch(
                        (String) action.getParameters().get("query"),
                        (Integer) action.getParameters().getOrDefault("topK", 5)
                ).toString();
            }

            case RESPOND -> {
                return action.getToolName();
            }

            case FINISH -> {
                return action.getToolName();
            }

            default -> {
                return "Unknown action type";
            }
        }
    }
}

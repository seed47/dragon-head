package org.dragon.agent.react;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.agent.tool.ToolConnector;
import org.dragon.agent.tool.ToolRegistry;
import org.dragon.character.mind.memory.MemoryAccess;
import org.dragon.task.Task;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import lombok.extern.slf4j.Slf4j;

/**
 * ReAct 执行器
 * 实现 ReAct (Reasoning + Acting) 循环框架
 *
 * 流程：Thought -> Action -> Observation -> Thought -> ... -> Finish
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
    private final Gson gson;

    public ReActExecutor(LLMCaller llmCaller,
                         ToolRegistry toolRegistry,
                         MemoryAccess memoryAccess) {
        this.llmCaller = llmCaller;
        this.toolRegistry = toolRegistry;
        this.memoryAccess = memoryAccess;
        this.gson = new Gson();
    }

    /**
     * 执行 ReAct 循环
     *
     * @param context 执行上下文
     * @return 执行结果
     */
    public ReActResult execute(ReActContext context) {
        log.info("[ReAct] Starting ReAct loop for execution: {}", context.getExecutionId());

        // ReAct 循环：Thought -> Action -> Observation
        while (!context.isComplete() && context.incrementIteration() <= context.getMaxIterations()) {
            try {
                log.debug("[ReAct] Iteration {} started", context.getCurrentIteration());

                // Step 1: Thought - 让 LLM 分析并决定下一步行动
                String thought = think(context);

                // Step 2: Action - 根据思考执行动作
                String actionResult = act(context, thought);

                // Step 3: Observation - 观察动作结果
                observe(context, actionResult);

                // 检查是否应该结束
                if (shouldFinish(context)) {
                    context.complete(actionResult);
                    log.info("[ReAct] Execution completed at iteration {}", context.getCurrentIteration());
                    break;
                }

            } catch (Exception e) {
                log.error("[ReAct] Error at iteration: {}", context.getCurrentIteration(), e);
                handleError(context, e);
            }
        }

        // 检查是否达到最大迭代次数
        if (!context.isComplete() && context.getCurrentIteration() >= context.getMaxIterations()) {
            context.complete("达到最大迭代次数");
            log.warn("[ReAct] Max iterations reached: {}", context.getMaxIterations());
        }

        return buildResult(context);
    }

    // ==================== ReAct 步骤方法 ====================

    /**
     * Step 1: Thought
     * 让 LLM 分析问题，决定下一步行动
     *
     * @param context 执行上下文
     * @return LLM 的思考结果
     */
    private String think(ReActContext context) {
        String modelId = resolveModelId(context);
        String prompt = buildThoughtPrompt(context);

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

        // 根据是否启用流式调用选择不同的方法
        if (context.isStreamingEnabled()) {
            return streamThink(context, request);
        } else {
            return syncThink(context, request);
        }
    }

    /**
     * 同步思考
     */
    private String syncThink(ReActContext context, LLMRequest request) {
        LLMResponse response = llmCaller.call(request);
        String thought = response.getContent();
        context.addThought(thought);
        log.debug("[ReAct] Thought: {}", thought);
        return thought;
    }

    /**
     * 流式思考
     */
    private String streamThink(ReActContext context, LLMRequest request) {
        Stream<LLMResponse> stream = llmCaller.streamCall(request);
        StringBuilder fullContent = new StringBuilder();

        stream.forEach(response -> {
            String chunk = response.getContent();
            if (chunk != null) {
                fullContent.append(chunk);

                // 写入 Task
                Task task = context.getTask();
                if (task != null) {
                    String current = task.getCurrentStreamingContent();
                    task.setCurrentStreamingContent((current != null ? current : "") + chunk);
                }
            }
        });

        String thought = fullContent.toString();
        context.addThought(thought);
        log.debug("[ReAct] Streamed Thought: {}", thought);

        return thought;
    }

    /**
     * Step 2: Action
     * 解析思考结果，执行相应的动作
     *
     * @param context 执行上下文
     * @param thought 思考结果
     * @return 动作执行结果
     */
    private String act(ReActContext context, String thought) {
        // 解析动作
        Action action = parseAction(thought);
        if (action == null) {
            log.warn("[ReAct] Failed to parse action from thought");
            return "无法解析动作";
        }

        context.addAction(action);
        log.debug("[ReAct] Action: {} - {}", action.getType(), action.getToolName());

        // 执行动作
        String modelId = resolveModelId(context, action);
        return executeAction(action, modelId);
    }

    /**
     * Step 3: Observation
     * 记录动作执行结果到上下文
     *
     * @param context 执行上下文
     * @param result 动作执行结果
     */
    private void observe(ReActContext context, String result) {
        context.addObservation(result);
        log.debug("[ReAct] Observation: {}", result);
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断是否应该结束执行
     *
     * @param context 执行上下文
     * @return 是否应该结束
     */
    private boolean shouldFinish(ReActContext context) {
        if (context.getActions().isEmpty()) {
            return false;
        }

        Action lastAction = context.getActions().get(context.getActions().size() - 1);
        return lastAction.getType() == Action.ActionType.FINISH
                || lastAction.getType() == Action.ActionType.RESPOND;
    }

    /**
     * 处理错误
     *
     * @param context 执行上下文
     * @param e 异常
     */
    private void handleError(ReActContext context, Exception e) {
        String errorMsg = "Error: " + e.getMessage();
        context.addObservation(errorMsg);

        if (context.getCurrentIteration() >= context.getMaxIterations()) {
            context.complete("执行达到最大迭代次数");
        }
    }

    /**
     * 解析动作
     * 优先尝试 JSON 解析，回退到关键词匹配
     *
     * @param thought LLM 响应
     * @return 动作
     */
    private Action parseAction(String thought) {
        // 优先尝试 JSON 解析
        Action jsonAction = parseJsonAction(thought);
        if (jsonAction != null) {
            return jsonAction;
        }

        // 回退到关键词匹配
        return parseKeywordAction(thought);
    }

    /**
     * 尝试从 JSON 格式解析动作
     * 期望格式: {"action": "TOOL|RESPOND|FINISH", "tool": "xxx", "params": {...}}
     *
     * @param thought LLM 响应
     * @return 动作，如果解析失败返回 null
     */
    private Action parseJsonAction(String thought) {
        try {
            // 尝试提取 JSON 对象
            String jsonStr = extractJson(thought);
            if (jsonStr == null) {
                return null;
            }

            JsonObject json = gson.fromJson(jsonStr, JsonObject.class);

            if (!json.has("action")) {
                return null;
            }

            String actionType = json.get("action").getAsString().toUpperCase();
            Action.ActionType type = switch (actionType) {
                case "TOOL" -> Action.ActionType.TOOL;
                case "RESPOND" -> Action.ActionType.RESPOND;
                case "FINISH" -> Action.ActionType.FINISH;
                case "MEMORY" -> Action.ActionType.MEMORY;
                default -> null;
            };

            if (type == null) {
                return null;
            }

            Action.ActionBuilder builder = Action.builder().type(type);

            if (json.has("tool")) {
                builder.toolName(json.get("tool").getAsString());
            }

            if (json.has("params")) {
                Map<String, Object> params = gson.fromJson(json.get("params"), Map.class);
                builder.parameters(params);
            }

            return builder.build();

        } catch (JsonSyntaxException | IllegalStateException e) {
            log.debug("[ReAct] JSON 解析失败，回退到关键词匹配");
            return null;
        }
    }

    /**
     * 从文本中提取 JSON 对象
     *
     * @param text 文本
     * @return JSON 字符串，如果不存在返回 null
     */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    /**
     * 通过关键词匹配解析动作（回退方案）
     *
     * @param thought LLM 响应
     * @return 动作
     */
    private Action parseKeywordAction(String thought) {
        if (thought.contains("FINISH") || thought.contains("完成")) {
            return Action.builder()
                    .type(Action.ActionType.FINISH)
                    .build();
        }

        if (thought.contains("TOOL:") || thought.contains("工具:")) {
            String toolName = extractToolName(thought);
            return Action.builder()
                    .type(Action.ActionType.TOOL)
                    .toolName(toolName)
                    .build();
        }

        // 默认为响应动作
        return Action.builder()
                .type(Action.ActionType.RESPOND)
                .toolName(thought)
                .build();
    }

    /**
     * 从思考中提取工具名称
     *
     * @param thought 思考内容
     * @return 工具名称
     */
    private String extractToolName(String thought) {
        // 简单实现：查找 TOOL: 后面的内容
        int index = thought.indexOf("TOOL:");
        if (index >= 0) {
            String after = thought.substring(index + 5).trim();
            int spaceIndex = after.indexOf(' ');
            if (spaceIndex > 0) {
                return after.substring(0, spaceIndex);
            }
            return after;
        }

        index = thought.indexOf("工具:");
        if (index >= 0) {
            String after = thought.substring(index + 3).trim();
            int spaceIndex = after.indexOf(' ');
            if (spaceIndex > 0) {
                return after.substring(0, spaceIndex);
            }
            return after;
        }

        return null;
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

            case RESPOND, FINISH -> {
                return action.getToolName();
            }

            default -> {
                return "Unknown action type";
            }
        }
    }

    /**
     * 构建思考阶段的 Prompt
     *
     * @param context 上下文
     * @return Prompt
     */
    private String buildThoughtPrompt(ReActContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("用户输入: ").append(context.getUserInput()).append("\n\n");

        // 添加历史记录
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
                prompt.append(i + 1).append(". ").append(a.getType());
                if (a.getToolName() != null) {
                    prompt.append(": ").append(a.getToolName());
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

        prompt.append("请分析上述信息，给出下一步的行动。\n");
        prompt.append("请以 JSON 格式返回你的决策：\n");
        prompt.append("{\n");
        prompt.append("  \"action\": \"TOOL|RESPOND|FINISH|MEMORY\",  // 动作类型\n");
        prompt.append("  \"tool\": \"工具名称\",  // TOOL 类型时必填\n");
        prompt.append("  \"params\": {\"key\": \"value\"}  // 可选参数\n");
        prompt.append("}\n");
        prompt.append("说明：\n");
        prompt.append("- TOOL: 使用工具，tool 填写工具名称\n");
        prompt.append("- RESPOND: 直接回复用户\n");
        prompt.append("- FINISH: 完成任务并给出最终回复\n");
        prompt.append("- MEMORY: 搜索记忆，params 需要包含 query 字段\n");

        return prompt.toString();
    }

    /**
     * 解析模型 ID
     *
     * @param context 上下文
     * @return 模型 ID
     */
    private String resolveModelId(ReActContext context) {
        String modelId = context.getCurrentModelId();

        // 检查是否需要切换模型
        if (context.hasModelSwitch()) {
            modelId = context.getNextModelId();
        }

        return modelId != null ? modelId : context.getDefaultModelId();
    }

    /**
     * 解析模型 ID（考虑动作指定的模型）
     *
     * @param context 上下文
     * @param action 动作
     * @return 模型 ID
     */
    private String resolveModelId(ReActContext context, Action action) {
        if (action.getModelId() != null) {
            return action.getModelId();
        }
        return resolveModelId(context);
    }

    /**
     * 构建执行结果
     *
     * @param context 上下文
     * @return 执行结果
     */
    private ReActResult buildResult(ReActContext context) {
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
}

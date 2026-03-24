package org.dragon.agent.workflow;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.dragon.agent.model.ModelRegistry;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认工作流执行器实现
 * 通过遍历 Workflow Node 逐步执行
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultWorkflowExecutor implements WorkflowExecutor {

    private final ModelRegistry modelRegistry;
    private final CharacterRegistry characterRegistry;
    private final CharacterRuntimeBinder characterRuntimeBinder;

    /**
     * 正在执行的工作流状态
     */
    private final Map<String, WorkflowState> runningWorkflows = new ConcurrentHashMap<>();

    @Override
    public WorkflowResult execute(Workflow workflow, Map<String, Object> input) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        WorkflowState state = WorkflowState.builder()
                .executionId(executionId)
                .workflowId(workflow.getId())
                .status(WorkflowState.State.RUNNING)
                .context(new ConcurrentHashMap<>(input != null ? input : new HashMap<>()))
                .currentNodeId(null)
                .startTime(LocalDateTime.now())
                .build();

        runningWorkflows.put(executionId, state);

        try {
            // 执行工作流节点
            Object result = executeNodes(workflow, state);

            long duration = System.currentTimeMillis() - startTime;
            state.setStatus(WorkflowState.State.COMPLETED);
            state.setEndTime(LocalDateTime.now());

            Map<String, Object> output = new HashMap<>();
            output.put("result", result);
            output.put("state", state.getContext());

            return WorkflowResult.builder()
                    .executionId(executionId)
                    .workflowId(workflow.getId())
                    .status(WorkflowState.State.COMPLETED)
                    .output(output)
                    .durationMs(duration)
                    .build();

        } catch (Exception e) {
            log.error("[DefaultWorkflowExecutor] Workflow {} execution failed: {}", workflow.getId(), e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;
            state.setStatus(WorkflowState.State.FAILED);
            state.setEndTime(LocalDateTime.now());
            state.addError(e.getMessage());

            return WorkflowResult.builder()
                    .executionId(executionId)
                    .workflowId(workflow.getId())
                    .status(WorkflowState.State.FAILED)
                    .errorMessage(e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }

    /**
     * 执行节点列表
     */
    private Object executeNodes(Workflow workflow, WorkflowState state) throws Exception {
        List<Workflow.Node> nodes = workflow.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        Object currentOutput = null;
        String nextNodeId = nodes.get(0).getId();

        while (nextNodeId != null && !nextNodeId.isEmpty()) {
            Workflow.Node currentNode = findNodeById(nodes, nextNodeId);
            if (currentNode == null) {
                throw new IllegalArgumentException("Node not found: " + nextNodeId);
            }

            state.setCurrentNodeId(currentNode.getId());
            log.info("[DefaultWorkflowExecutor] Executing node: {} ({})", currentNode.getName(), currentNode.getType());

            currentOutput = executeNode(currentNode, state);

            // 根据节点类型决定下一个节点
            nextNodeId = determineNextNode(currentNode, currentOutput, state);
        }

        return currentOutput;
    }

    /**
     * 执行单个节点
     */
    private Object executeNode(Workflow.Node node, WorkflowState state) throws Exception {
        switch (node.getType()) {
            case MODEL:
                return executeModelNode(node, state);
            case TOOL:
                return executeToolNode(node, state);
            case CONDITION:
                return evaluateCondition(node, state);
            case LOOP:
                return executeLoop(node, state);
            case END:
                return state.getContext().get("result");
            default:
                throw new IllegalArgumentException("Unknown node type: " + node.getType());
        }
    }

    /**
     * 执行模型调用节点
     */
    private Object executeModelNode(Workflow.Node node, WorkflowState state) throws Exception {
        String modelId = (String) node.getConfig().get("modelId");
        String promptTemplate = (String) node.getConfig().get("prompt");

        // 获取模型
        var modelOpt = modelRegistry.get(modelId);
        if (modelOpt.isEmpty()) {
            throw new IllegalStateException("Model not found: " + modelId);
        }

        // 替换变量
        String prompt = replaceVariables(promptTemplate, state.getContext());

        // 调用模型
        log.info("[DefaultWorkflowExecutor] Calling model {} with prompt: {}", modelId, prompt);

        // 模拟返回 - TODO: 实际调用模型
        String result = "Model response for: " + prompt;
        state.getContext().put("_lastResult", result);

        return result;
    }

    /**
     * 执行工具调用节点
     */
    private Object executeToolNode(Workflow.Node node, WorkflowState state) {
        String toolName = (String) node.getConfig().get("toolName");
        log.info("[DefaultWorkflowExecutor] Calling tool: {}", toolName);
        // TODO: 实际调用工具
        state.getContext().put("_lastResult", "Tool result: " + toolName);
        return "Tool result: " + toolName;
    }

    /**
     * 执行条件判断节点
     */
    private Object evaluateCondition(Workflow.Node node, WorkflowState state) {
        String conditionExpr = node.getConditionExpr();
        log.info("[DefaultWorkflowExecutor] Evaluating condition: {}", conditionExpr);
        // TODO: 实际解析表达式
        boolean result = true; // 简化处理
        state.getContext().put("_conditionResult", result);
        return result;
    }

    /**
     * 执行循环节点
     */
    private Object executeLoop(Workflow.Node node, WorkflowState state) throws Exception {
        Workflow.LoopConfig loopConfig = node.getLoopConfig();
        int maxIterations = loopConfig != null ? loopConfig.getMaxIterations() : 10;

        Object loopResult = null;
        for (int i = 0; i < maxIterations; i++) {
            state.setLoopIteration(i);
            log.info("[DefaultWorkflowExecutor] Loop iteration {}/{}", i + 1, maxIterations);

            // TODO: 执行循环体节点
            // 目前简化处理

            // 检查终止条件
            if (loopConfig != null && loopConfig.getConditionExpr() != null) {
                boolean shouldBreak = evaluateBreakCondition(loopConfig, state);
                if (shouldBreak) {
                    log.info("[DefaultWorkflowExecutor] Loop terminated by condition");
                    break;
                }
            }
        }

        return loopResult;
    }

    /**
     * 评估循环终止条件
     */
    private boolean evaluateBreakCondition(Workflow.LoopConfig config, WorkflowState state) {
        String breakVariable = config.getBreakVariable();
        String breakValue = config.getBreakValue();

        if (breakVariable != null && breakValue != null) {
            Object currentValue = state.getContext().get(breakVariable);
            return breakValue.equals(currentValue != null ? currentValue.toString() : null);
        }

        return false;
    }

    /**
     * 决定下一个节点
     */
    private String determineNextNode(Workflow.Node currentNode, Object nodeOutput, WorkflowState state) {
        // 如果当前节点有 nextNodeId，直接返回
        if (currentNode.getNextNodeId() != null && !currentNode.getNextNodeId().isEmpty()) {
            // 如果是条件节点，根据结果决定
            if (currentNode.getType() == Workflow.Node.NodeType.CONDITION) {
                Boolean conditionResult = (Boolean) state.getContext().get("_conditionResult");
                if (conditionResult != null && conditionResult) {
                    return currentNode.getNextNodeId(); // 条件为真，走 nextNodeId
                } else {
                    // 条件为假，查找 else 分支
                    Object elseNodeId = currentNode.getConfig().get("elseNodeId");
                    return elseNodeId != null ? elseNodeId.toString() : null;
                }
            }
            return currentNode.getNextNodeId();
        }

        // 如果是 END 节点，返回 null
        if (currentNode.getType() == Workflow.Node.NodeType.END) {
            return null;
        }

        return null;
    }

    /**
     * 查找节点
     */
    private Workflow.Node findNodeById(List<Workflow.Node> nodes, String nodeId) {
        if (nodes == null) return null;
        return nodes.stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 替换变量
     */
    private String replaceVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null) return template;

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    @Override
    public void terminate(String executionId) {
        WorkflowState state = runningWorkflows.get(executionId);
        if (state != null) {
            state.setStatus(WorkflowState.State.TERMINATED);
            state.setEndTime(LocalDateTime.now());
            log.info("[DefaultWorkflowExecutor] Terminated workflow: {}", executionId);
        }
    }

    @Override
    public void suspend(String executionId) {
        WorkflowState state = runningWorkflows.get(executionId);
        if (state != null) {
            state.setStatus(WorkflowState.State.SUSPENDED);
            log.info("[DefaultWorkflowExecutor] Suspended workflow: {}", executionId);
        }
    }

    @Override
    public void resume(String executionId) {
        WorkflowState state = runningWorkflows.get(executionId);
        if (state != null) {
            state.setStatus(WorkflowState.State.RUNNING);
            log.info("[DefaultWorkflowExecutor] Resumed workflow: {}", executionId);
        }
    }

    @Override
    public WorkflowState getState(String executionId) {
        return runningWorkflows.get(executionId);
    }
}
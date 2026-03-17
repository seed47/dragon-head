package org.dragon.agent.workflow;

import java.util.Map;

/**
 * 工作流执行器接口
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkflowExecutor {

    /**
     * 执行工作流
     *
     * @param workflow 工作流定义
     * @param input    输入参数
     * @return 执行结果
     */
    WorkflowResult execute(Workflow workflow, Map<String, Object> input);

    /**
     * 终止执行
     *
     * @param executionId 执行 ID
     */
    void terminate(String executionId);

    /**
     * 暂停执行
     *
     * @param executionId 执行 ID
     */
    void suspend(String executionId);

    /**
     * 恢复执行
     *
     * @param executionId 执行 ID
     */
    void resume(String executionId);

    /**
     * 获取执行状态
     *
     * @param executionId 执行 ID
     * @return 执行状态
     */
    WorkflowState getState(String executionId);
}

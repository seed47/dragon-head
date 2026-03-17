package org.dragon.agent.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工作流执行结果
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResult {

    /**
     * 执行 ID
     */
    private String executionId;

    /**
     * 工作流 ID
     */
    private String workflowId;

    /**
     * 执行状态
     */
    private WorkflowState.State status;

    /**
     * 输出结果
     */
    private Map<String, Object> output;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时 (毫秒)
     */
    private long durationMs;

    /**
     * 执行步数
     */
    private int stepsExecuted;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status == WorkflowState.State.COMPLETED;
    }
}

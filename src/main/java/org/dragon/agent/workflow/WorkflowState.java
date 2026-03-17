package org.dragon.agent.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行状态
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowState {

    /**
     * 执行 ID
     */
    private String executionId;

    /**
     * 工作流 ID
     */
    private String workflowId;

    /**
     * Character ID
     */
    private String characterId;

    /**
     * 当前节点 ID
     */
    private String currentNodeId;

    /**
     * 上下文变量
     */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    /**
     * 节点执行结果
     */
    @Builder.Default
    private Map<String, Object> results = new HashMap<>();

    /**
     * 状态
     */
    private State status;

    /**
     * 当前执行步数
     */
    private int currentStep;

    /**
     * 当前循环迭代次数
     */
    private int loopIteration;

    /**
     * 错误信息列表
     */
    @Builder.Default
    private List<String> errorMessages = new ArrayList<>();

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态枚举
     */
    public enum State {
        /** 运行中 */
        RUNNING,
        /** 暂停 */
        SUSPENDED,
        /** 完成 */
        COMPLETED,
        /** 失败 */
        FAILED,
        /** 终止 */
        TERMINATED
    }

    /**
     * 添加错误信息
     *
     * @param error 错误信息
     */
    public void addError(String error) {
        this.errorMessages.add(error);
    }

    /**
     * 检查是否有错误
     *
     * @return 是否有错误
     */
    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }
}

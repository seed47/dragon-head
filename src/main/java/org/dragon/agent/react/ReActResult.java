package org.dragon.agent.react;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ReAct 执行结果
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActResult {

    /**
     * 执行 ID
     */
    private String executionId;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 最终响应
     */
    private String response;

    /**
     * 迭代次数
     */
    private int iterations;

    /**
     * 思考历史
     */
    private List<String> thoughts;

    /**
     * 动作历史
     */
    private List<Action> actions;

    /**
     * 观察结果
     */
    private List<String> observations;

    /**
     * 错误信息
     */
    private String errorMessage;
}

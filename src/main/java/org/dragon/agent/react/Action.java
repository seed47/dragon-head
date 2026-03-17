package org.dragon.agent.react;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * ReAct 动作
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {

    /**
     * 动作类型
     */
    private ActionType type;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 参数
     */
    private Map<String, Object> parameters;

    /**
     * 指定执行该动作使用的模型 (可选)
     */
    private String modelId;

    /**
     * 动作类型枚举
     */
    public enum ActionType {
        /** 调用工具 */
        TOOL,
        /** 查询记忆 */
        MEMORY,
        /** 生成回复 */
        RESPOND,
        /** 结束执行 */
        FINISH
    }
}

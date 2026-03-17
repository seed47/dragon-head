package org.dragon.agent.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {

    /**
     * 工作流 ID
     */
    private String id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 节点列表
     */
    private List<Node> nodes;

    /**
     * 变量
     */
    private Map<String, Object> variables;

    /**
     * 终止配置
     */
    private TerminationConfig terminationConfig;

    /**
     * 描述
     */
    private String description;

    /**
     * 节点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        /**
         * 节点 ID
         */
        private String id;

        /**
         * 节点类型
         */
        private NodeType type;

        /**
         * 节点名称
         */
        private String name;

        /**
         * 配置
         */
        private Map<String, Object> config;

        /**
         * 下一节点 ID
         */
        private String nextNodeId;

        /**
         * 条件分支表达式
         */
        private String conditionExpr;

        /**
         * 循环节点列表
         */
        private List<String> loopNodes;

        /**
         * 循环配置
         */
        private LoopConfig loopConfig;

        /**
         * 节点类型枚举
         */
        public enum NodeType {
            /** 模型调用 */
            MODEL,
            /** 工具调用 */
            TOOL,
            /** 条件判断 */
            CONDITION,
            /** 循环 */
            LOOP,
            /** 结束 */
            END
        }
    }

    /**
     * 循环配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoopConfig {
        /**
         * 最大迭代次数
         */
        private int maxIterations;

        /**
         * 循环终止条件表达式
         */
        private String conditionExpr;

        /**
         * 终止变量名
         */
        private String breakVariable;

        /**
         * 终止变量值
         */
        private String breakValue;
    }
}

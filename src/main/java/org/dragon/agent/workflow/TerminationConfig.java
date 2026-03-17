package org.dragon.agent.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 终止配置
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminationConfig {

    /**
     * 最大步数限制
     */
    private int maxSteps;

    /**
     * 超时时间 (如 "5m", "30s")
     */
    private String timeout;

    /**
     * 自动终止条件列表
     */
    private List<TerminationCondition> conditions;

    /**
     * 终止条件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerminationCondition {
        /**
         * 检查的变量
         */
        private String variable;

        /**
         * 操作符
         */
        private Operator operator;

        /**
         * 比较值
         */
        private String value;

        /**
         * 达到条件时的状态
         */
        private WorkflowState.State terminationState;

        /**
         * 操作符枚举
         */
        public enum Operator {
            GT,   // 大于
            LT,   // 小于
            EQ,   // 等于
            GTE,  // 大于等于
            LTE,  // 小于等于
            NE    // 不等于
        }
    }
}

package org.dragon.workspace.commons.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 条件逻辑内容
 * 用于存储条件触发规则
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionalContent {

    /**
     * 条件
     */
    private Condition when;

    /**
     * 条件满足时的结果
     */
    private Object then;

    /**
     * 条件不满足时的结果（可选）
     */
    private Object else_;

    /**
     * 条件定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        /**
         * 字段名
         */
        private String field;

        /**
         * 操作符：equals, notEquals, contains, greaterThan, lessThan
         */
        private String equals;

        /**
         * 比较值
         */
        private Object value;
    }
}
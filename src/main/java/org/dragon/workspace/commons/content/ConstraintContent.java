package org.dragon.workspace.commons.content;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 约束列表内容
 * 用于存储多个约束条件
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintContent {

    /**
     * 约束列表
     */
    private List<Constraint> constraints;

    /**
     * 约束项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Constraint {
        /**
         * 约束键名
         */
        private String key;

        /**
         * 约束值
         */
        private Object value;

        /**
         * 单位（可选）
         */
        private String unit;
    }
}
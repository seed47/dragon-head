package org.dragon.workspace.commons.content;

/**
 * CommonSense 内容类型枚举
 * 定义不同类型的规则内容结构
 *
 * @author wyj
 * @version 1.0
 */
public enum ContentType {
    /**
     * 简单键值对
     */
    SIMPLE,

    /**
     * 约束列表
     */
    CONSTRAINT,

    /**
     * 禁止项
     */
    FORBIDDEN,

    /**
     * 条件逻辑
     */
    CONDITIONAL,

    /**
     * 模板
     */
    TEMPLATE
}
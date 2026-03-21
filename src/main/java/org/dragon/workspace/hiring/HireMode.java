package org.dragon.workspace.hiring;

/**
 * Hire 模式枚举
 * 定义 workspace 执行 hire/fire 操作时的模式
 *
 * @author wyj
 * @version 1.0
 */
public enum HireMode {

    /**
     * 默认模式：使用预定义的默认 Character 池
     */
    DEFAULT,

    /**
     * 手动模式：用户手动选择目标 Character
     */
    MANUAL,

    /**
     * 自动模式：使用 HR Character 执行 hire/fire 操作
     */
    AUTO
}

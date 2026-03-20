package org.dragon.config;

/**
 * Prompt 键常量定义
 * 用于在 ConfigStore 中管理各类 prompt
 *
 * @author wyj
 * @version 1.0
 */
public class PromptKeys {

    // ==================== Observer 模块 ====================

    /**
     * Observer 优化建议生成 prompt
     */
    public static final String OBSERVER_SUGGESTION = "observer.suggestion";

    /**
     * Observer Personality 增强 prompt
     */
    public static final String OBSERVER_PERSONALITY_ENHANCEMENT = "observer.personalityEnhancement";

    // ==================== ReAct 模块 ====================

    /**
     * ReAct 任务分解 prompt
     */
    public static final String REACT_TASK_DECOMPOSE = "react.taskDecompose";

    /**
     * ReAct 执行 prompt
     */
    public static final String REACT_EXECUTE = "react.execute";

    // ==================== Character 模块 ====================

    /**
     * Character 默认系统 prompt
     */
    public static final String CHARACTER_SYSTEM = "character.system";

    /**
     * Character 任务执行 prompt
     */
    public static final String CHARACTER_TASK = "character.task";

    // ==================== Organization 模块 ====================

    /**
     * Organization 任务分配 prompt
     */
    public static final String ORG_TASK_ASSIGN = "org.taskAssign";

    /**
     * Organization 协作 prompt
     */
    public static final String ORG_COLLABORATION = "org.collaboration";

    // ==================== 便捷方法 ====================

    /**
     * 判断是否为 Observer 相关的 prompt
     */
    public static boolean isObserverPrompt(String key) {
        return key != null && key.startsWith("observer.");
    }

    /**
     * 判断是否为 ReAct 相关的 prompt
     */
    public static boolean isReActPrompt(String key) {
        return key != null && key.startsWith("react.");
    }

    /**
     * 判断是否为 Character 相关的 prompt
     */
    public static boolean isCharacterPrompt(String key) {
        return key != null && key.startsWith("character.");
    }

    /**
     * 判断是否为 Organization 相关的 prompt
     */
    public static boolean isOrganizationPrompt(String key) {
        return key != null && key.startsWith("org.");
    }
}

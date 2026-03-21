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

    // ==================== HR 模块 ====================

    /**
     * HR 雇佣决策 prompt
     */
    public static final String HR_HIRE_DECISION = "hr.hire.decision";

    /**
     * HR 雇佣选择 prompt（从候选中选择 Character）
     */
    public static final String HR_HIRE_SELECT = "hr.hire.select";

    /**
     * HR 解雇决策 prompt
     */
    public static final String HR_FIRE_DECISION = "hr.fire.decision";

    /**
     * HR 生成职责描述 prompt
     */
    public static final String HR_DUTY_GENERATE = "hr.duty.generate";

    // ==================== 选择模块 ====================

    /**
     * 通用选择 prompt
     */
    public static final String SELECTION_GENERIC = "selection.generic";

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

    /**
     * 判断是否为 HR 相关的 prompt
     */
    public static boolean isHrPrompt(String key) {
        return key != null && key.startsWith("hr.");
    }

    /**
     * 判断是否为选择相关的 prompt
     */
    public static boolean isSelectionPrompt(String key) {
        return key != null && key.startsWith("selection.");
    }
}

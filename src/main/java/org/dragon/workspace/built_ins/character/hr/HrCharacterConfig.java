package org.dragon.workspace.built_ins.character.hr;

import lombok.Builder;
import lombok.Data;

/**
 * HR Character 配置类
 * 定义 HR Character 的默认配置信息
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
public class HrCharacterConfig {

    /**
     * HR Character 类型标识
     */
    public static final String CHARACTER_TYPE = "hr";

    /**
     * 默认名称
     */
    @Builder.Default
    private String name = "HR Manager";

    /**
     * 默认描述
     */
    @Builder.Default
    private String description = "负责 Workspace 的人力资源管理，包括招聘、解雇、职责分配等";

    /**
     * 雇佣决策提示词
     */
    @Builder.Default
    private String hirePrompt = "请评估是否应该雇佣该 Character 到工作空间，并给出理由。";

    /**
     * 解雇决策提示词
     */
    @Builder.Default
    private String firePrompt = "请评估是否应该解雇该 Character，并给出理由。";

    /**
     * 职责生成提示词
     */
    @Builder.Default
    private String dutyPrompt = "请为该 Character 生成一个合适的职责描述。";

    /**
     * 默认模型ID
     */
    @Builder.Default
    private String defaultModelId = "claude-sonnet-4-20250514";
}

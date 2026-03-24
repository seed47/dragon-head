package org.dragon.workspace.built_ins.character.prompt_writer.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PromptWriter 输入结构
 * 用于 WorkspaceTaskArrangementService 向 PromptWriter Character 传递结构化输入
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptWriterInput {

    /**
     * Workspace ID
     */
    private String workspaceId;

    /**
     * Prompt 类型：member_selection, task_decompose
     */
    private String promptType;

    /**
     * Prompt 模板
     */
    private String promptTemplate;

    /**
     * 任务信息
     */
    private TaskInfo task;

    /**
     * 成员列表
     */
    private List<MemberInfo> members;

    /**
     * 上下文提示（可选）
     */
    private Map<String, Object> contextHints;

    /**
     * 协作会话 ID（可选）
     */
    private String collaborationSessionId;

    /**
     * 同级 Character 列表（可选）
     */
    private List<String> peerCharacterIds;

    /**
     * 依赖任务列表（可选）
     */
    private List<String> dependencyTaskIds;

    /**
     * 最新会话消息（可选）
     */
    private List<String> latestSessionMessages;

    /**
     * 协作约束（可选）
     */
    private String collaborationConstraint;

    /**
     * 是否允许追问用户
     */
    @Builder.Default
    private boolean allowFollowUp = true;

    /**
     * 任务信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskInfo {
        private String id;
        private String name;
        private String description;
        private Object input;
        private String parentTaskId;
    }

    /**
     * 成员信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private String characterId;
        private String role;
        private String layer;
        private String capability;
        private String description;
    }
}
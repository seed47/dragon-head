package org.dragon.workspace.task;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WorkspaceTask 工作空间任务实体
 * 雇佣成功后生成的可执行单元
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceTask {

    /**
     * 执行者类型
     */
    public enum ExecutorType {
        CHARACTER
    }

    /**
     * 任务唯一标识
     */
    private String id;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 雇佣请求 ID
     */
    private String hiringRequestId;

    /**
     * 雇佣记录 ID
     */
    private String hiringRecordId;

    /**
     * 创建者 ID
     */
    private String creatorId;

    /**
     * 执行者类型
     */
    private ExecutorType executorType;

    /**
     * 执行者 ID（Character ID）
     */
    private String executorId;

    /**
     * 内部任务 ID（如 CharacterTask ID）
     */
    private String internalTaskId;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务状态
     */
    @Builder.Default
    private WorkspaceTaskStatus status = WorkspaceTaskStatus.PENDING;

    /**
     * 任务输入
     */
    private Object input;

    /**
     * 任务输出/结果
     */
    private Object output;

    /**
     * 任务结果（String 形式，用于兼容）
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 子任务 ID 列表
     */
    private List<String> subTaskIds;

    /**
     * 协作会话 ID
     */
    private String collaborationSessionId;

    /**
     * 分配的成员 ID 列表
     */
    private List<String> assignedMemberIds;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 开始执行时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
}

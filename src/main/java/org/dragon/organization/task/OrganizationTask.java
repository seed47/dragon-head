package org.dragon.organization.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OrganizationTask 组织级任务
 * 业务级别的任务，可分解为多个 SubTask
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationTask {

    /**
     * 任务类型
     */
    public enum OrgTaskType {
        COLLABORATION,  // 协作任务
        SPLITTED,       // 可拆分任务
        MANAGEMENT     // 管理任务
    }

    /**
     * 任务 ID
     */
    private String id;

    /**
     * 组织 ID
     */
    private String organizationId;

    /**
     * 父任务 ID（可选）
     */
    private String parentTaskId;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务类型
     */
    @Builder.Default
    private OrgTaskType type = OrgTaskType.COLLABORATION;

    /**
     * 任务状态
     */
    @Builder.Default
    private OrgTaskStatus status = OrgTaskStatus.SUBMITTED;

    /**
     * 任务输入
     */
    private Object input;

    /**
     * 任务输出
     */
    private Object output;

    /**
     * 分配的成员 ID 列表
     */
    private List<String> assignedMemberIds;

    /**
     * 子任务 ID 列表
     */
    private List<String> subTaskIds;

    /**
     * 依赖的任务 ID 列表
     */
    private List<String> dependencies;

    /**
     * 任务上下文
     */
    private Map<String, Object> context;

    /**
     * 扩展属性
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建者 ID
     */
    private String creatorId;

    /**
     * 任务目标
     */
    private String goal;

    /**
     * 约束条件
     */
    private String constraints;
}

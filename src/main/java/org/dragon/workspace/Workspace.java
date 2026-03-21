package org.dragon.workspace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workspace 实体
 * 工作空间是管理 Characters 团队和执行任务的统一入口
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    /**
     * 工作空间状态
     */
    public enum Status {
        ACTIVE,    // 活跃
        INACTIVE,  // 未激活
        ARCHIVED   // 已归档
    }

    /**
     * 工作空间唯一标识
     */
    private String id;

    /**
     * 工作空间名称
     */
    private String name;

    /**
     * 工作空间描述
     */
    private String description;

    /**
     * 所有者 ID
     */
    private String owner;

    /**
     * 工作空间状态
     */
    @Builder.Default
    private Status status = Status.INACTIVE;

    /**
     * 扩展属性
     * 包含 advantages, defaultWeight, defaultPriority 等
     */
    private Map<String, Object> properties;

    /**
     * 工作空间文化/人格
     * 定义工作空间的协作风格和决策模式
     */
    private WorkspacePersonality personality;

    /**
     * 成员列表（仅用于显示，不存储，由 WorkspaceMemberStore 管理）
     */
    @Builder.Default
    private List<org.dragon.workspace.member.WorkspaceMember> members = new java.util.ArrayList<>();

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

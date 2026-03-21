package org.dragon.workspace.actionlog;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workspace 动作日志实体
 * 统一记录 workspace 的 hire/fire 等操作
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceActionLog {

    /**
     * 动作类型枚举
     */
    public enum ActionType {
        /**
         * 雇佣
         */
        HIRE,
        /**
         * 解雇
         */
        FIRE,
        /**
         * 更新职责描述
         */
        UPDATE_DUTY,
        /**
         * 自动分配
         */
        AUTO_ASSIGN
    }

    /**
     * 唯一标识
     */
    private String id;

    /**
     * Workspace ID
     */
    private String workspaceId;

    /**
     * 动作类型
     */
    private ActionType actionType;

    /**
     * 目标 Character ID
     */
    private String targetCharacterId;

    /**
     * 操作者 (user / HR Character)
     */
    private String operator;

    /**
     * 详情 (JSON 格式)
     */
    private String details;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

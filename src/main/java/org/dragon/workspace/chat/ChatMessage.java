package org.dragon.workspace.chat;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChatMessage 聊天消息
 * 用于工作空间内 Character 之间的消息传递
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息类型
     */
    public enum MessageType {
        TEXT,         // 文本消息
        STRUCTURED,   // 结构化数据
        TASK,         // 任务消息
        BROADCAST,    // 广播消息
        TASK_UPDATE,      // 任务状态更新
        TASK_ASSIGNMENT, // 任务分配
        TASK_RESULT,     // 任务结果
        TASK_REQUEST,    // 任务请求
        TASK_HELP_REQUEST // 请求帮助
    }

    /**
     * 任务消息用途
     */
    public enum TaskMessagePurpose {
        TASK_UPDATE,      // 任务状态更新
        TASK_ASSIGNMENT, // 任务分配
        TASK_RESULT,     // 任务结果汇报
        TASK_DEPENDENCY, // 依赖通知
        TASK_PROGRESS,   // 进度汇报
        TASK_BLOCKED,    // 任务阻塞
        TASK_NEED_HELP,  // 请求帮助
        TASK_COMPLETE    // 任务完成
    }

    /**
     * 消息 ID
     */
    private String id;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 发送者 ID (Character ID)
     */
    private String senderId;

    /**
     * 接收者 ID (Character ID, null 表示广播)
     */
    private String receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    /**
     * 关联的协作会话 ID
     */
    private String sessionId;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 是否已读
     */
    @Builder.Default
    private boolean read = false;

    /**
     * 扩展属性
     */
    private java.util.Map<String, Object> metadata;

    // ==================== 任务协作字段 ====================

    /**
     * 关联的任务 ID
     */
    private String taskId;

    /**
     * 关联的父任务 ID
     */
    private String parentTaskId;

    /**
     * 任务消息用途（当 messageType 为 TASK_* 时使用）
     */
    private TaskMessagePurpose taskPurpose;

    /**
     * 关联的子任务 ID（用于任务分配等场景）
     */
    private String relatedTaskId;

    /**
     * 任务结果状态（用于 TASK_RESULT 类型消息）
     */
    private String taskResultStatus;
}

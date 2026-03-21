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
        BROADCAST     // 广播消息
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
}

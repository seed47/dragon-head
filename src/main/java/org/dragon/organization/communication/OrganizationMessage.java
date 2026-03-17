package org.dragon.organization.communication;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OrganizationMessage 组织消息
 * 用于组织成员之间的消息传递
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMessage {

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
     * 组织 ID
     */
    private String organizationId;

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

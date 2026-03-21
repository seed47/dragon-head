package org.dragon.workspace.chat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ChatMessageStore 消息存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface ChatMessageStore {

    /**
     * 保存消息
     *
     * @param message 消息
     */
    void save(ChatMessage message);

    /**
     * 根据 ID 获取消息
     *
     * @param messageId 消息 ID
     * @return 消息
     */
    ChatMessage findById(String messageId);

    /**
     * 获取工作空间消息
     *
     * @param workspaceId 工作空间 ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<ChatMessage> findByWorkspaceId(String workspaceId, int limit);

    /**
     * 获取工作空间时间范围内的消息
     *
     * @param workspaceId 工作空间 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息列表
     */
    List<ChatMessage> findByWorkspaceIdAndTimeRange(
            String workspaceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取接收者的消息
     *
     * @param workspaceId 工作空间 ID
     * @param receiverId 接收者 ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<ChatMessage> findByWorkspaceIdAndReceiverId(
            String workspaceId, String receiverId, int limit);

    /**
     * 获取特定 Character 发送或接收的消息
     *
     * @param characterId Character ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<ChatMessage> findByCharacterId(String characterId, int limit);

    /**
     * 获取会话消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> findBySessionId(String sessionId);

    /**
     * 标记消息为已读
     *
     * @param messageId 消息 ID
     */
    void markAsRead(String messageId);

    /**
     * 删除消息
     *
     * @param messageId 消息 ID
     */
    void delete(String messageId);

    /**
     * 删除工作空间所有消息
     *
     * @param workspaceId 工作空间 ID
     */
    void deleteByWorkspaceId(String workspaceId);
}

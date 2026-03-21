package org.dragon.workspace.chat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ChatRoomObserver 聊天室观察者接口
 * 用于 Observer 模块获取聊天记录
 *
 * @author wyj
 * @version 1.0
 */
public interface ChatRoomObserver {

    /**
     * 获取工作空间内所有消息
     *
     * @param workspaceId 工作空间 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息列表
     */
    List<ChatMessage> getAllMessages(String workspaceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取特定 Character 发送或接收的消息
     *
     * @param characterId Character ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<ChatMessage> getMessagesByCharacter(String characterId, int limit);

    /**
     * 获取会话中的所有消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> getSessionMessages(String sessionId);
}

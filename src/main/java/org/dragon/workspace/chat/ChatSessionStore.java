package org.dragon.workspace.chat;

import java.util.List;

/**
 * ChatSessionStore 聊天会话存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface ChatSessionStore {

    /**
     * 保存会话
     *
     * @param session 会话
     */
    void save(ChatSession session);

    /**
     * 根据 ID 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话
     */
    ChatSession findById(String sessionId);

    /**
     * 根据工作空间 ID 获取会话
     *
     * @param workspaceId 工作空间 ID
     * @return 会话列表
     */
    List<ChatSession> findByWorkspaceId(String workspaceId);

    /**
     * 根据任务 ID 获取会话
     *
     * @param taskId 任务 ID
     * @return 会话
     */
    ChatSession findByTaskId(String taskId);

    /**
     * 获取工作空间活跃会话
     *
     * @param workspaceId 工作空间 ID
     * @return 会话列表
     */
    List<ChatSession> findActiveByWorkspaceId(String workspaceId);

    /**
     * 更新会话
     *
     * @param session 会话
     */
    void update(ChatSession session);

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     */
    void delete(String sessionId);
}

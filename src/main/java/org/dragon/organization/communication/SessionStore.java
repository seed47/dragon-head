package org.dragon.organization.communication;

import java.util.List;

/**
 * SessionStore 协作会话存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface SessionStore {

    /**
     * 保存会话
     *
     * @param session 会话
     */
    void save(CollaborationSession session);

    /**
     * 根据 ID 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话
     */
    CollaborationSession findById(String sessionId);

    /**
     * 根据组织 ID 获取会话
     *
     * @param organizationId 组织 ID
     * @return 会话列表
     */
    List<CollaborationSession> findByOrganizationId(String organizationId);

    /**
     * 根据任务 ID 获取会话
     *
     * @param taskId 任务 ID
     * @return 会话
     */
    CollaborationSession findByTaskId(String taskId);

    /**
     * 获取组织活跃会话
     *
     * @param organizationId 组织 ID
     * @return 会话列表
     */
    List<CollaborationSession> findActiveByOrganizationId(String organizationId);

    /**
     * 更新会话
     *
     * @param session 会话
     */
    void update(CollaborationSession session);

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     */
    void delete(String sessionId);
}

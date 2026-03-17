package org.dragon.organization.communication;

import java.util.List;

/**
 * MessageStore 消息存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface MessageStore {

    /**
     * 保存消息
     *
     * @param message 消息
     */
    void save(OrganizationMessage message);

    /**
     * 根据 ID 获取消息
     *
     * @param messageId 消息 ID
     * @return 消息
     */
    OrganizationMessage findById(String messageId);

    /**
     * 获取组织消息
     *
     * @param organizationId 组织 ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<OrganizationMessage> findByOrganizationId(String organizationId, int limit);

    /**
     * 获取接收者的消息
     *
     * @param organizationId 组织 ID
     * @param receiverId 接收者 ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<OrganizationMessage> findByOrganizationIdAndReceiverId(
            String organizationId, String receiverId, int limit);

    /**
     * 获取会话消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<OrganizationMessage> findBySessionId(String sessionId);

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
     * 删除组织所有消息
     *
     * @param organizationId 组织 ID
     */
    void deleteByOrganizationId(String organizationId);
}

package org.dragon.organization.communication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dragon.organization.OrganizationRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CommunicationService 通讯服务
 * 提供组织内部的消息传递和协作会话管理
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationService {

    private final MessageStore messageStore;
    private final SessionStore sessionStore;
    private final OrganizationRegistry organizationRegistry;

    /**
     * 发送消息
     *
     * @param message 消息
     * @return 含 ID 的消息
     */
    public OrganizationMessage sendMessage(OrganizationMessage message) {
        // 验证组织存在
        organizationRegistry.get(message.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization not found: " + message.getOrganizationId()));

        // 生成 ID
        if (message.getId() == null || message.getId().isEmpty()) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        messageStore.save(message);
        log.info("[CommunicationService] Sent message {} in organization {}",
                message.getId(), message.getOrganizationId());

        return message;
    }

    /**
     * 获取消息
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<OrganizationMessage> getMessages(String organizationId, String characterId, int limit) {
        return messageStore.findByOrganizationIdAndReceiverId(organizationId, characterId, limit);
    }

    /**
     * 创建协作会话
     *
     * @param organizationId 组织 ID
     * @param participantIds 参与者 ID 列表
     * @param taskId 关联任务 ID
     * @return 创建的会话
     */
    public CollaborationSession createSession(String organizationId,
            List<String> participantIds, String taskId) {
        // 验证组织存在
        organizationRegistry.get(organizationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization not found: " + organizationId));

        CollaborationSession session = CollaborationSession.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(organizationId)
                .taskId(taskId)
                .participantIds(participantIds)
                .context(java.util.Collections.emptyMap())
                .decisions(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(CollaborationSession.Status.ACTIVE)
                .build();

        sessionStore.save(session);
        log.info("[CommunicationService] Created session {} in organization {}",
                session.getId(), organizationId);

        return session;
    }

    /**
     * 添加参与者到会话
     *
     * @param sessionId 会话 ID
     * @param characterId Character ID
     */
    public void addToSession(String sessionId, String characterId) {
        CollaborationSession session = sessionStore.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (!session.getParticipantIds().contains(characterId)) {
            session.getParticipantIds().add(characterId);
            session.setUpdatedAt(LocalDateTime.now());
            sessionStore.update(session);
        }
    }

    /**
     * 记录决策
     *
     * @param sessionId 会话 ID
     * @param decision 决策记录
     */
    public void recordDecision(String sessionId, CollaborationSession.DecisionRecord decision) {
        CollaborationSession session = sessionStore.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (decision.getId() == null || decision.getId().isEmpty()) {
            decision.setId(UUID.randomUUID().toString());
        }
        if (decision.getTimestamp() == null) {
            decision.setTimestamp(LocalDateTime.now());
        }

        session.getDecisions().add(decision);
        session.setUpdatedAt(LocalDateTime.now());
        sessionStore.update(session);
    }

    /**
     * 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话
     */
    public CollaborationSession getSession(String sessionId) {
        return sessionStore.findById(sessionId);
    }

    /**
     * 完成会话
     *
     * @param sessionId 会话 ID
     */
    public void completeSession(String sessionId) {
        CollaborationSession session = sessionStore.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        session.setStatus(CollaborationSession.Status.COMPLETED);
        session.setUpdatedAt(LocalDateTime.now());
        sessionStore.update(session);
    }

    /**
     * 获取任务关联的会话
     *
     * @param taskId 任务 ID
     * @return 会话
     */
    public CollaborationSession getSessionByTaskId(String taskId) {
        return sessionStore.findByTaskId(taskId);
    }
}

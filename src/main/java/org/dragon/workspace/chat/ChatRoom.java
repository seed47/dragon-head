package org.dragon.workspace.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ChatRoom 聊天室服务
 * 提供工作空间内的消息传递和协作会话管理
 * 会记录所有 Character 之间的沟通信息
 * 实现 ChatRoomObserver 接口供 Observer 使用
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoom implements ChatRoomObserver {

    private final ChatMessageStore messageStore;
    private final ChatSessionStore sessionStore;
    private final WorkspaceRegistry workspaceRegistry;

    // ==================== 消息功能 ====================

    /**
     * 发送消息
     *
     * @param message 消息
     * @return 含 ID 的消息
     */
    public ChatMessage sendMessage(ChatMessage message) {
        // 验证工作空间存在
        workspaceRegistry.get(message.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workspace not found: " + message.getWorkspaceId()));

        // 生成 ID
        if (message.getId() == null || message.getId().isEmpty()) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        messageStore.save(message);
        log.info("[ChatRoom] Sent message {} in workspace {}",
                message.getId(), message.getWorkspaceId());

        return message;
    }

    /**
     * 获取消息
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<ChatMessage> getMessages(String workspaceId, String characterId, int limit) {
        return messageStore.findByWorkspaceIdAndReceiverId(workspaceId, characterId, limit);
    }

    /**
     * 获取工作空间内所有消息（用于 Observer）
     *
     * @param workspaceId 工作空间 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息列表
     */
    public List<ChatMessage> getAllMessages(String workspaceId, LocalDateTime startTime, LocalDateTime endTime) {
        return messageStore.findByWorkspaceIdAndTimeRange(workspaceId, startTime, endTime);
    }

    /**
     * 获取特定 Character 发送或接收的消息（用于 Observer）
     *
     * @param characterId Character ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<ChatMessage> getMessagesByCharacter(String characterId, int limit) {
        return messageStore.findByCharacterId(characterId, limit);
    }

    // ==================== 会话功能 ====================

    /**
     * 创建协作会话
     *
     * @param workspaceId 工作空间 ID
     * @param participantIds 参与者 ID 列表
     * @param taskId 关联任务 ID
     * @return 创建的会话
     */
    public ChatSession createSession(String workspaceId,
            List<String> participantIds, String taskId) {
        // 验证工作空间存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workspace not found: " + workspaceId));

        ChatSession session = ChatSession.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .taskId(taskId)
                .participantIds(participantIds)
                .context(java.util.Collections.emptyMap())
                .decisions(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(ChatSession.Status.ACTIVE)
                .build();

        sessionStore.save(session);
        log.info("[ChatRoom] Created session {} in workspace {}",
                session.getId(), workspaceId);

        return session;
    }

    /**
     * 添加参与者到会话
     *
     * @param sessionId 会话 ID
     * @param characterId Character ID
     */
    public void addToSession(String sessionId, String characterId) {
        ChatSession session = sessionStore.findById(sessionId);
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
    public void recordDecision(String sessionId, ChatSession.DecisionRecord decision) {
        ChatSession session = sessionStore.findById(sessionId);
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
    public ChatSession getSession(String sessionId) {
        return sessionStore.findById(sessionId);
    }

    /**
     * 获取会话中的所有消息（用于 Observer）
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    public List<ChatMessage> getSessionMessages(String sessionId) {
        return messageStore.findBySessionId(sessionId);
    }

    /**
     * 完成会话
     *
     * @param sessionId 会话 ID
     */
    public void completeSession(String sessionId) {
        ChatSession session = sessionStore.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        session.setStatus(ChatSession.Status.COMPLETED);
        session.setUpdatedAt(LocalDateTime.now());
        sessionStore.update(session);
    }

    /**
     * 获取任务关联的会话
     *
     * @param taskId 任务 ID
     * @return 会话
     */
    public ChatSession getSessionByTaskId(String taskId) {
        return sessionStore.findByTaskId(taskId);
    }

    // ==================== 任务协作方法 ====================

    /**
     * 开始任务协作会话
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param participantIds 参与者 ID 列表
     * @return 创建的协作会话
     */
    public ChatSession startTaskCollaboration(String workspaceId, String taskId, List<String> participantIds) {
        log.info("[ChatRoom] Starting task collaboration for task {} in workspace {}", taskId, workspaceId);

        // 检查是否已存在该任务的协作会话
        ChatSession existingSession = sessionStore.findByTaskId(taskId);
        if (existingSession != null) {
            log.info("[ChatRoom] Task {} already has collaboration session {}", taskId, existingSession.getId());
            return existingSession;
        }

        return createSession(workspaceId, participantIds, taskId);
    }

    /**
     * 发送任务消息
     *
     * @param message 消息（需包含 taskId）
     * @return 含 ID 的消息
     */
    public ChatMessage sendTaskMessage(ChatMessage message) {
        if (message.getTaskId() == null) {
            throw new IllegalArgumentException("taskId is required for task messages");
        }

        // 确保消息类型为 TASK
        if (message.getMessageType() == ChatMessage.MessageType.TEXT) {
            message.setMessageType(ChatMessage.MessageType.TASK);
        }

        return sendMessage(message);
    }

    /**
     * 发送任务状态更新消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param senderId 发送者 ID
     * @param status 新状态
     * @param content 更新内容
     * @return 消息
     */
    public ChatMessage sendTaskUpdateMessage(String workspaceId, String taskId,
            String senderId, String status, String content) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(content)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_UPDATE)
                .taskResultStatus(status)
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 发送任务分配消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param parentTaskId 父任务 ID
     * @param senderId 发送者 ID
     * @param assigneeId 被分配者 ID
     * @param content 分配说明
     * @return 消息
     */
    public ChatMessage sendTaskAssignmentMessage(String workspaceId, String taskId,
            String parentTaskId, String senderId, String assigneeId, String content) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .receiverId(assigneeId)
                .taskId(taskId)
                .parentTaskId(parentTaskId)
                .content(content)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_ASSIGNMENT)
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 发送任务结果消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param senderId 发送者 ID
     * @param resultStatus 结果状态（COMPLETED/FAILED）
     * @param resultContent 结果内容
     * @return 消息
     */
    public ChatMessage sendTaskResultMessage(String workspaceId, String taskId,
            String senderId, String resultStatus, String resultContent) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(resultContent)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_RESULT)
                .taskResultStatus(resultStatus)
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 发送任务阻塞消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param senderId 发送者 ID
     * @param reason 阻塞原因
     * @return 消息
     */
    public ChatMessage sendTaskBlockedMessage(String workspaceId, String taskId,
            String senderId, String reason) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(reason)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_BLOCKED)
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 发送任务完成通知
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param senderId 发送者 ID
     * @param summary 完成摘要
     * @return 消息
     */
    public ChatMessage sendTaskCompleteMessage(String workspaceId, String taskId,
            String senderId, String summary) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(summary)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_COMPLETE)
                .taskResultStatus("COMPLETED")
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 获取任务协作历史
     *
     * @param taskId 任务 ID
     * @return 消息列表
     */
    public List<ChatMessage> getTaskCollaborationHistory(String taskId) {
        ChatSession session = sessionStore.findByTaskId(taskId);
        if (session == null) {
            return new ArrayList<>();
        }
        return messageStore.findBySessionId(session.getId());
    }

    /**
     * 获取任务的所有消息（不限于会话）
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @return 任务相关的消息列表
     */
    public List<ChatMessage> getTaskMessages(String workspaceId, String taskId) {
        return messageStore.findByTaskId(taskId);
    }

    /**
     * 通知任务协作完成
     *
     * @param taskId 任务 ID
     */
    public void notifyTaskCollaborationComplete(String taskId) {
        ChatSession session = sessionStore.findByTaskId(taskId);
        if (session != null) {
            completeSession(session.getId());
            log.info("[ChatRoom] Task collaboration {} completed", taskId);
        }
    }
}

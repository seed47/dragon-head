package org.dragon.workspace.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * MemoryChatMessageStore 聊天消息内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryChatMessageStore implements ChatMessageStore {

    private final Map<String, ChatMessage> messages = new ConcurrentHashMap<>();

    @Override
    public void save(ChatMessage message) {
        if (message == null || message.getId() == null) {
            throw new IllegalArgumentException("Message or Message id cannot be null");
        }
        messages.put(message.getId(), message);
    }

    @Override
    public ChatMessage findById(String messageId) {
        return messages.get(messageId);
    }

    @Override
    public List<ChatMessage> findByWorkspaceId(String workspaceId, int limit) {
        return messages.values().stream()
                .filter(m -> m.getWorkspaceId().equals(workspaceId))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findByWorkspaceIdAndTimeRange(
            String workspaceId, LocalDateTime startTime, LocalDateTime endTime) {
        return messages.values().stream()
                .filter(m -> m.getWorkspaceId().equals(workspaceId)
                        && m.getTimestamp() != null
                        && !m.getTimestamp().isBefore(startTime)
                        && !m.getTimestamp().isAfter(endTime))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findByWorkspaceIdAndReceiverId(
            String workspaceId, String receiverId, int limit) {
        return messages.values().stream()
                .filter(m -> m.getWorkspaceId().equals(workspaceId)
                        && (m.getReceiverId() == null || m.getReceiverId().equals(receiverId)))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findByCharacterId(String characterId, int limit) {
        return messages.values().stream()
                .filter(m -> m.getSenderId().equals(characterId)
                        || (m.getReceiverId() != null && m.getReceiverId().equals(characterId)))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findBySessionId(String sessionId) {
        return messages.values().stream()
                .filter(m -> sessionId.equals(m.getSessionId()))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(String messageId) {
        ChatMessage message = messages.get(messageId);
        if (message != null) {
            message.setRead(true);
        }
    }

    @Override
    public void delete(String messageId) {
        messages.remove(messageId);
    }

    @Override
    public void deleteByWorkspaceId(String workspaceId) {
        List<String> keysToRemove = messages.keySet().stream()
                .filter(key -> messages.get(key).getWorkspaceId().equals(workspaceId))
                .collect(Collectors.toList());
        keysToRemove.forEach(messages::remove);
    }

    @Override
    public List<ChatMessage> findByTaskId(String taskId) {
        return messages.values().stream()
                .filter(m -> taskId.equals(m.getTaskId()))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());
    }
}

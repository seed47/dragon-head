package org.dragon.workspace.chat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * MemoryChatSessionStore 聊天会话内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryChatSessionStore implements ChatSessionStore {

    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(ChatSession session) {
        if (session == null || session.getId() == null) {
            throw new IllegalArgumentException("Session or Session id cannot be null");
        }
        sessions.put(session.getId(), session);
    }

    @Override
    public ChatSession findById(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public List<ChatSession> findByWorkspaceId(String workspaceId) {
        return sessions.values().stream()
                .filter(s -> s.getWorkspaceId().equals(workspaceId))
                .collect(Collectors.toList());
    }

    @Override
    public ChatSession findByTaskId(String taskId) {
        return sessions.values().stream()
                .filter(s -> taskId.equals(s.getTaskId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<ChatSession> findActiveByWorkspaceId(String workspaceId) {
        return sessions.values().stream()
                .filter(s -> s.getWorkspaceId().equals(workspaceId)
                        && s.getStatus() == ChatSession.Status.ACTIVE)
                .collect(Collectors.toList());
    }

    @Override
    public void update(ChatSession session) {
        if (session == null || session.getId() == null) {
            throw new IllegalArgumentException("Session or Session id cannot be null");
        }
        sessions.put(session.getId(), session);
    }

    @Override
    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }
}

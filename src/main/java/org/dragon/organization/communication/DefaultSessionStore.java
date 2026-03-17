package org.dragon.organization.communication;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * DefaultSessionStore 默认协作会话存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class DefaultSessionStore implements SessionStore {

    private final Map<String, CollaborationSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(CollaborationSession session) {
        if (session == null || session.getId() == null) {
            throw new IllegalArgumentException("Session or Session id cannot be null");
        }
        sessions.put(session.getId(), session);
    }

    @Override
    public CollaborationSession findById(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public List<CollaborationSession> findByOrganizationId(String organizationId) {
        return sessions.values().stream()
                .filter(s -> s.getOrganizationId().equals(organizationId))
                .collect(Collectors.toList());
    }

    @Override
    public CollaborationSession findByTaskId(String taskId) {
        return sessions.values().stream()
                .filter(s -> taskId.equals(s.getTaskId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<CollaborationSession> findActiveByOrganizationId(String organizationId) {
        return sessions.values().stream()
                .filter(s -> s.getOrganizationId().equals(organizationId)
                        && s.getStatus() == CollaborationSession.Status.ACTIVE)
                .collect(Collectors.toList());
    }

    @Override
    public void update(CollaborationSession session) {
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

package org.dragon.organization.communication;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * DefaultMessageStore 默认消息存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class DefaultMessageStore implements MessageStore {

    private final Map<String, OrganizationMessage> messages = new ConcurrentHashMap<>();

    @Override
    public void save(OrganizationMessage message) {
        if (message == null || message.getId() == null) {
            throw new IllegalArgumentException("Message or Message id cannot be null");
        }
        messages.put(message.getId(), message);
    }

    @Override
    public OrganizationMessage findById(String messageId) {
        return messages.get(messageId);
    }

    @Override
    public List<OrganizationMessage> findByOrganizationId(String organizationId, int limit) {
        return messages.values().stream()
                .filter(m -> m.getOrganizationId().equals(organizationId))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrganizationMessage> findByOrganizationIdAndReceiverId(
            String organizationId, String receiverId, int limit) {
        return messages.values().stream()
                .filter(m -> m.getOrganizationId().equals(organizationId)
                        && (m.getReceiverId() == null || m.getReceiverId().equals(receiverId)))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrganizationMessage> findBySessionId(String sessionId) {
        return messages.values().stream()
                .filter(m -> sessionId.equals(m.getSessionId()))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(String messageId) {
        OrganizationMessage message = messages.get(messageId);
        if (message != null) {
            message.setRead(true);
        }
    }

    @Override
    public void delete(String messageId) {
        messages.remove(messageId);
    }

    @Override
    public void deleteByOrganizationId(String organizationId) {
        List<String> keysToRemove = messages.keySet().stream()
                .filter(key -> messages.get(key).getOrganizationId().equals(organizationId))
                .collect(Collectors.toList());
        keysToRemove.forEach(messages::remove);
    }
}

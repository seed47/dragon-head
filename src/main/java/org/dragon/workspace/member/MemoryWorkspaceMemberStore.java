package org.dragon.workspace.member;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * MemoryWorkspaceMemberStore 工作空间成员内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryWorkspaceMemberStore implements WorkspaceMemberStore {

    /**
     * 成员存储
     * key: memberId (workspaceId_characterId)
     */
    private final Map<String, WorkspaceMember> members = new ConcurrentHashMap<>();

    @Override
    public void save(WorkspaceMember member) {
        if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("Member or Member id cannot be null");
        }
        members.put(member.getId(), member);
    }

    @Override
    public Optional<WorkspaceMember> findById(String memberId) {
        return Optional.ofNullable(members.get(memberId));
    }

    @Override
    public List<WorkspaceMember> findByWorkspaceId(String workspaceId) {
        return members.values().stream()
                .filter(m -> m.getWorkspaceId().equals(workspaceId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<WorkspaceMember> findByWorkspaceIdAndCharacterId(
            String workspaceId, String characterId) {
        String memberId = WorkspaceMember.createId(workspaceId, characterId);
        return findById(memberId);
    }

    @Override
    public List<WorkspaceMember> findByCharacterId(String characterId) {
        return members.values().stream()
                .filter(m -> m.getCharacterId().equals(characterId))
                .collect(Collectors.toList());
    }

    @Override
    public void update(WorkspaceMember member) {
        if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("Member or Member id cannot be null");
        }
        if (!members.containsKey(member.getId())) {
            throw new IllegalArgumentException("Member not found: " + member.getId());
        }
        members.put(member.getId(), member);
    }

    @Override
    public void delete(String memberId) {
        members.remove(memberId);
    }

    @Override
    public void deleteByWorkspaceId(String workspaceId) {
        List<String> keysToRemove = members.keySet().stream()
                .filter(key -> key.startsWith(workspaceId + "_"))
                .collect(Collectors.toList());
        keysToRemove.forEach(members::remove);
    }

    @Override
    public int countByWorkspaceId(String workspaceId) {
        return (int) members.values().stream()
                .filter(m -> m.getWorkspaceId().equals(workspaceId))
                .count();
    }
}

package org.dragon.organization.member;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * DefaultMemberStore 默认组织成员存储实现
 * 使用内存存储
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class DefaultMemberStore implements OrganizationMemberStore {

    /**
     * 成员存储
     * key: memberId (orgId_characterId)
     */
    private final Map<String, OrganizationMember> members = new ConcurrentHashMap<>();

    @Override
    public void save(OrganizationMember member) {
        if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("Member or Member id cannot be null");
        }
        members.put(member.getId(), member);
    }

    @Override
    public Optional<OrganizationMember> findById(String memberId) {
        return Optional.ofNullable(members.get(memberId));
    }

    @Override
    public List<OrganizationMember> findByOrganizationId(String organizationId) {
        return members.values().stream()
                .filter(m -> m.getOrganizationId().equals(organizationId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OrganizationMember> findByOrganizationIdAndCharacterId(
            String organizationId, String characterId) {
        String memberId = OrganizationMember.createId(organizationId, characterId);
        return findById(memberId);
    }

    @Override
    public List<OrganizationMember> findByCharacterId(String characterId) {
        return members.values().stream()
                .filter(m -> m.getCharacterId().equals(characterId))
                .collect(Collectors.toList());
    }

    @Override
    public void update(OrganizationMember member) {
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
    public void deleteByOrganizationId(String organizationId) {
        List<String> keysToRemove = members.keySet().stream()
                .filter(key -> key.startsWith(organizationId + "_"))
                .collect(Collectors.toList());
        keysToRemove.forEach(members::remove);
    }

    @Override
    public int countByOrganizationId(String organizationId) {
        return (int) members.values().stream()
                .filter(m -> m.getOrganizationId().equals(organizationId))
                .count();
    }
}

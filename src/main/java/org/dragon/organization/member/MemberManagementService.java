package org.dragon.organization.member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.dragon.organization.Organization;
import org.dragon.organization.OrganizationRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MemberManagementService 成员管理服务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberManagementService {

    private final OrganizationMemberStore memberStore;
    private final OrganizationRegistry organizationRegistry;

    /**
     * 添加成员到组织
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param role 角色
     * @param layer 层级
     * @return 添加的成员
     */
    public OrganizationMember addMember(String organizationId, String characterId,
            String role, OrganizationMember.Layer layer) {
        // 验证组织存在
        Organization org = organizationRegistry.get(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        // 检查成员是否已存在
        Optional<OrganizationMember> existing = memberStore.findByOrganizationIdAndCharacterId(
                organizationId, characterId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Character already exists in organization: " + characterId);
        }

        // 创建成员
        OrganizationMember member = OrganizationMember.builder()
                .id(OrganizationMember.createId(organizationId, characterId))
                .organizationId(organizationId)
                .characterId(characterId)
                .role(role)
                .layer(layer != null ? layer : OrganizationMember.Layer.NORMAL)
                .weight(org.getDefaultWeight())
                .priority(org.getDefaultPriority())
                .reputation(0)
                .joinAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        memberStore.save(member);
        log.info("[MemberManagementService] Added member {} to organization {}",
                characterId, organizationId);

        return member;
    }

    /**
     * 从组织移除成员
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     */
    public void removeMember(String organizationId, String characterId) {
        String memberId = OrganizationMember.createId(organizationId, characterId);
        memberStore.delete(memberId);
        log.info("[MemberManagementService] Removed member {} from organization {}",
                characterId, organizationId);
    }

    /**
     * 获取组织所有成员
     *
     * @param organizationId 组织 ID
     * @return 成员列表
     */
    public List<OrganizationMember> listMembers(String organizationId) {
        return memberStore.findByOrganizationId(organizationId);
    }

    /**
     * 获取组织特定成员
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @return Optional 成员
     */
    public Optional<OrganizationMember> getMember(String organizationId, String characterId) {
        return memberStore.findByOrganizationIdAndCharacterId(organizationId, characterId);
    }

    /**
     * 更新成员角色
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param role 新角色
     */
    public void updateMemberRole(String organizationId, String characterId, String role) {
        OrganizationMember member = memberStore.findByOrganizationIdAndCharacterId(organizationId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setRole(role);
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);
        log.info("[MemberManagementService] Updated member {} role to {} in organization {}",
                characterId, role, organizationId);
    }

    /**
     * 更新成员标签
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param tags 新标签列表
     */
    public void updateMemberTags(String organizationId, String characterId, List<String> tags) {
        OrganizationMember member = memberStore.findByOrganizationIdAndCharacterId(organizationId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setTags(tags);
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);
        log.info("[MemberManagementService] Updated member {} tags in organization {}",
                characterId, organizationId);
    }

    /**
     * 获取 Character 所属的所有组织
     *
     * @param characterId Character ID
     * @return 组织列表
     */
    public List<Organization> getOrganizationsForCharacter(String characterId) {
        List<OrganizationMember> memberships = memberStore.findByCharacterId(characterId);
        return memberships.stream()
                .map(m -> organizationRegistry.get(m.getOrganizationId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * 更新成员权重
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param weight 新权重
     */
    public void updateMemberWeight(String organizationId, String characterId, double weight) {
        OrganizationMember member = memberStore.findByOrganizationIdAndCharacterId(organizationId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setWeight(weight);
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);
    }

    /**
     * 更新成员优先级
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param priority 新优先级
     */
    public void updateMemberPriority(String organizationId, String characterId, int priority) {
        OrganizationMember member = memberStore.findByOrganizationIdAndCharacterId(organizationId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setPriority(priority);
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);
    }

    /**
     * 更新成员声誉积分
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @param reputationChange 声誉积分变化（正负值）
     */
    public void updateMemberReputation(String organizationId, String characterId, int reputationChange) {
        OrganizationMember member = memberStore.findByOrganizationIdAndCharacterId(organizationId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        int newReputation = member.getReputation() + reputationChange;
        member.setReputation(Math.max(0, newReputation)); // 不允许负值
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);

        log.info("[MemberManagementService] Updated member {} reputation by {} in organization {}, new value: {}",
                characterId, reputationChange, organizationId, member.getReputation());
    }

    /**
     * 获取成员数量
     *
     * @param organizationId 组织 ID
     * @return 成员数量
     */
    public int getMemberCount(String organizationId) {
        return memberStore.countByOrganizationId(organizationId);
    }
}

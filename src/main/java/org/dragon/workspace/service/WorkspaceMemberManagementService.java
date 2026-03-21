package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceMemberManagementService 工作空间成员管理服务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMemberManagementService {

    private final WorkspaceMemberStore memberStore;
    private final WorkspaceRegistry workspaceRegistry;

    /**
     * 添加成员到工作空间
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param role 角色
     * @param layer 层级
     * @return 添加的成员
     */
    public WorkspaceMember addMember(String workspaceId, String characterId,
            String role, WorkspaceMember.Layer layer) {
        // 验证工作空间存在
        Workspace workspace = workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 检查成员是否已存在
        Optional<WorkspaceMember> existing = memberStore.findByWorkspaceIdAndCharacterId(
                workspaceId, characterId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Character already exists in workspace: " + characterId);
        }

        // 创建成员
        WorkspaceMember member = WorkspaceMember.builder()
                .id(WorkspaceMember.createId(workspaceId, characterId))
                .workspaceId(workspaceId)
                .characterId(characterId)
                .role(role)
                .layer(layer != null ? layer : WorkspaceMember.Layer.NORMAL)
                .weight(1.0) // TODO: 从 Workspace 获取默认权重
                .priority(0) // TODO: 从 Workspace 获取默认优先级
                .reputation(0)
                .joinAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        memberStore.save(member);
        log.info("[WorkspaceMemberManagementService] Added member {} to workspace {}",
                characterId, workspaceId);

        return member;
    }

    /**
     * 从工作空间移除成员
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     */
    public void removeMember(String workspaceId, String characterId) {
        String memberId = WorkspaceMember.createId(workspaceId, characterId);
        memberStore.delete(memberId);
        log.info("[WorkspaceMemberManagementService] Removed member {} from workspace {}",
                characterId, workspaceId);
    }

    /**
     * 获取工作空间所有成员
     *
     * @param workspaceId 工作空间 ID
     * @return 成员列表
     */
    public List<WorkspaceMember> listMembers(String workspaceId) {
        return memberStore.findByWorkspaceId(workspaceId);
    }

    /**
     * 获取工作空间特定成员
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @return Optional 成员
     */
    public Optional<WorkspaceMember> getMember(String workspaceId, String characterId) {
        return memberStore.findByWorkspaceIdAndCharacterId(workspaceId, characterId);
    }

    /**
     * 更新成员角色
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param role 新角色
     */
    public void updateMemberRole(String workspaceId, String characterId, String role) {
        WorkspaceMember member = memberStore.findByWorkspaceIdAndCharacterId(workspaceId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setRole(role);
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);
        log.info("[WorkspaceMemberManagementService] Updated member {} role to {} in workspace {}",
                characterId, role, workspaceId);
    }

    /**
     * 更新成员标签
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param tags 新标签列表
     */
    public void updateMemberTags(String workspaceId, String characterId, List<String> tags) {
        WorkspaceMember member = memberStore.findByWorkspaceIdAndCharacterId(workspaceId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setTags(tags);
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);
        log.info("[WorkspaceMemberManagementService] Updated member {} tags in workspace {}",
                characterId, workspaceId);
    }

    /**
     * 获取 Character 所属的所有工作空间
     *
     * @param characterId Character ID
     * @return 工作空间列表
     */
    public List<Workspace> getWorkspacesForCharacter(String characterId) {
        List<WorkspaceMember> memberships = memberStore.findByCharacterId(characterId);
        return memberships.stream()
                .map(m -> workspaceRegistry.get(m.getWorkspaceId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * 更新成员权重
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param weight 新权重
     */
    public void updateMemberWeight(String workspaceId, String characterId, double weight) {
        WorkspaceMember member = memberStore.findByWorkspaceIdAndCharacterId(workspaceId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setWeight(weight);
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);
    }

    /**
     * 更新成员优先级
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param priority 新优先级
     */
    public void updateMemberPriority(String workspaceId, String characterId, int priority) {
        WorkspaceMember member = memberStore.findByWorkspaceIdAndCharacterId(workspaceId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setPriority(priority);
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);
    }

    /**
     * 更新成员声誉积分
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param reputationChange 声誉积分变化（正负值）
     */
    public void updateMemberReputation(String workspaceId, String characterId, int reputationChange) {
        WorkspaceMember member = memberStore.findByWorkspaceIdAndCharacterId(workspaceId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        int newReputation = member.getReputation() + reputationChange;
        member.setReputation(Math.max(0, newReputation)); // 不允许负值
        member.setLastActiveAt(LocalDateTime.now());
        memberStore.update(member);

        log.info("[WorkspaceMemberManagementService] Updated member {} reputation by {} in workspace {}, new value: {}",
                characterId, reputationChange, workspaceId, member.getReputation());
    }

    /**
     * 获取成员数量
     *
     * @param workspaceId 工作空间 ID
     * @return 成员数量
     */
    public int getMemberCount(String workspaceId) {
        return memberStore.countByWorkspaceId(workspaceId);
    }
}

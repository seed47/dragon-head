package org.dragon.workspace.member;

import java.util.List;
import java.util.Optional;

/**
 * WorkspaceMemberStore 工作空间成员存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkspaceMemberStore {

    /**
     * 保存成员
     *
     * @param member 成员
     */
    void save(WorkspaceMember member);

    /**
     * 根据 ID 获取成员
     *
     * @param memberId 成员 ID
     * @return Optional 成员
     */
    Optional<WorkspaceMember> findById(String memberId);

    /**
     * 根据工作空间 ID 获取所有成员
     *
     * @param workspaceId 工作空间 ID
     * @return 成员列表
     */
    List<WorkspaceMember> findByWorkspaceId(String workspaceId);

    /**
     * 根据工作空间 ID 和 Character ID 获取成员
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @return Optional 成员
     */
    Optional<WorkspaceMember> findByWorkspaceIdAndCharacterId(
            String workspaceId, String characterId);

    /**
     * 根据 Character ID 获取所有成员身份
     *
     * @param characterId Character ID
     * @return 成员列表
     */
    List<WorkspaceMember> findByCharacterId(String characterId);

    /**
     * 更新成员
     *
     * @param member 成员
     */
    void update(WorkspaceMember member);

    /**
     * 删除成员
     *
     * @param memberId 成员 ID
     */
    void delete(String memberId);

    /**
     * 根据工作空间 ID 删除所有成员
     *
     * @param workspaceId 工作空间 ID
     */
    void deleteByWorkspaceId(String workspaceId);

    /**
     * 获取工作空间成员数量
     *
     * @param workspaceId 工作空间 ID
     * @return 成员数量
     */
    int countByWorkspaceId(String workspaceId);
}

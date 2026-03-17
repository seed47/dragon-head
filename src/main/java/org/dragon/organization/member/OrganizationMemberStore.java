package org.dragon.organization.member;

import java.util.List;
import java.util.Optional;

/**
 * OrganizationMemberStore 组织成员存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface OrganizationMemberStore {

    /**
     * 保存成员
     *
     * @param member 成员
     */
    void save(OrganizationMember member);

    /**
     * 根据 ID 获取成员
     *
     * @param memberId 成员 ID
     * @return Optional 成员
     */
    Optional<OrganizationMember> findById(String memberId);

    /**
     * 根据组织 ID 获取所有成员
     *
     * @param organizationId 组织 ID
     * @return 成员列表
     */
    List<OrganizationMember> findByOrganizationId(String organizationId);

    /**
     * 根据组织 ID 和 Character ID 获取成员
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @return Optional 成员
     */
    Optional<OrganizationMember> findByOrganizationIdAndCharacterId(
            String organizationId, String characterId);

    /**
     * 根据 Character ID 获取所有成员身份
     *
     * @param characterId Character ID
     * @return 成员列表
     */
    List<OrganizationMember> findByCharacterId(String characterId);

    /**
     * 更新成员
     *
     * @param member 成员
     */
    void update(OrganizationMember member);

    /**
     * 删除成员
     *
     * @param memberId 成员 ID
     */
    void delete(String memberId);

    /**
     * 根据组织 ID 删除所有成员
     *
     * @param organizationId 组织 ID
     */
    void deleteByOrganizationId(String organizationId);

    /**
     * 获取组织成员数量
     *
     * @param organizationId 组织 ID
     * @return 成员数量
     */
    int countByOrganizationId(String organizationId);
}

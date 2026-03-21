package org.dragon.workspace.member;

import java.util.List;
import java.util.Optional;

/**
 * Character Duty 存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface CharacterDutyStore {

    /**
     * 保存职责
     */
    void save(CharacterDuty duty);

    /**
     * 更新职责
     */
    void update(CharacterDuty duty);

    /**
     * 删除职责
     */
    void delete(String id);

    /**
     * 根据 ID 查询
     */
    Optional<CharacterDuty> findById(String id);

    /**
     * 根据 workspaceId 和 characterId 查询
     */
    Optional<CharacterDuty> findByWorkspaceIdAndCharacterId(String workspaceId, String characterId);

    /**
     * 根据 workspaceId 查询所有职责
     */
    List<CharacterDuty> findByWorkspaceId(String workspaceId);
}

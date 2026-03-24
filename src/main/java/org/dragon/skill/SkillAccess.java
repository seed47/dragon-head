package org.dragon.skill;

import java.util.List;

/**
 * 技能访问接口。
 * 提供对 Character 私有技能的统一访问，包括查询、注册、更新和删除操作。
 *
 * @author wyj
 * @version 1.0
 * @since 1.0
 */
public interface SkillAccess {

    /**
     * 获取 Character ID。
     *
     * @return Character ID
     * @since 1.0
     */
    String getCharacterId();

    // ================= 单个查询 (Single Query) =================

    /**
     * 根据 ID 获取技能
     *
     * @param skillId 技能 ID
     * @return 技能
     */
    Skill get(String skillId);

    /**
     * 根据名称查找技能
     *
     * @param name 技能名称
     * @return 技能列表
     */
    List<Skill> findByName(String name);

    /**
     * 根据描述查找技能
     *
     * @param description 描述关键词
     * @return 技能列表
     */
    List<Skill> findByDescription(String description);

    /**
     * 获取技能元数据
     *
     * @param skillId 技能 ID
     * @return 元数据
     */
    Skill.SkillMetadata getMetadata(String skillId);

    // ================= 批量操作 (Batch Operations) =================

    /**
     * 根据 ID 列表批量获取技能
     *
     * @param skillIds 技能 ID 列表
     * @return 技能列表
     */
    List<Skill> findByIds(List<String> skillIds);

    /**
     * 获取所有技能
     *
     * @return 所有技能列表
     */
    List<Skill> findAll();

    /**
     * 根据分类获取技能
     *
     * @param category 分类
     * @return 技能列表
     */
    List<Skill> findByCategory(String category);

    /**
     * 根据标签获取技能
     *
     * @param tags 标签列表
     * @return 技能列表
     */
    List<Skill> findByTags(List<String> tags);

    // ================= 写入操作 (Write Operations) =================

    /**
     * 注册技能
     *
     * @param skill 技能
     */
    void register(Skill skill);

    /**
     * 批量注册技能
     *
     * @param skills 技能列表
     */
    void registerBatch(List<Skill> skills);

    /**
     * 更新技能
     *
     * @param skill 技能
     */
    void update(Skill skill);

    /**
     * 删除技能
     *
     * @param skillId 技能 ID
     */
    void delete(String skillId);

    /**
     * 清空所有技能
     */
    void clear();

    /**
     * 获取技能数量
     *
     * @return 技能数量
     */
    int count();
}

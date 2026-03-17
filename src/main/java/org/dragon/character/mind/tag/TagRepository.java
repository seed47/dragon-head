package org.dragon.character.mind.tag;


import java.util.List;

/**
 * 标签仓储接口
 * 提供标签的增删改查能力
 *
 * @author wyj
 * @version 1.0
 */
public interface TagRepository {

    /**
     * 为指定 Character 添加标签
     *
     * @param targetCharacterId 目标 Character ID
     * @param tag               标签
     */
    void addTag(String targetCharacterId, Tag tag);

    /**
     * 更新指定 Character 的标签
     *
     * @param targetCharacterId 目标 Character ID
     * @param tagName           标签名称
     * @param tag               新标签
     */
    void updateTag(String targetCharacterId, String tagName, Tag tag);

    /**
     * 获取指定 Character 的所有标签
     *
     * @param targetCharacterId 目标 Character ID
     * @return 标签列表
     */
    List<Tag> getTags(String targetCharacterId);

    /**
     * 根据标签过滤 Character
     *
     * @param tagName  标签名称
     * @param tagValue 标签值
     * @return 符合条件的目标 Character ID 列表
     */
    List<String> findByTag(String tagName, String tagValue);

    /**
     * 删除指定 Character 的指定标签
     *
     * @param targetCharacterId 目标 Character ID
     * @param tagName           标签名称
     */
    void removeTag(String targetCharacterId, String tagName);

    /**
     * 删除指定 Character 的所有标签
     *
     * @param targetCharacterId 目标 Character ID
     */
    void clearTags(String targetCharacterId);

    /**
     * 获取 Character 的标签数量
     *
     * @param targetCharacterId 目标 Character ID
     * @return 标签数量
     */
    int getTagCount(String targetCharacterId);
}

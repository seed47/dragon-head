package org.dragon.character.mind;

import org.dragon.character.mind.memory.MemoryAccess;
import org.dragon.character.mind.skill.SkillAccess;
import org.dragon.character.mind.tag.TagRepository;

/**
 * Mind 心智接口
 * 负责 Character 的性格特征、标签、记忆和技能的统一访问
 *
 * @author wyj
 * @version 1.0
 */
public interface Mind {

    /**
     * 获取 Character ID
     *
     * @return Character ID
     */
    String getCharacterId();

    /**
     * 获取性格描述
     *
     * @return PersonalityDescriptor
     */
    PersonalityDescriptor getPersonality();

    /**
     * 加载性格描述文件
     *
     * @param descriptorPath 描述文件路径
     */
    void loadPersonality(String descriptorPath);

    /**
     * 更新性格描述
     *
     * @param descriptor 性格描述
     */
    void updatePersonality(PersonalityDescriptor descriptor);

    /**
     * 获取标签仓储
     *
     * @return TagRepository
     */
    TagRepository getTagRepository();

    /**
     * 获取记忆访问接口
     *
     * @return MemoryAccess
     */
    MemoryAccess getMemoryAccess();

    /**
     * 获取技能访问接口
     *
     * @return SkillAccess
     */
    SkillAccess getSkillAccess();
}

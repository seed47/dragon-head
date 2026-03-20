package org.dragon.character.mind;

import org.dragon.character.mind.memory.MemoryAccess;
import org.dragon.character.mind.skill.SkillAccess;
import org.dragon.character.mind.tag.Tag;
import org.dragon.character.mind.tag.TagRepository;

import java.util.List;
import java.util.Map;

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

    /**
     * 通过 LLM 分析suggestion并增强 personality
     * Observer使用此方法将LLM生成的优化建议应用到Character的心智模型
     *
     * @param suggestions 优化建议列表
     * @return 更新后的 PersonalityDescriptor
     */
    PersonalityDescriptor enhanceByLLM(List<String> suggestions);

    /**
     * 通过 LLM 分析suggestion并更新 tags
     * 用于更新对其他Character的印象、信任度等
     *
     * @param suggestions 优化建议列表
     * @return targetCharacterId -> Tag 的映射，表示需要更新的tag
     */
    Map<String, Tag> enhanceTagsByLLM(List<String> suggestions);
}

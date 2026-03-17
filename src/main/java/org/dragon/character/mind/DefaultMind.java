package org.dragon.character.mind;

import lombok.extern.slf4j.Slf4j;
import org.dragon.character.mind.memory.MemoryAccess;
import org.dragon.character.mind.skill.SkillAccess;
import org.dragon.character.mind.tag.TagRepository;

/**
 * Mind 默认实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
public class DefaultMind implements Mind {

    private final String characterId;

    private PersonalityDescriptor personality;

    private final TagRepository tagRepository;

    private final MemoryAccess memoryAccess;

    private final SkillAccess skillAccess;

    public DefaultMind(String characterId,
                       TagRepository tagRepository,
                       MemoryAccess memoryAccess,
                       SkillAccess skillAccess) {
        this.characterId = characterId;
        this.tagRepository = tagRepository;
        this.memoryAccess = memoryAccess;
        this.skillAccess = skillAccess;
    }

    @Override
    public String getCharacterId() {
        return characterId;
    }

    @Override
    public PersonalityDescriptor getPersonality() {
        return personality;
    }

    @Override
    public void loadPersonality(String descriptorPath) {
        // TODO: 实现从文件加载
        log.info("[Mind] Loading personality from: {}", descriptorPath);
    }

    @Override
    public void updatePersonality(PersonalityDescriptor descriptor) {
        this.personality = descriptor;
        log.info("[Mind] Updated personality for character: {}", characterId);
    }

    @Override
    public TagRepository getTagRepository() {
        return tagRepository;
    }

    @Override
    public MemoryAccess getMemoryAccess() {
        return memoryAccess;
    }

    @Override
    public SkillAccess getSkillAccess() {
        return skillAccess;
    }
}

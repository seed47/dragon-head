package org.dragon.character.mind;

import lombok.extern.slf4j.Slf4j;
import org.dragon.character.mind.memory.MemoryAccess;
import org.dragon.skill.SkillAccess;
import org.dragon.character.mind.tag.Tag;
import org.dragon.character.mind.tag.TagRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public PersonalityDescriptor enhanceByLLM(List<String> suggestions) {
        log.info("[DefaultMind] Enhancing personality with {} suggestions", suggestions.size());

        if (personality == null) {
            log.warn("[DefaultMind] No personality to enhance");
            return null;
        }

        // 简化实现：基于建议关键词更新 personality
        // 实际生产环境应该调用 LLM 来解析 suggestions 并生成更新后的 PersonalityDescriptor
        for (String suggestion : suggestions) {
            String lowerSuggestion = suggestion.toLowerCase();

            // 沟通风格调整
            if (lowerSuggestion.contains("沟通") || lowerSuggestion.contains("communication")) {
                String newStyle = extractStyleFromSuggestion(suggestion);
                if (newStyle != null && personality.getCommunicationStyle() != null) {
                    personality.setCommunicationStyle(newStyle);
                }
            }

            // 决策方式调整
            if (lowerSuggestion.contains("决策") || lowerSuggestion.contains("decision")) {
                String newDecisionStyle = extractDecisionStyleFromSuggestion(suggestion);
                if (newDecisionStyle != null) {
                    personality.setDecisionStyle(newDecisionStyle);
                }
            }

            // 价值观调整
            if (lowerSuggestion.contains("价值") || lowerSuggestion.contains("value")) {
                // 简化处理：记录更新日志
                log.info("[DefaultMind] Suggestion to adjust values: {}", suggestion);
            }
        }

        log.info("[DefaultMind] Personality enhanced successfully");
        return personality;
    }

    @Override
    public Map<String, Tag> enhanceTagsByLLM(List<String> suggestions) {
        log.info("[DefaultMind] Enhancing tags with {} suggestions", suggestions.size());

        Map<String, Tag> tagUpdates = new HashMap<>();

        // 简化实现：解析 suggestions 中关于 tag 的建议
        // 实际生产环境应该调用 LLM 来确定需要更新哪些 tag
        for (String suggestion : suggestions) {
            String lowerSuggestion = suggestion.toLowerCase();

            if (lowerSuggestion.contains("印象") || lowerSuggestion.contains("tag") || lowerSuggestion.contains("信任")) {
                // 解析 target character 和 tag 信息
                // 这是一个简化实现
                log.info("[DefaultMind] Tag suggestion: {}", suggestion);
            }
        }

        return tagUpdates;
    }

    /**
     * 从建议中提取沟通风格
     */
    private String extractStyleFromSuggestion(String suggestion) {
        String lower = suggestion.toLowerCase();
        if (lower.contains("正式") || lower.contains("formal")) return "formal";
        if (lower.contains("非正式") || lower.contains("informal")) return "informal";
        if (lower.contains("简洁") || lower.contains("concise")) return "concise";
        if (lower.contains("详细") || lower.contains("detailed")) return "detailed";
        return null;
    }

    /**
     * 从建议中提取决策风格
     */
    private String extractDecisionStyleFromSuggestion(String suggestion) {
        String lower = suggestion.toLowerCase();
        if (lower.contains("快速") || lower.contains("quick")) return "quick";
        if (lower.contains("谨慎") || lower.contains("cautious")) return "cautious";
        if (lower.contains("民主") || lower.contains("democratic")) return "democratic";
        if (lower.contains("集中") || lower.contains("centralized")) return "centralized";
        return null;
    }
}

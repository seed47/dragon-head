package org.dragon.organization;

import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.organization.personality.OrganizationPersonality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OrganizationEnhancer 组织LLM增强器
 * 通过 LLM 分析优化建议并更新 Organization 的配置
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class OrganizationEnhancer {

    private static final Logger log = LoggerFactory.getLogger(OrganizationEnhancer.class);

    private final LLMCaller llmCaller;

    @Value("${dragon.observer.llm.enhancement.model:claude-sonnet-4-6}")
    private String enhancementModelId;

    @Value("${dragon.observer.llm.enhancement.temperature:0.7}")
    private Double temperature;

    @Value("${dragon.observer.llm.enhancement.max-tokens:2000}")
    private Integer maxTokens;

    public OrganizationEnhancer(LLMCaller llmCaller) {
        this.llmCaller = llmCaller;
    }

    /**
     * 通过 LLM 分析suggestion并增强 OrganizationPersonality
     *
     * @param organization 当前Organization
     * @param suggestions 优化建议列表
     * @return 更新后的 OrganizationPersonality
     */
    public OrganizationPersonality enhanceByLLM(Organization organization, List<String> suggestions) {
        log.info("[OrganizationEnhancer] Enhancing organization personality with {} suggestions", suggestions.size());

        OrganizationPersonality personality = organization.getPersonality();
        if (personality == null) {
            personality = OrganizationPersonality.builder().build();
        }

        // 简化实现：基于建议关键词更新 personality
        // 实际生产环境应该调用 LLM 来解析 suggestions 并生成更新后的 OrganizationPersonality
        for (String suggestion : suggestions) {
            String lowerSuggestion = suggestion.toLowerCase();

            // 工作风格调整
            if (lowerSuggestion.contains("工作风格") || lowerSuggestion.contains("working style")) {
                String newStyle = extractWorkingStyleFromSuggestion(suggestion);
                if (newStyle != null) {
                    try {
                        personality.setWorkingStyle(OrganizationPersonality.WorkingStyle.valueOf(newStyle.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("[OrganizationEnhancer] Invalid working style: {}", newStyle);
                    }
                }
            }

            // 风险偏好调整
            if (lowerSuggestion.contains("风险") || lowerSuggestion.contains("risk")) {
                Double newRiskTolerance = extractRiskToleranceFromSuggestion(suggestion);
                if (newRiskTolerance != null) {
                    personality.setRiskTolerance(newRiskTolerance);
                }
            }

            // 决策模式调整
            if (lowerSuggestion.contains("决策") || lowerSuggestion.contains("decision")) {
                String newDecisionPattern = extractDecisionPatternFromSuggestion(suggestion);
                if (newDecisionPattern != null) {
                    try {
                        personality.setDecisionPattern(OrganizationPersonality.DecisionPattern.valueOf(newDecisionPattern.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("[OrganizationEnhancer] Invalid decision pattern: {}", newDecisionPattern);
                    }
                }
            }

            // 协作偏好调整
            if (lowerSuggestion.contains("协作") || lowerSuggestion.contains("collaboration")) {
                log.info("[OrganizationEnhancer] Suggestion to adjust collaboration: {}", suggestion);
            }
        }

        log.info("[OrganizationEnhancer] Organization personality enhanced successfully");
        return personality;
    }

    /**
     * 通过 LLM 分析suggestion并更新沟通方式
     *
     * @param suggestions 优化建议列表
     * @return 建议的沟通方式更新
     */
    public CommunicationConfig enhanceCommunicationByLLM(List<String> suggestions) {
        log.info("[OrganizationEnhancer] Enhancing communication config with {} suggestions", suggestions.size());

        CommunicationConfig config = new CommunicationConfig();

        for (String suggestion : suggestions) {
            String lowerSuggestion = suggestion.toLowerCase();

            if (lowerSuggestion.contains("沟通") || lowerSuggestion.contains("communication")) {
                // 解析沟通方式建议
                if (lowerSuggestion.contains("正式")) {
                    config.setFormal(true);
                } else if (lowerSuggestion.contains("非正式")) {
                    config.setFormal(false);
                }

                if (lowerSuggestion.contains("简洁")) {
                    config.setConcise(true);
                }

                if (lowerSuggestion.contains("详细")) {
                    config.setDetailed(true);
                }
            }
        }

        return config;
    }

    /**
     * 从建议中提取工作风格
     */
    private String extractWorkingStyleFromSuggestion(String suggestion) {
        String lower = suggestion.toLowerCase();
        if (lower.contains("激进") || lower.contains("aggressive")) return "AGGRESSIVE";
        if (lower.contains("保守") || lower.contains("conservative")) return "CONSERVATIVE";
        if (lower.contains("协作") || lower.contains("collaborative")) return "COLLABORATIVE";
        if (lower.contains("创新") || lower.contains("innovative")) return "INNOVATIVE";
        if (lower.contains("分析") || lower.contains("analytical")) return "ANALYTICAL";
        return null;
    }

    /**
     * 从建议中提取风险偏好
     */
    private Double extractRiskToleranceFromSuggestion(String suggestion) {
        String lower = suggestion.toLowerCase();
        // 提取数字
        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(lower);
        if (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                if (value >= 0 && value <= 1) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * 从建议中提取决策模式
     */
    private String extractDecisionPatternFromSuggestion(String suggestion) {
        String lower = suggestion.toLowerCase();
        if (lower.contains("民主") || lower.contains("democratic")) return "DEMOCRATIC";
        if (lower.contains("集中") || lower.contains("autocratic")) return "AUTOCRATIC";
        if (lower.contains("共识") || lower.contains("consensus")) return "CONSENSUS";
        if (lower.contains("咨询") || lower.contains("consultative")) return "CONSULTATIVE";
        return null;
    }

    /**
     * 沟通配置
     */
    public static class CommunicationConfig {
        private boolean formal;
        private boolean concise;
        private boolean detailed;
        private String preferredFormat;

        public boolean isFormal() { return formal; }
        public void setFormal(boolean formal) { this.formal = formal; }
        public boolean isConcise() { return concise; }
        public void setConcise(boolean concise) { this.concise = concise; }
        public boolean isDetailed() { return detailed; }
        public void setDetailed(boolean detailed) { this.detailed = detailed; }
        public String getPreferredFormat() { return preferredFormat; }
        public void setPreferredFormat(String preferredFormat) { this.preferredFormat = preferredFormat; }
    }
}

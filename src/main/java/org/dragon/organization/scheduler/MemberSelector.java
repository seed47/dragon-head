package org.dragon.organization.scheduler;

import org.dragon.organization.member.OrganizationMember;
import org.dragon.organization.task.OrganizationTask;

import java.util.List;
import java.util.Map;

/**
 * MemberSelector 成员选择器接口
 * 使用 LLM 根据 Mind 和历史记录智能选择成员
 *
 * @author wyj
 * @version 1.0
 */
public interface MemberSelector {

    /**
     * 使用 LLM 根据 Mind 和历史记录选择最合适的成员
     *
     * @param organizationId 组织 ID
     * @param task 任务
     * @param candidates 候选成员列表
     * @return 排序后的成员列表（含评分和理由）
     */
    List<SelectedMember> selectWithLLM(
            String organizationId,
            OrganizationTask task,
            List<OrganizationMember> candidates
    );

    /**
     * 获取候选成员的 Mind 信息
     *
     * @param characterIds Character ID 列表
     * @return 成员 Mind 信息映射
     */
    Map<String, MemberMindProfile> getMemberMindProfiles(
            List<String> characterIds
    );

    /**
     * 获取候选成员的历史表现
     *
     * @param organizationId 组织 ID
     * @param characterIds Character ID 列表
     * @param recentTasksLimit 最近任务数量限制
     * @return 成员历史记录映射
     */
    Map<String, MemberHistoryRecord> getMemberHistory(
            String organizationId,
            List<String> characterIds,
            int recentTasksLimit
    );

    /**
     * MemberMindProfile 成员 Mind 信息
     */
    class MemberMindProfile {
        private String characterId;
        private String name;
        private String personality; // 性格描述
        private List<String> capabilities; // 能力列表
        private List<String> strengths; // 优势领域
        private List<String> weaknesses; // 劣势领域
        private java.util.Map<String, Object> customAttributes;

        // Getters and Setters
        public String getCharacterId() { return characterId; }
        public void setCharacterId(String characterId) { this.characterId = characterId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPersonality() { return personality; }
        public void setPersonality(String personality) { this.personality = personality; }
        public List<String> getCapabilities() { return capabilities; }
        public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
        public List<String> getStrengths() { return strengths; }
        public void setStrengths(List<String> strengths) { this.strengths = strengths; }
        public List<String> getWeaknesses() { return weaknesses; }
        public void setWeaknesses(List<String> weaknesses) { this.weaknesses = weaknesses; }
        public java.util.Map<String, Object> getCustomAttributes() { return customAttributes; }
        public void setCustomAttributes(java.util.Map<String, Object> customAttributes) { this.customAttributes = customAttributes; }
    }

    /**
     * MemberHistoryRecord 成员历史记录
     */
    class MemberHistoryRecord {
        private String characterId;
        private int totalTasks;
        private int successTasks;
        private double successRate;
        private double avgQualityScore;
        private long avgDurationMs;
        private List<String> recentTaskTypes;
        private java.util.Map<String, Integer> collaborationCount; // 与其他成员的协作次数

        // Getters and Setters
        public String getCharacterId() { return characterId; }
        public void setCharacterId(String characterId) { this.characterId = characterId; }
        public int getTotalTasks() { return totalTasks; }
        public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
        public int getSuccessTasks() { return successTasks; }
        public void setSuccessTasks(int successTasks) { this.successTasks = successTasks; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public double getAvgQualityScore() { return avgQualityScore; }
        public void setAvgQualityScore(double avgQualityScore) { this.avgQualityScore = avgQualityScore; }
        public long getAvgDurationMs() { return avgDurationMs; }
        public void setAvgDurationMs(long avgDurationMs) { this.avgDurationMs = avgDurationMs; }
        public List<String> getRecentTaskTypes() { return recentTaskTypes; }
        public void setRecentTaskTypes(List<String> recentTaskTypes) { this.recentTaskTypes = recentTaskTypes; }
        public java.util.Map<String, Integer> getCollaborationCount() { return collaborationCount; }
        public void setCollaborationCount(java.util.Map<String, Integer> collaborationCount) { this.collaborationCount = collaborationCount; }
    }

    /**
     * SelectedMember 选中的成员
     */
    class SelectedMember {
        private String characterId;
        private double score;
        private String reasoning; // LLM 选择理由
        private List<String> recommendedActions; // 推荐执行的动作

        // Getters and Setters
        public String getCharacterId() { return characterId; }
        public void setCharacterId(String characterId) { this.characterId = characterId; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
        public List<String> getRecommendedActions() { return recommendedActions; }
        public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }
    }
}

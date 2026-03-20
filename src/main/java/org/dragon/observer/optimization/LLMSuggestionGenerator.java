package org.dragon.observer.optimization;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.mind.Mind;
import org.dragon.character.mind.PersonalityDescriptor;
import org.dragon.character.mind.tag.Tag;
import org.dragon.config.PromptKeys;
import org.dragon.config.PromptManager;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.evaluation.EvaluationEngine;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.EvaluationRecordStore;
import org.dragon.organization.Organization;
import org.dragon.organization.OrganizationRegistry;
import org.dragon.organization.personality.OrganizationPersonality;
import org.dragon.util.GsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLMSuggestionGenerator LLM优化建议生成器
 * 通过 LLM 分析任务执行数据，生成优化建议
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class LLMSuggestionGenerator {

    private static final Logger log = LoggerFactory.getLogger(LLMSuggestionGenerator.class);

    private final LLMCaller llmCaller;
    private final DataCollector dataCollector;
    private final CharacterRegistry characterRegistry;
    private final OrganizationRegistry organizationRegistry;
    private final EvaluationRecordStore evaluationRecordStore;
    private final PromptManager promptManager;

    @Value("${dragon.observer.llm.suggestion.model:claude-sonnet-4-6}")
    private String suggestionModelId;

    @Value("${dragon.observer.llm.suggestion.temperature:0.7}")
    private Double temperature;

    @Value("${dragon.observer.llm.suggestion.max-tokens:2000}")
    private Integer maxTokens;

    /**
     * 生成优化建议
     *
     * @param workspace         工作空间ID
     * @param organizationId    组织ID (可为空)
     * @param targetType        目标类型
     * @param targetId          目标ID
     * @param recentTasksCount  考虑最近N个任务
     * @return 优化建议列表
     */
    public List<String> generateSuggestions(
            String workspace,
            String organizationId,
            OptimizationAction.TargetType targetType,
            String targetId,
            int recentTasksCount) {

        log.info("[LLMSuggestionGenerator] Generating suggestions for {}: {} in workspace: {}",
                targetType, targetId, workspace);

        // 1. 收集目标当前状态
        Map<String, Object> currentState = collectCurrentState(targetType, targetId);

        // 2. 收集最近任务数据
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7); // 默认考虑最近7天
        List<EvaluationEngine.TaskData> recentTasks = collectRecentTasks(targetType, targetId, startTime, endTime, recentTasksCount);

        // 3. 收集历史评估记录
        List<EvaluationRecord> evaluationRecords = collectEvaluationRecords(targetType, targetId, startTime, endTime);

        // 4. 构建 prompt
        String prompt = buildPrompt(targetType, targetId, currentState, recentTasks, evaluationRecords);

        // 5. 调用 LLM
        List<String> suggestions = callLLM(workspace, organizationId, targetId, prompt);

        log.info("[LLMSuggestionGenerator] Generated {} suggestions for {}: {}",
                suggestions.size(), targetType, targetId);

        return suggestions;
    }

    /**
     * 收集目标当前状态
     */
    private Map<String, Object> collectCurrentState(OptimizationAction.TargetType targetType, String targetId) {
        Map<String, Object> state = new HashMap<>();

        switch (targetType) {
            case CHARACTER:
                return collectCharacterState(targetId);
            case ORGANIZATION:
                return collectOrganizationState(targetId);
            default:
                return state;
        }
    }

    /**
     * 收集 Character 当前状态
     */
    private Map<String, Object> collectCharacterState(String characterId) {
        Map<String, Object> state = new HashMap<>();

        Character character = characterRegistry.get(characterId).orElse(null);
        if (character == null) {
            log.warn("[LLMSuggestionGenerator] Character not found: {}", characterId);
            return state;
        }

        state.put("id", character.getId());
        state.put("name", character.getName());
        state.put("description", character.getDescription());

        Mind mind = character.getMind();
        if (mind != null) {
            PersonalityDescriptor personality = mind.getPersonality();
            if (personality != null) {
                state.put("personality", personality);
            }

            // 获取 tags
            if (mind.getTagRepository() != null) {
                List<Tag> tags = mind.getTagRepository().getTags(characterId);
                state.put("tags", tags);
            }
        }

        return state;
    }

    /**
     * 收集 Organization 当前状态
     */
    private Map<String, Object> collectOrganizationState(String orgId) {
        Map<String, Object> state = new HashMap<>();

        Organization organization = organizationRegistry.get(orgId).orElse(null);
        if (organization == null) {
            log.warn("[LLMSuggestionGenerator] Organization not found: {}", orgId);
            return state;
        }

        state.put("id", organization.getId());
        state.put("name", organization.getName());
        state.put("description", organization.getDescription());
        state.put("properties", organization.getProperties());
        state.put("advantages", organization.getAdvantages());

        OrganizationPersonality personality = organization.getPersonality();
        if (personality != null) {
            state.put("personality", personality);
        }

        return state;
    }

    /**
     * 收集最近任务数据
     */
    private List<EvaluationEngine.TaskData> collectRecentTasks(
            OptimizationAction.TargetType targetType,
            String targetId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int recentTasksCount) {

        List<EvaluationEngine.TaskData> allTasks;

        switch (targetType) {
            case CHARACTER:
                allTasks = dataCollector.collectCharacterTaskData(targetId, startTime, endTime);
                break;
            case ORGANIZATION:
                allTasks = dataCollector.collectOrganizationTaskData(targetId, startTime, endTime);
                break;
            default:
                return new ArrayList<>();
        }

        // 取最近的 N 个任务
        int size = Math.min(allTasks.size(), recentTasksCount);
        return allTasks.subList(Math.max(0, allTasks.size() - size), allTasks.size());
    }

    /**
     * 收集历史评估记录
     */
    private List<EvaluationRecord> collectEvaluationRecords(
            OptimizationAction.TargetType targetType,
            String targetId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        EvaluationRecord.TargetType evalTargetType = targetType == OptimizationAction.TargetType.CHARACTER
                ? EvaluationRecord.TargetType.CHARACTER
                : EvaluationRecord.TargetType.ORGANIZATION;

        return evaluationRecordStore.findByTargetAndTimeRange(evalTargetType, targetId, startTime, endTime);
    }

    /**
     * 构建 LLM prompt
     */
    private String buildPrompt(
            OptimizationAction.TargetType targetType,
            String targetId,
            Map<String, Object> currentState,
            List<EvaluationEngine.TaskData> recentTasks,
            List<EvaluationRecord> evaluationRecords) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个AI优化顾问，负责分析Character或Organization在任务执行过程中的表现，并提供优化建议。\n\n");

        // 目标类型和ID
        prompt.append("## 目标信息\n");
        prompt.append(String.format("目标类型: %s\n", targetType == OptimizationAction.TargetType.CHARACTER ? "Character" : "Organization"));
        prompt.append(String.format("目标ID: %s\n\n", targetId));

        // 当前状态
        prompt.append("## 当前状态\n");
        try {
            String stateJson = GsonUtils.toJson(currentState);
            prompt.append(stateJson).append("\n\n");
        } catch (Exception e) {
            log.warn("[LLMSuggestionGenerator] Failed to serialize state", e);
            prompt.append(currentState.toString()).append("\n\n");
        }

        // 最近任务数据
        prompt.append("## 最近任务执行数据\n");
        prompt.append(String.format("共 %d 个任务:\n\n", recentTasks.size()));
        for (int i = 0; i < recentTasks.size(); i++) {
            EvaluationEngine.TaskData task = recentTasks.get(i);
            prompt.append(String.format("任务 %d:\n", i + 1));
            prompt.append(String.format("  - 任务ID: %s\n", task.getTaskId()));
            prompt.append(String.format("  - 输入: %s\n", truncate(task.getTaskInput(), 200)));
            prompt.append(String.format("  - 输出: %s\n", truncate(task.getTaskOutput(), 200)));
            prompt.append(String.format("  - 成功: %s\n", task.getSuccess()));
            prompt.append(String.format("  - 耗时: %d ms\n", task.getDurationMs() != null ? task.getDurationMs() : 0));
            if (task.getErrorMessage() != null) {
                prompt.append(String.format("  - 错误: %s\n", truncate(task.getErrorMessage(), 100)));
            }
            prompt.append("\n");
        }

        // 历史评估记录
        if (!evaluationRecords.isEmpty()) {
            prompt.append("## 历史评估记录\n");
            for (int i = 0; i < Math.min(evaluationRecords.size(), 5); i++) {
                EvaluationRecord record = evaluationRecords.get(i);
                prompt.append(String.format("评估 %d:\n", i + 1));
                prompt.append(String.format("  - 评分: %.2f\n", record.getOverallScore() != null ? record.getOverallScore() : 0.0));
                prompt.append(String.format("  - 建议: %s\n", record.getSuggestions()));
                prompt.append("\n");
            }
        }

        // 输出要求
        prompt.append("## 输出要求\n");
        prompt.append("请分析以上信息，生成3-5条优化建议。建议应包括以下类型之一：\n");
        prompt.append("1. personality调整 - 如修改沟通风格、决策方式、价值观等\n");
        prompt.append("2. tag更新 - 如更新对其他Character的印象、信任度等\n");
        prompt.append("3. 技能增删 - 如添加新技能、移除不擅长的技能\n");
        prompt.append("4. 沟通方式优化 - 如调整消息格式、回复风格等\n");
        prompt.append("5. 工作方式调整 - 如风险偏好、协作模式等\n\n");
        prompt.append("请以JSON数组格式输出，每条建议是一个字符串：\n");
        prompt.append("[\"建议1的具体内容\", \"建议2的具体内容\", \"建议3的具体内容\"]\n");

        return prompt.toString();
    }

    /**
     * 调用 LLM 生成建议
     *
     * @param workspace        工作空间ID
     * @param organizationId   组织ID
     * @param targetId         目标ID (用于获取Character级别的prompt)
     * @param prompt          用户prompt
     * @return 建议列表
     */
    private List<String> callLLM(String workspace, String organizationId, String targetId, String prompt) {
        // 通过 PromptManager 获取 system prompt，支持层级查找
        String characterId = targetId; // targetId 对于 CHARACTER 类型就是 characterId
        String systemPrompt = promptManager.getPrompt(workspace, organizationId, characterId, PromptKeys.OBSERVER_SUGGESTION);
        if (systemPrompt == null) {
            // 使用默认 system prompt
            systemPrompt = "你是一个专业的AI优化顾问，擅长分析AI角色的行为模式并提供改进建议。请根据提供的数据生成具体、可执行的优化建议。";
        }

        LLMRequest request = LLMRequest.builder()
                .modelId(suggestionModelId)
                .systemPrompt(systemPrompt)
                .messages(List.of(
                        LLMRequest.LLMMessage.builder()
                                .role(LLMRequest.LLMMessage.Role.USER)
                                .content(prompt)
                                .build()
                ))
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        try {
            LLMResponse response = llmCaller.call(request);
            String content = response.getContent();

            if (content == null || content.isEmpty()) {
                log.warn("[LLMSuggestionGenerator] LLM returned empty content");
                return new ArrayList<>();
            }

            return parseSuggestions(content);
        } catch (Exception e) {
            log.error("[LLMSuggestionGenerator] LLM call failed", e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析 LLM 响应，提取建议列表
     */
    private List<String> parseSuggestions(String content) {
        List<String> suggestions = new ArrayList<>();

        // 尝试提取 JSON 数组
        Pattern jsonArrayPattern = Pattern.compile("\\[[\\s\\S]*\\]");
        Matcher matcher = jsonArrayPattern.matcher(content);

        if (matcher.find()) {
            String jsonArray = matcher.group();
            try {
                suggestions = GsonUtils.fromJson(jsonArray, List.class);
            } catch (Exception e) {
                log.warn("[LLMSuggestionGenerator] Failed to parse JSON array: {}", jsonArray, e);
                // 尝试其他解析方式
                suggestions = parseSuggestionsFallback(content);
            }
        } else {
            suggestions = parseSuggestionsFallback(content);
        }

        return suggestions;
    }

    /**
     * 回退解析方法
     */
    private List<String> parseSuggestionsFallback(String content) {
        List<String> suggestions = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-") || line.matches("^\\d+\\..*")) {
                // 移除序号或破折号
                String suggestion = line.replaceFirst("^[-\\d.]+\\s*", "").trim();
                if (!suggestion.isEmpty() && suggestion.length() > 10) {
                    suggestions.add(suggestion);
                }
            }
        }
        return suggestions;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}

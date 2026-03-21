package org.dragon.agent.llm.util;

import java.util.List;
import java.util.function.Function;

import org.dragon.character.Character;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Character 调用工具类
 * 封装对 Character 的调用，提供统一的调用方式和结果处理
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class CharacterCaller {

    private static final Logger log = LoggerFactory.getLogger(CharacterCaller.class);

    /**
     * 简单调用 Character，返回原始结果
     *
     * @param character 要调用的 Character
     * @param prompt 提示词
     * @return Character 返回的结果
     */
    public String call(Character character, String prompt) {
        try {
            return character.run(prompt);
        } catch (Exception e) {
            log.warn("[CharacterCaller] Character {} call failed: {}", character.getId(), e.getMessage());
            throw new RuntimeException("Character call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 带默认值的调用
     *
     * @param character 要调用的 Character
     * @param prompt 提示词
     * @param defaultValue 调用失败时的默认值
     * @return Character 返回的结果，失败时返回默认值
     */
    public String call(Character character, String prompt, String defaultValue) {
        try {
            return character.run(prompt);
        } catch (Exception e) {
            log.warn("[CharacterCaller] Character {} call failed, using default: {}", character.getId(), e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 决策类调用
     * 返回 Boolean（true = 批准，false = 拒绝）
     *
     * @param character 要调用的 Character
     * @param prompt 提示词（应包含决策要求）
     * @return true 表示 APPROVE/批准，false 表示 DENY/拒绝
     */
    public boolean callForDecision(Character character, String prompt) {
        String result = call(character, prompt, "APPROVE");
        return isApproved(result);
    }

    /**
     * 选择类调用（通用版本）
     * 从候选列表中选择一个元素
     *
     * @param character 要调用的 Character
     * @param prompt 提示词（应包含选择要求）
     * @param candidates 候选列表
     * @param <T> 候选元素类型
     * @return 选中的元素，如果未选择或失败返回 null
     */
    public <T> T callForSelection(Character character, String prompt, List<T> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // 将候选列表格式化到 prompt 中
        String fullPrompt = buildSelectionPrompt(prompt, candidates);

        String selectedId;
        try {
            selectedId = call(character, fullPrompt).trim();
            log.debug("[CharacterCaller] Selected: {}", selectedId);
        } catch (Exception e) {
            log.warn("[CharacterCaller] Selection failed, using first candidate: {}", e.getMessage());
            return candidates.get(0);
        }

        if ("NONE".equalsIgnoreCase(selectedId)) {
            return null;
        }

        // 查找匹配的候选元素
        return candidates.stream()
                .filter(c -> String.valueOf(c).equals(selectedId) || toString(c).equals(selectedId))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("[CharacterCaller] Selected {} not found in candidates, using first", selectedId);
                    return candidates.isEmpty() ? null : candidates.get(0);
                });
    }

    /**
     * 选择类调用（Character 版本）
     * 从 Character 候选列表中选择，通过 ID 匹配
     *
     * @param hrCharacter 用于执行选择的 HR Character
     * @param prompt 提示词（应包含选择要求）
     * @param candidates Character 候选列表
     * @return 选中的 Character，如果未选择或失败返回 null
     */
    public Character callForSelectionCharacter(Character hrCharacter, String prompt, List<Character> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        String fullPrompt = buildCharacterSelectionPrompt(prompt, candidates);

        String selectedId;
        try {
            selectedId = call(hrCharacter, fullPrompt).trim();
            log.debug("[CharacterCaller] Selected character ID: {}", selectedId);
        } catch (Exception e) {
            log.warn("[CharacterCaller] Character selection failed, using first candidate: {}", e.getMessage());
            return candidates.get(0);
        }

        if ("NONE".equalsIgnoreCase(selectedId)) {
            return null;
        }

        // 通过 ID 匹配 Character
        return candidates.stream()
                .filter(c -> c.getId().equals(selectedId))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("[CharacterCaller] Selected character {} not found, using first", selectedId);
                    return candidates.isEmpty() ? null : candidates.get(0);
                });
    }

    /**
     * 构建 Character 选择提示词
     */
    private String buildCharacterSelectionPrompt(String basePrompt, List<Character> candidates) {
        StringBuilder builder = new StringBuilder();
        builder.append(basePrompt).append("\n\n候选列表：\n");

        for (int i = 0; i < candidates.size(); i++) {
            Character c = candidates.get(i);
            builder.append(String.format("%d. %s (ID: %s) - %s\n",
                    i + 1, c.getName(), c.getId(),
                    c.getDescription() != null ? c.getDescription() : "无描述"));
        }

        builder.append("\n请直接返回选中的 Character ID（只返回 ID，不要包含其他内容）。");
        builder.append("如果不选择任何候选，请返回 \"NONE\"。");

        return builder.toString();
    }

    /**
     * 带解析器的调用
     *
     * @param character 要调用的 Character
     * @param prompt 提示词
     * @param parser 结果解析器
     * @param <R> 返回类型
     * @return 解析后的结果
     */
    public <R> R callWithParser(Character character, String prompt, Function<String, R> parser) {
        return callWithParser(character, prompt, parser, null);
    }

    /**
     * 带解析器和默认值的调用
     *
     * @param character 要调用的 Character
     * @param prompt 提示词
     * @param parser 结果解析器
     * @param defaultValue 失败时的默认值
     * @param <R> 返回类型
     * @return 解析后的结果，失败时返回默认值
     */
    public <R> R callWithParser(Character character, String prompt, Function<String, R> parser, R defaultValue) {
        try {
            String result = character.run(prompt);
            return parser.apply(result);
        } catch (Exception e) {
            log.warn("[CharacterCaller] Character {} call with parser failed: {}", character.getId(), e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 判断决策结果是否为批准
     */
    private boolean isApproved(String decision) {
        if (decision == null) {
            return false;
        }
        String upperDecision = decision.toUpperCase();
        return upperDecision.contains("APPROVE") || upperDecision.contains("批准");
    }

    /**
     * 构建选择提示词
     */
    private <T> String buildSelectionPrompt(String basePrompt, List<T> candidates) {
        StringBuilder builder = new StringBuilder();
        builder.append(basePrompt).append("\n\n候选列表：\n");

        for (int i = 0; i < candidates.size(); i++) {
            builder.append(String.format("%d. %s\n", i + 1, toString(candidates.get(i))));
        }

        builder.append("\n请直接返回选中的编号或内容（只返回选择结果，不要包含其他内容）。");
        builder.append("如果不选择任何候选，请返回 \"NONE\"。");

        return builder.toString();
    }

    /**
     * 转换为字符串
     */
    private <T> String toString(T obj) {
        return obj != null ? obj.toString() : "";
    }
}

package org.dragon.character.mind;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 性格描述对象
 * 用于定义 Character 的性格特征、价值观、行为偏好等
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityDescriptor {

    /**
     * Character 名称
     */
    private String name;

    /**
     * 性格特征列表
     */
    private List<String> traits;

    /**
     * 价值观列表
     */
    private List<String> values;

    /**
     * 沟通风格
     */
    private String communicationStyle;

    /**
     * 决策风格
     */
    private String decisionStyle;

    /**
     * 专业知识领域
     */
    private List<String> expertise;

    /**
     * 与其他 Character 的关系
     */
    private List<Relationship> relationships;

    /**
     * 扩展字段
     */
    private java.util.Map<String, Object> extensions;

    /**
     * 将性格描述转换为 LLM 系统 Prompt
     * 用于注入到 LLM 的 system message 中
     *
     * @return 格式化的 prompt 字符串
     */
    public String toPrompt() {
        StringBuilder prompt = new StringBuilder();

        if (name != null && !name.isEmpty()) {
            prompt.append("你是 ").append(name).append("。\n");
        }

        if (traits != null && !traits.isEmpty()) {
            prompt.append("性格特征：").append(String.join("、", traits)).append("。\n");
        }

        if (values != null && !values.isEmpty()) {
            prompt.append("价值观：").append(String.join("、", values)).append("。\n");
        }

        if (communicationStyle != null && !communicationStyle.isEmpty()) {
            prompt.append("沟通风格：").append(communicationStyle).append("。\n");
        }

        if (decisionStyle != null && !decisionStyle.isEmpty()) {
            prompt.append("决策风格：").append(decisionStyle).append("。\n");
        }

        if (expertise != null && !expertise.isEmpty()) {
            prompt.append("专业领域：").append(String.join("、", expertise)).append("。\n");
        }

        return prompt.toString();
    }

    /**
     * 关系描述
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Relationship {
        /**
         * 关联的 Character ID
         */
        private String characterId;

        /**
         * 印象描述
         */
        private String impression;

        /**
         * 关系类型
         */
        private String relationType;
    }
}

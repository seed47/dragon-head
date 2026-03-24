package org.dragon.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 技能
 * Character 拥有的技能定义
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    /**
     * 技能唯一 ID
     */
    private String id;

    /**
     * 技能名称
     */
    private String name;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 技能分类
     */
    private String category;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 技能元数据
     */
    private SkillMetadata metadata;

    /**
     * 技能元数据定义。
     * 包含输入/输出参数定义以及额外配置信息。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillMetadata {
        /**
         * 输入参数定义
         */
        private List<Parameter> inputParams;

        /**
         * 输出定义
         */
        private List<Parameter> outputParams;

        /**
         * 额外配置
         */
        private Map<String, Object> config;
    }

    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        /**
         * 参数名
         */
        private String name;

        /**
         * 参数类型
         */
        private String type;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 是否必需
         */
        private boolean required;

        /**
         * 默认值
         */
        private String defaultValue;
    }
}

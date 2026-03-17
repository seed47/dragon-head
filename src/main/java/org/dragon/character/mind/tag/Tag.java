package org.dragon.character.mind.tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签
 * 用于 Character 对其他 Character 的印象标记
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签值
     */
    private String value;

    /**
     * 情感色彩
     */
    private Sentiment sentiment;

    /**
     * 信任等级 (1-10)
     */
    private int trustLevel;

    /**
     * 摘要描述
     */
    private String summary;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 情感色彩枚举
     */
    public enum Sentiment {
        /** 正面 */
        POSITIVE,
        /** 中性 */
        NEUTRAL,
        /** 负面 */
        NEGATIVE
    }
}

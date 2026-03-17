package org.dragon.character.mind.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 记忆
 * 存储 Character 的对话、行为、观察等记忆内容
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Memory {

    /**
     * 记忆唯一 ID
     */
    private String id;

    /**
     * 所属 Character ID
     */
    private String characterId;

    /**
     * 记忆类型
     */
    private MemoryType type;

    /**
     * 记忆内容
     */
    private String content;

    /**
     * 向量嵌入（用于语义检索）
     */
    private Map<String, Object> embeddings;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 记忆类型枚举
     */
    public enum MemoryType {
        /** 对话记忆 */
        CONVERSATION,
        /** 行为记忆 */
        ACTION,
        /** 观察记忆 */
        OBSERVATION,
        /** 系统记忆 */
        SYSTEM
    }
}

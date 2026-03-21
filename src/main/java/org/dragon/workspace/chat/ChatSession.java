package org.dragon.workspace.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChatSession 聊天会话
 * 当多个 Character 协同完成一个任务时创建的会话
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    /**
     * 会话状态
     */
    public enum Status {
        ACTIVE,     // 活跃
        COMPLETED,  // 已完成
        ARCHIVED    // 已归档
    }

    /**
     * 会话 ID
     */
    private String id;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 关联的任务 ID
     */
    private String taskId;

    /**
     * 参与者 ID 列表 (Character IDs)
     */
    private List<String> participantIds;

    /**
     * 会话上下文
     */
    private Map<String, Object> context;

    /**
     * 决策记录
     */
    private List<DecisionRecord> decisions;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 状态
     */
    @Builder.Default
    private Status status = Status.ACTIVE;

    /**
     * DecisionRecord 决策记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionRecord {
        /**
         * 决策 ID
         */
        private String id;

        /**
         * 决策者 ID
         */
        private String characterId;

        /**
         * 决策描述
         */
        private String decision;

        /**
         * 决策依据
         */
        private String rationale;

        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
    }
}

package org.dragon.organization.member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OrganizationMember 组织成员
 * 表示 Character 在组织中的身份和属性
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMember {

    /**
     * 成员层级
     */
    public enum Layer {
        MANAGEMENT,  // 管理层 - 决策层
        CORE,        // 核心层 - 核心贡献者
        NORMAL       // 普通层 - 一般成员
    }

    /**
     * 成员全局唯一标识
     * 格式: orgId_characterId
     */
    private String id;

    /**
     * 组织 ID
     */
    private String organizationId;

    /**
     * Character ID
     */
    private String characterId;

    /**
     * 角色
     * 如: 队长、专家、执行者、观察员
     */
    private String role;

    /**
     * 层级
     */
    @Builder.Default
    private Layer layer = Layer.NORMAL;

    /**
     * 标签列表
     * 描述成员擅长领域、历史表现、可信度等
     */
    private List<String> tags;

    /**
     * 调度权重
     * 影响任务分配优先级
     */
    @Builder.Default
    private double weight = 1.0;

    /**
     * 调度优先级
     */
    @Builder.Default
    private int priority = 0;

    /**
     * 声誉积分
     * 影响角色晋升或权限
     */
    @Builder.Default
    private int reputation = 0;

    /**
     * 资源配额
     */
    private ResourceQuota resourceQuota;

    /**
     * 加入时间
     */
    private LocalDateTime joinAt;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;

    /**
     * 扩展属性
     */
    private Map<String, Object> metadata;

    /**
     * 创建复合 ID
     *
     * @param organizationId 组织 ID
     * @param characterId Character ID
     * @return 复合 ID
     */
    public static String createId(String organizationId, String characterId) {
        return organizationId + "_" + characterId;
    }

    /**
     * ResourceQuota 资源配额
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceQuota {
        /**
         * Token 预算配额
         */
        private int tokenBudget;

        /**
         * 并发任务限制
         */
        @Builder.Default
        private int maxConcurrentTasks = 5;

        /**
         * 每日最大使用时长（分钟）
         */
        @Builder.Default
        private int maxDailyMinutes = 480;
    }
}

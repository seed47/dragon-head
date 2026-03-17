package org.dragon.organization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.dragon.organization.personality.OrganizationPersonality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Organization 实体
 * 组织是由多个 Character 组成的协作单元，具备共同的目标、文化、奖惩规则
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    /**
     * 组织状态
     */
    public enum Status {
        ACTIVE,    // 活跃
        INACTIVE,  // 未激活
        ARCHIVED   // 已归档
    }

    /**
     * 组织全局唯一标识
     */
    private String id;

    /**
     * 组织名称
     */
    private String name;

    /**
     * 组织描述
     */
    private String description;

    /**
     * 版本号，用于版本管理
     */
    @Builder.Default
    private int version = 1;

    /**
     * 组织状态
     */
    @Builder.Default
    private Status status = Status.INACTIVE;

    /**
     * 组织性格/文化（类比 Character Mind）
     */
    private OrganizationPersonality personality;

    /**
     * 组织特性标签
     * 如: "创新导向", "成本优先", "安全第一", "客户服务型"
     */
    private Map<String, Object> properties;

    /**
     * 优势领域
     * 如: 数据分析、创意生成、代码开发
     */
    private List<String> advantages;

    /**
     * 默认权重（调度时使用）
     */
    @Builder.Default
    private double defaultWeight = 1.0;

    /**
     * 默认优先级
     */
    @Builder.Default
    private int defaultPriority = 0;

    /**
     * 扩展属性
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建者 ID
     */
    private String creatorId;
}

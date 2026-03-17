package org.dragon.organization.recruitment;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RecruitmentRequest 招聘需求
 * 组织发布的空缺角色需求
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentRequest {

    /**
     * 需求状态
     */
    public enum Status {
        OPEN,       // 开放
        CLOSED,     // 已关闭
        FILLED      // 已填补
    }

    /**
     * 需求 ID
     */
    private String id;

    /**
     * 组织 ID
     */
    private String organizationId;

    /**
     * 职位名称
     */
    private String position;

    /**
     * 需求描述
     */
    private String description;

    /**
     * 所需能力列表
     */
    private List<String> requiredCapabilities;

    /**
     * 所需经验
     */
    private String requiredExperience;

    /**
     * 性格特征要求
     */
    private List<String> personalityRequirements;

    /**
     * 角色类型
     */
    private String roleType;

    /**
     * 层级
     */
    private String layer;

    /**
     * 状态
     */
    @Builder.Default
    private Status status = Status.OPEN;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 扩展属性
     */
    private java.util.Map<String, Object> metadata;
}

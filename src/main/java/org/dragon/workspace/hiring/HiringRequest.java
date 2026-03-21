package org.dragon.workspace.hiring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HiringRequest 雇佣请求实体
 * 外部提交给 Workspace 的工作单元，描述需要完成的任务及对执行者的要求
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HiringRequest {

    /**
     * 雇佣目标类型
     */
    public enum TargetType {
        CHARACTER,      // 指定 Character
        ROLE,           // 指定角色类型，由 Workspace 自动筛选
        AUTO            // 自动匹配最优执行者
    }

    /**
     * 雇佣请求唯一标识
     */
    private String id;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 工作描述（任务目标，自然语言或结构化指令）
     */
    private String workDescription;

    /**
     * 雇佣目标类型
     */
    private TargetType targetType;

    /**
     * 指定目标 ID（当 targetType 为 CHARACTER 时）
     */
    private String targetId;

    /**
     * 雇佣数量（批量雇佣）
     */
    @Builder.Default
    private int quantity = 1;

    /**
     * 能力要求：技能标签、性格特征、历史表现门槛等
     */
    private List<String> requiredCapabilities;

    /**
     * 任务参数：键值对形式
     */
    private Map<String, Object> taskParameters;

    /**
     * 物料引用：物料 ID 列表
     */
    private List<String> materialIds;

    /**
     * 回调地址（可选）：任务完成后通知
     */
    private String callbackUrl;

    /**
     * 雇佣请求状态
     */
    @Builder.Default
    private HiringRequestStatus status = HiringRequestStatus.OPEN;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

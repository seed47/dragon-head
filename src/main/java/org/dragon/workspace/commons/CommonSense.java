package org.dragon.workspace.commons;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CommonSense 常识实体 (Workspace 版本)
 * 存储 Workspace 级别的常识规则，支持转换成 prompt
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonSense {

    /**
     * 常识类别
     */
    public enum Category {
        PRIVACY,      // 隐私保护
        SAFETY,       // 安全合规
        PERFORMANCE,  // 性能约束
        BUSINESS,     // 业务规则
        ETHICS,       // 伦理道德
        SYSTEM        // 系统约束
    }

    /**
     * 严重程度
     */
    public enum Severity {
        CRITICAL,  // 关键 - 违反将导致系统不可用或严重违规
        HIGH,      // 高 - 违反可能导致严重后果
        MEDIUM     // 中 - 违反会产生一定影响
    }

    /**
     * Prompt 更新来源
     */
    public enum UpdateSource {
        AUTO,      // 自动更新 (commonsense 内容变化触发)
        MANUAL,    // 手动触发
        SCHEDULED  // 定时任务
    }

    /**
     * 常识唯一标识
     */
    private String id;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 文件夹 ID
     * 可选，关联到文件夹
     */
    private String folderId;

    /**
     * 常识名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 类别
     */
    private Category category;

    /**
     * 规则表达式
     * 使用简单的规则描述，可以是 JSON 或特定语法
     */
    private String rule;

    /**
     * 严重程度
     */
    private Severity severity;

    /**
     * 版本号
     */
    @Builder.Default
    private int version = 1;

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 转换成 prompt 的模板
     * 支持变量替换，如: ${name}, ${rule}
     */
    private String promptTemplate;

    /**
     * 模板变量
     */
    private Map<String, Object> promptVariables;

    /**
     * 缓存的 prompt
     */
    private String cachedPrompt;

    /**
     * 上次 prompt 更新时间
     */
    private LocalDateTime lastPromptUpdateAt;

    /**
     * Prompt 更新来源
     */
    private UpdateSource promptUpdateSource;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 检查是否为关键级别
     */
    public boolean isCritical() {
        return severity == Severity.CRITICAL;
    }

    /**
     * 检查是否可禁用
     * 关键级别常识不可禁用
     */
    public boolean canDisable() {
        return severity != Severity.CRITICAL;
    }
}

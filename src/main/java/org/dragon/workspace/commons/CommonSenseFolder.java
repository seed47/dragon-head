package org.dragon.workspace.commons;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CommonSenseFolder 常识文件夹
 * 用于组织 Workspace 下的 CommonSense，支持层级结构
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonSenseFolder {

    /**
     * 文件夹唯一标识
     */
    private String id;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 父文件夹 ID
     * null 表示根文件夹
     */
    private String parentId;

    /**
     * 文件夹名称
     */
    private String name;

    /**
     * 文件夹描述
     */
    private String description;

    /**
     * 排序顺序
     */
    @Builder.Default
    private int sortOrder = 0;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 检查是否为根文件夹
     */
    public boolean isRoot() {
        return parentId == null || parentId.isEmpty();
    }
}

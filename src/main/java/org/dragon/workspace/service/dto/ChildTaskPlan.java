package org.dragon.workspace.service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 子任务计划
 * 由 ProjectManager 分解任务后返回的子任务结构
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildTaskPlan {

    /**
     * 子任务名称
     */
    private String name;

    /**
     * 子任务描述
     */
    private String description;

    /**
     * 分配的执行者 Character ID
     */
    private String characterId;

    /**
     * 执行者角色名称
     */
    private String characterRole;

    /**
     * 依赖的父任务 ID 列表
     */
    private List<String> dependencyTaskIds;

    /**
     * 是否需要用户输入
     */
    private boolean needsUserInput;

    /**
     * 预期输出描述
     */
    private String expectedOutput;
}

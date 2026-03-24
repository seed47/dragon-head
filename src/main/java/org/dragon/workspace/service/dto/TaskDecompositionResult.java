package org.dragon.workspace.service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务分解结果
 * 由 ProjectManager 分解任务后返回的完整结构
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDecompositionResult {

    /**
     * 分解总结
     */
    private String summary;

    /**
     * 协作模式：AUTO, SEQUENTIAL, PARALLEL, HYBRID
     */
    private String collaborationMode;

    /**
     * 子任务计划列表
     */
    private List<ChildTaskPlan> childTasks;
}

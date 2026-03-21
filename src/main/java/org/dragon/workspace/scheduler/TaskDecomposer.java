package org.dragon.workspace.scheduler;

import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.task.SubTask;
import org.dragon.workspace.task.WorkspaceTask;

import java.util.List;

/**
 * TaskDecomposer 任务分解器接口
 * 使用 ReAct Agent 智能分解任务
 *
 * @author wyj
 * @version 1.0
 */
public interface TaskDecomposer {

    /**
     * 使用 ReAct Agent 分解任务
     *
     * @param task 工作空间任务
     * @param workspace 工作空间信息
     * @param availableMembers 可用成员列表
     * @return 子任务列表
     */
    List<SubTask> decomposeWithReAct(
            WorkspaceTask task,
            Workspace workspace,
            List<WorkspaceMember> availableMembers
    );

    /**
     * 简单任务分解（不使用 ReAct）
     *
     * @param task 工作空间任务
     * @param workspace 工作空间信息
     * @param availableMembers 可用成员列表
     * @return 子任务列表
     */
    List<SubTask> decomposeSimple(
            WorkspaceTask task,
            Workspace workspace,
            List<WorkspaceMember> availableMembers
    );
}

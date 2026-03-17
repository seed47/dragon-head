package org.dragon.organization.scheduler;

import org.dragon.organization.Organization;
import org.dragon.organization.member.OrganizationMember;
import org.dragon.organization.task.OrganizationTask;
import org.dragon.organization.task.SubTask;

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
     * @param task 组织任务
     * @param organization 组织信息
     * @param availableMembers 可用成员列表
     * @return 子任务列表
     */
    List<SubTask> decomposeWithReAct(
            OrganizationTask task,
            Organization organization,
            List<OrganizationMember> availableMembers
    );

    /**
     * 简单任务分解（不使用 ReAct）
     *
     * @param task 组织任务
     * @param organization 组织信息
     * @param availableMembers 可用成员列表
     * @return 子任务列表
     */
    List<SubTask> decomposeSimple(
            OrganizationTask task,
            Organization organization,
            List<OrganizationMember> availableMembers
    );
}

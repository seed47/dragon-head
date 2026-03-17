package org.dragon.organization.path;

import java.util.List;

/**
 * PathStore 路径存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface PathStore {

    /**
     * 保存路径
     *
     * @param path 工作路径
     */
    void save(WorkflowPath path);

    /**
     * 根据 ID 获取路径
     *
     * @param pathId 路径 ID
     * @return 工作路径
     */
    WorkflowPath findById(String pathId);

    /**
     * 根据组织 ID 获取所有路径
     *
     * @param organizationId 组织 ID
     * @return 路径列表
     */
    List<WorkflowPath> findByOrganizationId(String organizationId);

    /**
     * 根据任务类型获取路径
     *
     * @param organizationId 组织 ID
     * @param taskType 任务类型
     * @return 路径列表
     */
    List<WorkflowPath> findByOrganizationIdAndTaskType(String organizationId, String taskType);

    /**
     * 获取组织的最佳实践路径
     *
     * @param organizationId 组织 ID
     * @return 最佳实践路径列表
     */
    List<WorkflowPath> findBestPractices(String organizationId);

    /**
     * 更新路径
     *
     * @param path 工作路径
     */
    void update(WorkflowPath path);

    /**
     * 删除路径
     *
     * @param pathId 路径 ID
     */
    void delete(String pathId);

    /**
     * 根据组织 ID 删除所有路径
     *
     * @param organizationId 组织 ID
     */
    void deleteByOrganizationId(String organizationId);
}

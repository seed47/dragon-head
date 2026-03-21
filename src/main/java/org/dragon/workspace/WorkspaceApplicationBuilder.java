package org.dragon.workspace;

import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.scheduler.WorkspaceScheduler;
import org.dragon.workspace.service.WorkspaceActionLogService;
import org.dragon.workspace.service.WorkspaceHiringService;
import org.dragon.workspace.service.WorkspaceLifecycleService;
import org.dragon.workspace.service.WorkspaceMaterialService;
import org.dragon.workspace.service.WorkspaceMemberManagementService;
import org.dragon.workspace.service.WorkspaceTaskService;
import org.dragon.workspace.task.WorkspaceTaskStore;

/**
 * WorkspaceApplicationBuilder Workspace 应用构建器
 * 用于构建 WorkspaceApplication 实例，在构建过程中完成 Workspace 的初始化
 *
 * @author wyj
 * @version 1.0
 */
public class WorkspaceApplicationBuilder {

    // 必需属性
    String workspaceId;

    // 服务依赖
    WorkspaceLifecycleService workspaceLifecycleService;
    WorkspaceHiringService workspaceHiringService;
    WorkspaceActionLogService workspaceActionLogService;
    WorkspaceMemberManagementService workspaceMemberService;
    WorkspaceMaterialService materialService;
    WorkspaceTaskService workspaceTaskService;
    CharacterRegistry characterRegistry;
    WorkspaceScheduler workspaceScheduler;
    WorkspaceTaskStore workspaceTaskStore;

    /**
     * 设置 Workspace ID
     *
     * @param workspaceId Workspace ID
     * @return self
     */
    public WorkspaceApplicationBuilder workspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    /**
     * 设置 Workspace 生命周期服务
     *
     * @param workspaceLifecycleService WorkspaceLifecycleService
     * @return self
     */
    public WorkspaceApplicationBuilder workspaceLifecycleService(WorkspaceLifecycleService workspaceLifecycleService) {
        this.workspaceLifecycleService = workspaceLifecycleService;
        return this;
    }

    /**
     * 设置 Workspace 雇佣服务
     *
     * @param workspaceHiringService WorkspaceHiringService
     * @return self
     */
    public WorkspaceApplicationBuilder workspaceHiringService(WorkspaceHiringService workspaceHiringService) {
        this.workspaceHiringService = workspaceHiringService;
        return this;
    }

    /**
     * 设置 Workspace 行为日志服务
     *
     * @param workspaceActionLogService WorkspaceActionLogService
     * @return self
     */
    public WorkspaceApplicationBuilder workspaceActionLogService(WorkspaceActionLogService workspaceActionLogService) {
        this.workspaceActionLogService = workspaceActionLogService;
        return this;
    }

    /**
     * 设置 Workspace 成员服务
     *
     * @param workspaceMemberService WorkspaceMemberManagementService
     * @return self
     */
    public WorkspaceApplicationBuilder workspaceMemberService(WorkspaceMemberManagementService workspaceMemberService) {
        this.workspaceMemberService = workspaceMemberService;
        return this;
    }

    /**
     * 设置物料服务
     *
     * @param materialService WorkspaceMaterialService
     * @return self
     */
    public WorkspaceApplicationBuilder materialService(WorkspaceMaterialService materialService) {
        this.materialService = materialService;
        return this;
    }

    /**
     * 设置 Workspace 任务服务
     *
     * @param workspaceTaskService WorkspaceTaskService
     * @return self
     */
    public WorkspaceApplicationBuilder workspaceTaskService(WorkspaceTaskService workspaceTaskService) {
        this.workspaceTaskService = workspaceTaskService;
        return this;
    }

    /**
     * 设置 Character 注册表
     *
     * @param characterRegistry CharacterRegistry
     * @return self
     */
    public WorkspaceApplicationBuilder characterRegistry(CharacterRegistry characterRegistry) {
        this.characterRegistry = characterRegistry;
        return this;
    }

    /**
     * 设置 Workspace 调度器
     *
     * @param workspaceScheduler WorkspaceScheduler
     * @return self
     */
    public WorkspaceApplicationBuilder workspaceScheduler(WorkspaceScheduler workspaceScheduler) {
        this.workspaceScheduler = workspaceScheduler;
        return this;
    }

    /**
     * 设置 Workspace 任务存储
     *
     * @param workspaceTaskStore WorkspaceTaskStore
     * @return self
     */
    public WorkspaceApplicationBuilder workspaceTaskStore(WorkspaceTaskStore workspaceTaskStore) {
        this.workspaceTaskStore = workspaceTaskStore;
        return this;
    }

    /**
     * 构建 WorkspaceApplication 实例
     * 在构建过程中可以完成 Workspace 的初始化逻辑
     *
     * @return WorkspaceApplication 实例
     */
    public WorkspaceApplication build() {
        // 验证必需属性
        if (workspaceId == null || workspaceId.isEmpty()) {
            throw new IllegalStateException("workspaceId is required");
        }

        // 可以在这里添加初始化逻辑，例如：
        // - 验证 workspace 是否存在
        // - 初始化 workspace 相关的资源
        // - 加载 workspace 配置

        return new WorkspaceApplication(this);
    }
}

package org.dragon.workspace;

import org.dragon.character.CharacterRegistry;
import org.dragon.task.TaskStore;
import org.dragon.observer.actionlog.ObserverActionLogService;
import org.dragon.workspace.service.WorkspaceHiringService;
import org.dragon.workspace.service.WorkspaceLifecycleService;
import org.dragon.workspace.service.WorkspaceMaterialService;
import org.dragon.workspace.service.WorkspaceMemberManagementService;
import org.dragon.workspace.service.WorkspaceTaskArrangementService;
import org.dragon.workspace.service.WorkspaceTaskExecutionService;
import org.dragon.workspace.service.WorkspaceTaskService;
import org.dragon.workspace.service.TaskContinuationResolver;

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
    ObserverActionLogService workspaceActionLogService;
    WorkspaceMemberManagementService workspaceMemberService;
    WorkspaceMaterialService materialService;
    WorkspaceTaskService workspaceTaskService;
    CharacterRegistry characterRegistry;
    WorkspaceTaskArrangementService workspaceTaskArrangementService;
    TaskStore taskStore;
    TaskContinuationResolver taskContinuationResolver;
    WorkspaceTaskExecutionService taskExecutionService;

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
    public WorkspaceApplicationBuilder workspaceActionLogService(ObserverActionLogService workspaceActionLogService) {
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
     * 设置 Workspace 任务编排服务
     *
     * @param workspaceTaskArrangementService WorkspaceTaskArrangementService
     * @return self
     */
    public WorkspaceApplicationBuilder workspaceTaskArrangementService(WorkspaceTaskArrangementService workspaceTaskArrangementService) {
        this.workspaceTaskArrangementService = workspaceTaskArrangementService;
        return this;
    }

    /**
     * 设置 Workspace 任务存储
     *
     * @param taskStore TaskStore
     * @return self
     */
    public WorkspaceApplicationBuilder taskStore(TaskStore taskStore) {
        this.taskStore = taskStore;
        return this;
    }

    /**
     * 设置任务续跑解析器
     *
     * @param taskContinuationResolver TaskContinuationResolver
     * @return self
     */
    public WorkspaceApplicationBuilder taskContinuationResolver(TaskContinuationResolver taskContinuationResolver) {
        this.taskContinuationResolver = taskContinuationResolver;
        return this;
    }

    /**
     * 设置任务执行服务
     *
     * @param taskExecutionService WorkspaceTaskExecutionService
     * @return self
     */
    public WorkspaceApplicationBuilder taskExecutionService(WorkspaceTaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
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
        if (workspaceTaskService == null) {
            throw new IllegalStateException("workspaceTaskService is required");
        }
        if (workspaceTaskArrangementService == null) {
            throw new IllegalStateException("workspaceTaskArrangementService is required");
        }
        if (taskStore == null) {
            throw new IllegalStateException("taskStore is required");
        }
        if (taskContinuationResolver == null) {
            throw new IllegalStateException("taskContinuationResolver is required");
        }
        if (taskExecutionService == null) {
            throw new IllegalStateException("taskExecutionService is required");
        }

        // 可以在这里添加初始化逻辑，例如：
        // - 验证 workspace 是否存在
        // - 初始化 workspace 相关的资源
        // - 加载 workspace 配置

        return new WorkspaceApplication(this);
    }
}

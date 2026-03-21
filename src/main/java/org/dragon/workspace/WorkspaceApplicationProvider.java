package org.dragon.workspace;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.service.WorkspaceActionLogService;
import org.dragon.workspace.service.WorkspaceHiringService;
import org.dragon.workspace.service.WorkspaceLifecycleService;
import org.dragon.workspace.service.WorkspaceMaterialService;
import org.dragon.workspace.service.WorkspaceMemberManagementService;
import org.dragon.workspace.service.WorkspaceTaskService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceApplicationProvider Workspace 应用提供者
 * 提供 WorkspaceApplication 实例的工厂类
 * 可以为每个 Workspace ID 创建或缓存 WorkspaceApplication 实例
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceApplicationProvider {

    private final WorkspaceLifecycleService workspaceLifecycleService;
    private final WorkspaceHiringService workspaceHiringService;
    private final WorkspaceActionLogService workspaceActionLogService;
    private final WorkspaceMemberManagementService workspaceMemberService;
    private final WorkspaceMaterialService materialService;
    private final WorkspaceTaskService workspaceTaskService;
    private final CharacterRegistry characterRegistry;

    /**
     * 获取 WorkspaceApplication 实例
     * 如果是首次调用，需要通过 Builder 构建
     * 后续可以缓存使用
     *
     * @param workspaceId Workspace ID
     * @return WorkspaceApplication 实例
     */
    public WorkspaceApplication getApplication(String workspaceId) {
        return new WorkspaceApplicationBuilder()
                .workspaceId(workspaceId)
                .workspaceLifecycleService(workspaceLifecycleService)
                .workspaceHiringService(workspaceHiringService)
                .workspaceActionLogService(workspaceActionLogService)
                .workspaceMemberService(workspaceMemberService)
                .materialService(materialService)
                .workspaceTaskService(workspaceTaskService)
                .characterRegistry(characterRegistry)
                .build();
    }

    /**
     * 执行即时任务
     *
     * @param characterId Character ID
     * @param userInput 用户输入
     * @return 执行结果
     */
    public String executeInstantTask(String characterId, String userInput) {
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        String result = character.run(userInput);
        log.info("[WorkspaceApplicationProvider] Executed instant task for character: {}", characterId);

        return result;
    }
}

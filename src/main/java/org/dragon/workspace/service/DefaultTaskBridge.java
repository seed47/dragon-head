package org.dragon.workspace.service;

import java.time.LocalDateTime;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认任务执行桥接器实现
 * 通过 Character#run() 真正执行任务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultTaskBridge implements TaskBridge {

    private final CharacterRegistry characterRegistry;
    private final TaskStore taskStore;
    private final CharacterRuntimeBinder characterRuntimeBinder;

    @Override
    public Task execute(Task task, String workspaceId) {
        String characterId = task.getCharacterId();
        if (characterId == null || characterId.isEmpty()) {
            log.error("[DefaultTaskBridge] No characterId assigned to task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No character assigned");
            taskStore.update(task);
            return task;
        }

        // 获取 Character
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalStateException("Character not found: " + characterId));

        // 绑定运行时依赖（ReActExecutor, PromptManager 等）
        characterRuntimeBinder.bind(character, workspaceId);

        // 更新任务状态为 RUNNING
        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        taskStore.update(task);

        try {
            // 将任务输入转为字符串
            String userInput = task.getInput() != null ? task.getInput().toString() : "";

            // 调用 Character 执行
            String result = character.run(userInput);

            // 执行成功
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            task.setResult(result);
            taskStore.update(task);

            log.info("[DefaultTaskBridge] Task {} executed successfully on character {}", task.getId(), characterId);
            return task;

        } catch (Exception e) {
            log.error("[DefaultTaskBridge] Task {} execution failed: {}", task.getId(), e.getMessage(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            taskStore.update(task);
            return task;
        }
    }

    @Override
    public Task suspend(Task task, String reason) {
        task.setStatus(TaskStatus.SUSPENDED);
        task.setErrorMessage(reason);
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[DefaultTaskBridge] Task {} suspended: {}", task.getId(), reason);
        return task;
    }

    @Override
    public Task resume(Task task, Object newInput) {
        // 更新输入（追加用户回复）
        if (newInput != null) {
            Object currentInput = task.getInput();
            task.setInput(currentInput != null ? currentInput.toString() + "\n" + newInput.toString() : newInput.toString());
        }

        // 恢复为 RUNNING
        task.setStatus(TaskStatus.RUNNING);
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[DefaultTaskBridge] Task {} resumed", task.getId());

        // 继续执行
        return execute(task, task.getWorkspaceId());
    }

    @Override
    public void notifyDependencyResolved(String taskId, String dependencyTaskId) {
        // 查找所有等待此依赖的任务
        // 当依赖任务完成后，将等待依赖的任务重新置为可执行
        // 目前是简单实现，后续可扩展为依赖图管理
        log.info("[DefaultTaskBridge] Dependency {} resolved for task {}", dependencyTaskId, taskId);
    }
}

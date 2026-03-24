package org.dragon.workspace.service;

import java.util.List;
import java.util.Optional;

import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务续跑解析器
 * 判断用户消息应该继续执行已有任务还是开启新任务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskContinuationResolver {

    private final TaskStore taskStore;

    /**
     * 续跑决策结果
     */
    public enum ContinuationDecision {
        /**
         * 继续已有任务
         */
        CONTINUE_EXISTING_TASK,
        /**
         * 开启新任务
         */
        START_NEW_TASK,
        /**
         * 需要更多信息来判断
         */
        NEEDS_MORE_INFO
    }

    /**
     * 续跑解析结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ContinuationResult {
        private ContinuationDecision decision;
        private String taskId;
        private String reason;
    }

    /**
     * 解析消息，判断应该继续任务还是开启新任务
     *
     * @param workspaceId Workspace ID
     * @param message 归一化的用户消息
     * @return 续跑解析结果
     */
    public ContinuationResult resolve(String workspaceId, NormalizedMessage message) {
        // 1. 检查是否引用了原消息（通过 quoteMessageId）
        if (message.getMessageId() != null && !message.getMessageId().isEmpty()) {
            Optional<Task> referencedTask = findTaskBySourceMessageId(workspaceId, message.getMessageId());
            if (referencedTask.isPresent()) {
                log.info("[TaskContinuationResolver] Message {} references task {}, continuing",
                        message.getMessageId(), referencedTask.get().getId());
                return ContinuationResult.builder()
                        .decision(ContinuationDecision.CONTINUE_EXISTING_TASK)
                        .taskId(referencedTask.get().getId())
                        .reason("Message references existing task")
                        .build();
            }
        }

        // 2. 检查 chat/thread/chatId 是否匹配挂起的任务
        if (message.getChatId() != null) {
            Optional<Task> waitingTask = findWaitingTaskByChatId(workspaceId, message.getChatId());
            if (waitingTask.isPresent()) {
                log.info("[TaskContinuationResolver] Found waiting task {} for chatId {}",
                        waitingTask.get().getId(), message.getChatId());
                return ContinuationResult.builder()
                        .decision(ContinuationDecision.CONTINUE_EXISTING_TASK)
                        .taskId(waitingTask.get().getId())
                        .reason("Matching chatId with waiting task")
                        .build();
            }
        }

        // 3. 检查是否有 WAITING_USER_INPUT 状态的任务
        Optional<Task> waitingUserInputTask = findWaitingUserInputTask(workspaceId);
        if (waitingUserInputTask.isPresent()) {
            // 检查消息是否是对上一个问题的回复
            Task task = waitingUserInputTask.get();
            if (isLikelyResponseToQuestion(message, task)) {
                log.info("[TaskContinuationResolver] Message appears to be response to question from task {}",
                        task.getId());
                return ContinuationResult.builder()
                        .decision(ContinuationDecision.CONTINUE_EXISTING_TASK)
                        .taskId(task.getId())
                        .reason("User response to waiting question")
                        .build();
            }
        }

        // 4. 默认开启新任务
        log.info("[TaskContinuationResolver] No matching continuation, starting new task");
        return ContinuationResult.builder()
                .decision(ContinuationDecision.START_NEW_TASK)
                .reason("No matching continuation criteria")
                .build();
    }

    /**
     * 通过源消息 ID 查找任务
     */
    private Optional<Task> findTaskBySourceMessageId(String workspaceId, String sourceMessageId) {
        List<Task> tasks = taskStore.findByWorkspaceId(workspaceId);
        return tasks.stream()
                .filter(task -> sourceMessageId.equals(task.getSourceMessageId()))
                .findFirst();
    }

    /**
     * 通过 chatId 查找等待中的任务
     */
    private Optional<Task> findWaitingTaskByChatId(String workspaceId, String chatId) {
        List<Task> tasks = taskStore.findByWorkspaceId(workspaceId);
        return tasks.stream()
                .filter(task -> chatId.equals(task.getSourceChatId()))
                .filter(task -> task.getStatus() == TaskStatus.SUSPENDED
                        || task.getStatus() == TaskStatus.WAITING_USER_INPUT
                        || task.getStatus() == TaskStatus.WAITING_DEPENDENCY)
                .findFirst();
    }

    /**
     * 查找等待用户输入的任务
     */
    private Optional<Task> findWaitingUserInputTask(String workspaceId) {
        List<Task> tasks = taskStore.findByWorkspaceId(workspaceId);
        return tasks.stream()
                .filter(task -> task.getWorkspaceId().equals(workspaceId))
                .filter(task -> task.getStatus() == TaskStatus.WAITING_USER_INPUT)
                .findFirst();
    }

    /**
     * 判断消息是否像是对问题的回复
     */
    private boolean isLikelyResponseToQuestion(NormalizedMessage message, Task task) {
        // 如果有 lastQuestion，说明这个任务之前问过用户问题
        if (task.getLastQuestion() != null && !task.getLastQuestion().isEmpty()) {
            // 简单判断：如果消息不是问句，更可能是回复
            String text = message.getTextContent();
            if (text != null && !text.trim().endsWith("?")) {
                return true;
            }
        }
        return false;
    }
}

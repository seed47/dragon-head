package org.dragon.organization.scheduler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.react.ReActContext;
import org.dragon.agent.react.ReActExecutor;
import org.dragon.agent.react.ReActResult;
import org.dragon.config.PromptKeys;
import org.dragon.config.PromptManager;
import org.dragon.organization.Organization;
import org.dragon.organization.member.OrganizationMember;
import org.dragon.organization.task.OrgTaskStatus;
import org.dragon.organization.task.OrganizationTask;
import org.dragon.organization.task.SubTask;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ReActTaskDecomposer 基于 Agent ReAct 的任务分解实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReActTaskDecomposer implements TaskDecomposer {

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个组织调度专家，负责把复杂任务拆解为可执行的子任务。";

    private final ReActExecutor reActExecutor;
    private final ModelRegistry modelRegistry;
    private final PromptManager promptManager;

    @Override
    public List<SubTask> decomposeWithReAct(OrganizationTask task,
                                             Organization organization,
                                             List<OrganizationMember> availableMembers) {
        if (task == null) {
            throw new IllegalArgumentException("OrganizationTask cannot be null");
        }

        if (availableMembers == null || availableMembers.isEmpty()) {
            log.warn("[ReActTaskDecomposer] No available members, fallback to simple decomposition");
            return decomposeSimple(task, organization, availableMembers);
        }

        try {
            // 获取 system prompt，支持 workspace-organization 层级查找
            // workspace 可以从 organization 获取，这里简化处理先使用 null
            String workspace = null;
            String organizationId = task.getOrganizationId();
            String systemPrompt = promptManager.getPrompt(workspace, organizationId, null, PromptKeys.REACT_TASK_DECOMPOSE);
            if (systemPrompt == null) {
                systemPrompt = DEFAULT_SYSTEM_PROMPT;
            }

            ReActContext context = ReActContext.builder()
                    .executionId(UUID.randomUUID().toString())
                    .characterId("org-scheduler-" + task.getOrganizationId())
                    .defaultModelId(resolveDefaultModelId().orElse(null))
                    .currentModelId(resolveDefaultModelId().orElse(null))
                    .userInput(buildDecomposePrompt(task, organization, availableMembers))
                    .systemPrompt(systemPrompt)
                    .maxIterations(1)
                    .build();

            ReActResult result = reActExecutor.execute(context);
            String content = extractReActContent(result);
            List<SubTask> parsed = parseSubTasks(content, task);

            if (!parsed.isEmpty()) {
                log.info("[ReActTaskDecomposer] ReAct decomposed task {} into {} sub tasks",
                        task.getId(), parsed.size());
                return parsed;
            }

            log.warn("[ReActTaskDecomposer] ReAct output cannot be parsed, fallback to simple decomposition, taskId={}",
                    task.getId());
            return decomposeSimple(task, organization, availableMembers);

        } catch (Exception e) {
            log.error("[ReActTaskDecomposer] ReAct decomposition failed, fallback to simple decomposition, taskId={}",
                    task.getId(), e);
            return decomposeSimple(task, organization, availableMembers);
        }
    }

    @Override
    public List<SubTask> decomposeSimple(OrganizationTask task,
                                         Organization organization,
                                         List<OrganizationMember> availableMembers) {
        String raw = firstNonBlank(task.getDescription(), task.getGoal(), task.getName(), "请完成任务");
        List<String> segments = splitToSegments(raw);
        List<SubTaskDraft> drafts = new ArrayList<>();

        for (String segment : segments) {
            if (segment != null && !segment.trim().isEmpty()) {
                SubTaskDraft draft = new SubTaskDraft();
                draft.description = segment.trim();
                draft.role = "executor";
                draft.input = task.getInput();
                drafts.add(draft);
            }
        }

        if (drafts.isEmpty()) {
            SubTaskDraft draft = new SubTaskDraft();
            draft.description = raw;
            draft.role = "executor";
            draft.input = task.getInput();
            drafts.add(draft);
        }

        return buildSubTasksFromDrafts(drafts, task);
    }

    private Optional<String> resolveDefaultModelId() {
        return modelRegistry.getDefault().map(m -> m.getId());
    }

    private String buildDecomposePrompt(OrganizationTask task,
                                         Organization organization,
                                         List<OrganizationMember> availableMembers) {
        StringBuilder builder = new StringBuilder();

        builder.append("请将任务拆解为多个可以由不同成员执行的子任务。\n");
        builder.append("输出必须是 JSON 数组，不要输出任何解释性文字，不要使用 Markdown 代码块。\n\n");

        builder.append("组织信息:\n");
        builder.append("- id: ").append(nullSafe(organization != null ? organization.getId() : null)).append("\n");
        builder.append("- name: ").append(nullSafe(organization != null ? organization.getName() : null)).append("\n");
        builder.append("- description: ").append(nullSafe(organization != null ? organization.getDescription() : null)).append("\n\n");

        builder.append("任务信息:\n");
        builder.append("- id: ").append(nullSafe(task.getId())).append("\n");
        builder.append("- name: ").append(nullSafe(task.getName())).append("\n");
        builder.append("- description: ").append(nullSafe(task.getDescription())).append("\n");
        builder.append("- goal: ").append(nullSafe(task.getGoal())).append("\n");
        builder.append("- constraints: ").append(nullSafe(task.getConstraints())).append("\n");
        builder.append("- input: ").append(task.getInput() == null ? "" : task.getInput().toString()).append("\n\n");

        builder.append("可用成员:\n");
        int idx = 1;
        for (OrganizationMember member : availableMembers) {
            builder.append(idx++)
                    .append(") characterId=").append(nullSafe(member.getCharacterId()))
                    .append(", role=").append(nullSafe(member.getRole()))
                    .append(", layer=").append(nullSafe(member.getLayer() != null ? member.getLayer().name() : null))
                    .append(", tags=").append(member.getTags() != null ? member.getTags() : "[]")
                    .append("\n");
        }

        builder.append("\n请输出 JSON 数组，每个元素包含：\n");
        builder.append("- description: 子任务描述\n");
        builder.append("- role: 建议角色\n");
        builder.append("- dependencies: 该子任务依赖的其他子任务索引列表（可选）\n");

        return builder.toString();
    }

    private String extractReActContent(ReActResult result) {
        if (result == null) {
            return null;
        }
        if (result.getResponse() != null && !result.getResponse().trim().isEmpty()) {
            return result.getResponse();
        }
        if (result.getThoughts() != null && !result.getThoughts().isEmpty()) {
            return result.getThoughts().get(result.getThoughts().size() - 1);
        }
        return null;
    }

    private List<SubTask> parseSubTasks(String content, OrganizationTask parentTask) {
        List<SubTask> subTasks = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return subTasks;
        }

        String json = extractJsonArray(content.trim());
        if (json == null) {
            return subTasks;
        }

        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) {
                return subTasks;
            }

            JsonArray array = root.getAsJsonArray();
            int order = 0;
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                SubTaskDraft draft = new SubTaskDraft();
                draft.description = getString(obj, "description");
                draft.role = getString(obj, "role");
                if (draft.role == null || draft.role.trim().isEmpty()) {
                    draft.role = "executor";
                }
                draft.input = parentTask.getInput();

                subTasks.add(buildSubTask(draft, parentTask, order++));
            }

        } catch (Exception e) {
            log.warn("[ReActTaskDecomposer] JSON parse failed: {}", e.getMessage());
        }

        return subTasks;
    }

    private String extractJsonArray(String content) {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return null;
    }

    private String getString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    private List<String> splitToSegments(String text) {
        List<String> segments = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return segments;
        }
        String[] parts = text.split("[。\\n;；]");
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                segments.add(part.trim());
            }
        }
        return segments;
    }

    private List<SubTask> buildSubTasksFromDrafts(List<SubTaskDraft> drafts, OrganizationTask parentTask) {
        List<SubTask> subTasks = new ArrayList<>();
        int order = 0;
        for (SubTaskDraft draft : drafts) {
            subTasks.add(buildSubTask(draft, parentTask, order++));
        }
        log.info("[ReActTaskDecomposer] Simple decomposition created {} sub tasks for task {}",
                subTasks.size(), parentTask.getId());
        return subTasks;
    }

    private SubTask buildSubTask(SubTaskDraft draft, OrganizationTask parentTask, int order) {
        return SubTask.builder()
                .id(UUID.randomUUID().toString())
                .organizationTaskId(parentTask.getId())
                .description(draft.description)
                .role(draft.role)
                .input(draft.input)
                .status(OrgTaskStatus.SUBMITTED)
                .order(order)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static class SubTaskDraft {
        String description;
        String role;
        Object input;
    }
}

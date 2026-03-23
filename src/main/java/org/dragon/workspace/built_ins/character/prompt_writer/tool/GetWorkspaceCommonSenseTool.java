package org.dragon.workspace.built_ins.character.prompt_writer.tool;

import java.util.List;
import java.util.Map;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.workspace.commons.CommonSenseService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 获取 Workspace CommonSense Prompt 的工具
 * 供 PromptWriter Character 在 ReAct 过程中按需调用
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetWorkspaceCommonSenseTool implements ToolConnector {

    private final CommonSenseService commonSenseService;

    @Override
    public String getName() {
        return "get_workspace_common_sense";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            String workspaceId = (String) params.get("workspaceId");
            if (workspaceId == null || workspaceId.isEmpty()) {
                return ToolResult.builder()
                        .success(false)
                        .errorMessage("Missing required parameter: workspaceId")
                        .build();
            }

            log.info("[GetWorkspaceCommonSenseTool] Generating common sense prompt for workspace: {}", workspaceId);
            String prompt = commonSenseService.generatePrompt(workspaceId);

            return ToolResult.builder()
                    .success(true)
                    .content(prompt != null ? prompt : "")
                    .build();

        } catch (Exception e) {
            log.error("[GetWorkspaceCommonSenseTool] Failed to get common sense", e);
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Failed to get common sense: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName())
                .description("获取指定 Workspace 的 CommonSense Prompt。当需要了解工作空间的常识规则、约束条件、角色边界或长期目标时调用此工具。")
                .inputParameters(List.of(
                        ToolParameter.builder()
                                .name("workspaceId")
                                .type("string")
                                .description("工作空间 ID")
                                .required(true)
                                .build()
                ))
                .outputParameter(ToolParameter.builder()
                        .name("commonSensePrompt")
                        .type("string")
                        .description("CommonSense Prompt 内容")
                        .build())
                .build();
    }
}

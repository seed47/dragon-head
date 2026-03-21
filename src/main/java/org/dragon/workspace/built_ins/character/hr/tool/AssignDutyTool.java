package org.dragon.workspace.built_ins.character.hr.tool;

import java.util.List;
import java.util.Map;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.workspace.service.WorkspaceHiringService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Assign Duty 工具
 * 供 HR Character 使用，为 Character 分配职责描述
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class AssignDutyTool implements ToolConnector {

    private final WorkspaceHiringService workspaceHiringService;

    @Override
    public String getName() {
        return "assign_duty";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            String workspaceId = (String) params.get("workspaceId");
            String characterId = (String) params.get("characterId");
            String dutyDescription = (String) params.get("dutyDescription");

            if (workspaceId == null || characterId == null || dutyDescription == null) {
                return ToolResult.builder()
                        .success(false)
                        .errorMessage("Missing required parameters: workspaceId, characterId or dutyDescription")
                        .build();
            }

            // 设置职责描述
            workspaceHiringService.setCharacterDuty(workspaceId, characterId, dutyDescription);

            return ToolResult.builder()
                    .success(true)
                    .content("Successfully assigned duty to character " + characterId + " in workspace " + workspaceId)
                    .build();

        } catch (Exception e) {
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Failed to assign duty: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName())
                .description("Assign a duty description to a character in a workspace. Use this tool to define what the character should do.")
                .inputParameters(List.of(
                        ToolParameter.builder()
                                .name("workspaceId")
                                .type("string")
                                .description("The workspace ID")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("characterId")
                                .type("string")
                                .description("The character ID")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("dutyDescription")
                                .type("string")
                                .description("The duty description for the character")
                                .required(true)
                                .build()
                ))
                .build();
    }
}

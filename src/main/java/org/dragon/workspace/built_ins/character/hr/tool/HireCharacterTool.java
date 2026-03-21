package org.dragon.workspace.built_ins.character.hr.tool;

import java.util.List;
import java.util.Map;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.workspace.WorkspaceService;
import org.dragon.workspace.hiring.HireMode;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Hire Character 工具
 * 供 HR Character 使用，执行雇佣操作
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class HireCharacterTool implements ToolConnector {

    private final WorkspaceService workspaceService;

    @Override
    public String getName() {
        return "hire_character";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            String workspaceId = (String) params.get("workspaceId");
            String characterId = (String) params.get("characterId");

            if (workspaceId == null || characterId == null) {
                return ToolResult.builder()
                        .success(false)
                        .errorMessage("Missing required parameters: workspaceId or characterId")
                        .build();
            }

            // 执行雇佣（使用 MANUAL 模式，因为是 HR 执行）
            workspaceService.hire(workspaceId, characterId, HireMode.MANUAL);

            return ToolResult.builder()
                    .success(true)
                    .content("Successfully hired character " + characterId + " to workspace " + workspaceId)
                    .build();

        } catch (Exception e) {
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Failed to hire character: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName())
                .description("Hire a character to a workspace. Use this tool when you need to add a character to your team.")
                .inputParameters(List.of(
                        ToolParameter.builder()
                                .name("workspaceId")
                                .type("string")
                                .description("The workspace ID to hire the character into")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("characterId")
                                .type("string")
                                .description("The character ID to hire")
                                .required(true)
                                .build()
                ))
                .build();
    }
}

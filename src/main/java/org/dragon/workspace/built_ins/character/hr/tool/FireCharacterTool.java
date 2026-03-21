package org.dragon.workspace.built_ins.character.hr.tool;

import java.util.List;
import java.util.Map;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.workspace.WorkspaceService;
import org.dragon.workspace.hiring.HireMode;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Fire Character 工具
 * 供 HR Character 使用，执行解雇操作
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class FireCharacterTool implements ToolConnector {

    private final WorkspaceService workspaceService;

    @Override
    public String getName() {
        return "fire_character";
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

            // 执行解雇（使用 MANUAL 模式）
            workspaceService.fire(workspaceId, characterId, HireMode.MANUAL);

            return ToolResult.builder()
                    .success(true)
                    .content("Successfully fired character " + characterId + " from workspace " + workspaceId)
                    .build();

        } catch (Exception e) {
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Failed to fire character: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName())
                .description("Fire a character from a workspace. Use this tool when you need to remove a character from your team.")
                .inputParameters(List.of(
                        ToolParameter.builder()
                                .name("workspaceId")
                                .type("string")
                                .description("The workspace ID to fire the character from")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("characterId")
                                .type("string")
                                .description("The character ID to fire")
                                .required(true)
                                .build()
                ))
                .build();
    }
}

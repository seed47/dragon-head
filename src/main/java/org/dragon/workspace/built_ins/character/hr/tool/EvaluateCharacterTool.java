package org.dragon.workspace.built_ins.character.hr.tool;

import java.util.List;
import java.util.Map;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Evaluate Character 工具
 * 供 HR Character 使用，评估 Character 是否适合某个 Workspace
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class EvaluateCharacterTool implements ToolConnector {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;

    @Override
    public String getName() {
        return "evaluate_character";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            String characterId = (String) params.get("characterId");
            String workspaceId = (String) params.get("workspaceId");

            if (characterId == null || workspaceId == null) {
                return ToolResult.builder()
                        .success(false)
                        .errorMessage("Missing required parameters: characterId or workspaceId")
                        .build();
            }

            // 验证 Character 存在
            Character character = characterRegistry.get(characterId)
                    .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

            // 验证 Workspace 存在
            workspaceRegistry.get(workspaceId)
                    .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

            // 构建评估结果
            StringBuilder content = new StringBuilder();
            content.append("Evaluation for character ").append(characterId).append(" in workspace ").append(workspaceId).append(":\n");
            content.append("- Name: ").append(character.getName()).append("\n");
            content.append("- Status: ").append(character.getStatus()).append("\n");
            content.append("- Description: ").append(character.getDescription() != null ? character.getDescription() : "N/A").append("\n");

            // 检查是否已在 workspace 中
            boolean isMember = character.getWorkspaceIds() != null && character.getWorkspaceIds().contains(workspaceId);
            content.append("- Already member: ").append(isMember).append("\n");

            return ToolResult.builder()
                    .success(true)
                    .content(content.toString())
                    .build();

        } catch (Exception e) {
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Failed to evaluate character: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName())
                .description("Evaluate a character's suitability for a workspace. Returns character information and compatibility assessment.")
                .inputParameters(List.of(
                        ToolParameter.builder()
                                .name("characterId")
                                .type("string")
                                .description("The character ID to evaluate")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("workspaceId")
                                .type("string")
                                .description("The workspace ID to evaluate against")
                                .required(true)
                                .build()
                ))
                .build();
    }
}

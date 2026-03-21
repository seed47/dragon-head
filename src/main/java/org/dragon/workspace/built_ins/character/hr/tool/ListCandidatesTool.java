package org.dragon.workspace.built_ins.character.hr.tool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.member.WorkspaceMemberManagementService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * List Candidates 工具
 * 供 HR Character 使用，列出可用的 Character 候选人
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class ListCandidatesTool implements ToolConnector {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceMemberManagementService memberManagementService;

    @Override
    public String getName() {
        return "list_candidates";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            String workspaceId = (String) params.get("workspaceId");

            if (workspaceId == null) {
                return ToolResult.builder()
                        .success(false)
                        .errorMessage("Missing required parameter: workspaceId")
                        .build();
            }

            // 获取当前 workspace 的成员
            List<String> currentMemberIds = memberManagementService.listMembers(workspaceId)
                    .stream()
                    .map(m -> m.getCharacterId())
                    .collect(Collectors.toList());

            // 获取所有可用 Character，排除已在 workspace 中的
            List<Character> availableCharacters = characterRegistry.listAll().stream()
                    .filter(c -> !currentMemberIds.contains(c.getId()))
                    .filter(c -> c.getStatus() == Character.Status.RUNNING)
                    .collect(Collectors.toList());

            StringBuilder content = new StringBuilder();
            content.append("Available candidates for workspace ").append(workspaceId).append(":\n");
            for (Character c : availableCharacters) {
                content.append("- ").append(c.getId()).append(": ").append(c.getName());
                if (c.getDescription() != null) {
                    content.append(" - ").append(c.getDescription());
                }
                content.append("\n");
            }

            return ToolResult.builder()
                    .success(true)
                    .content(content.toString())
                    .build();

        } catch (Exception e) {
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Failed to list candidates: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName())
                .description("List available character candidates for a workspace. This shows characters that are not yet members of the workspace.")
                .inputParameters(List.of(
                        ToolParameter.builder()
                                .name("workspaceId")
                                .type("string")
                                .description("The workspace ID to list candidates for")
                                .required(true)
                                .build()
                ))
                .build();
    }
}

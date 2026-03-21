package org.dragon.workspace.built_ins.character.member_selector;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * MemberSelector Character 工具工厂
 * 提供 MemberSelector Character 可用的工具列表
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemberSelectorCharacterTools {

    private final ToolRegistry toolRegistry;

    public MemberSelectorCharacterTools(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 获取 MemberSelector Character 可用的工具列表
     *
     * @return 工具列表
     */
    public List<ToolConnector> getAvailableTools() {
        return List.of(
                toolRegistry.get("list_workspace_members").orElse(null),
                toolRegistry.get("get_member_profile").orElse(null),
                toolRegistry.get("select_member").orElse(null)
        );
    }

    /**
     * 检查指定工具是否可用
     *
     * @param toolName 工具名称
     * @return 是否可用
     */
    public boolean isToolAvailable(String toolName) {
        return toolRegistry.exists(toolName);
    }
}

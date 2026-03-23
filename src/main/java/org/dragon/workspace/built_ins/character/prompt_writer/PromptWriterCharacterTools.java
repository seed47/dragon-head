package org.dragon.workspace.built_ins.character.prompt_writer;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.agent.tool.ToolRegistry;
import org.dragon.workspace.built_ins.character.commonsense_writer.CommonSenseWriterCharacterTools;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * PromptWriter Character 工具类
 * 提供 PromptWriter Character 可用的工具列表
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class PromptWriterCharacterTools {

    private final CommonSenseWriterCharacterTools commonSenseWriterCharacterTools;
    private final ToolRegistry toolRegistry;

    /**
     * 获取可用的工具列表
     *
     * @return 工具列表
     */
    public List<ToolConnector> getAvailableTools() {
        return toolRegistry.get("get_workspace_common_sense")
                .map(List::of)
                .orElse(List.of());
    }

    /**
     * 获取包含 CommonSense 的完整 prompt
     * 将 CommonSense Prompt 注入到常规 prompt 中
     *
     * @param workspaceId    Workspace ID
     * @param promptTemplate 常规 prompt 模板
     * @return 包含 CommonSense 的完整 prompt
     * @deprecated 使用 ReAct + get_workspace_common_sense 工具的拉取式架构，不再需要此方法
     */
    @Deprecated
    public String buildPromptWithCommonSense(String workspaceId, String promptTemplate) {
        // 1. 获取 CommonSense Prompt
        String commonSensePrompt = commonSenseWriterCharacterTools.generateCommonSensePrompt(workspaceId);

        // 2. 注入到常规 prompt 中
        if (commonSensePrompt != null && !commonSensePrompt.isEmpty()) {
            return promptTemplate + "\n\n" + commonSensePrompt;
        }

        return promptTemplate;
    }
}

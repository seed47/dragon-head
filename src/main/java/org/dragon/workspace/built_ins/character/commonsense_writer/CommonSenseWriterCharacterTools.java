package org.dragon.workspace.built_ins.character.commonsense_writer;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.workspace.commons.CommonSenseService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * CommonSenseWriter Character 工具类
 * 提供 CommonSenseWriter Character 可用的工具列表
 * 优先从缓存获取 prompt，缓存为空才触发生成
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class CommonSenseWriterCharacterTools {

    private final CommonSenseService commonSenseService;

    /**
     * 生成 CommonSense Prompt
     * 优先从缓存获取，缓存为空才触发生成
     *
     * @param workspaceId Workspace ID
     * @return 生成的 CommonSense Prompt
     */
    public String generateCommonSensePrompt(String workspaceId) {
        // 1. 优先从缓存获取
        String cached = commonSenseService.getCachedPrompt(workspaceId);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 2. 缓存为空时才触发生成
        String generated = commonSenseService.generatePrompt(workspaceId);

        // 3. 返回生成的 prompt (会被自动缓存)
        return generated;
    }

    /**
     * 检查是否有缓存
     *
     * @param workspaceId Workspace ID
     * @return 是否有缓存
     */
    public boolean hasCache(String workspaceId) {
        return commonSenseService.hasCache(workspaceId);
    }

    /**
     * 获取可用的工具列表
     *
     * @return 工具列表
     */
    public List<ToolConnector> getAvailableTools() {
        // CommonSenseWriter 目前不需要额外工具，prompt 生成由 LLM 直接处理
        return List.of();
    }
}

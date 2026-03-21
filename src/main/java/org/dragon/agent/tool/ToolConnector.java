package org.dragon.agent.tool;

import java.util.List;
import java.util.Map;

/**
 * 工具连接器接口
 * 提供各类工具的统一访问接口
 *
 * @author wyj
 * @version 1.0
 */
public interface ToolConnector {

    /**
     * 获取工具名称
     *
     * @return 工具名称
     */
    String getName();

    /**
     * 执行工具
     *
     * @param params 参数
     * @return 工具结果
     */
    ToolResult execute(Map<String, Object> params);

    /**
     * 获取工具 schema
     *
     * @return 工具 schema
     */
    ToolSchema getSchema();

    /**
     * 获取工具类型
     *
     * @return 工具类型
     */
    default ToolType getType() {
        return ToolType.CUSTOM;
    }

    /**
     * 工具结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class ToolResult {
        private boolean success;
        private String content;
        private Map<String, Object> data;
        private String errorMessage;
    }

    /**
     * 工具 Schema
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class ToolSchema {
        private String name;
        private String description;
        private List<ToolParameter> inputParameters;
        private ToolParameter outputParameter;
    }

    /**
     * 工具参数
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class ToolParameter {
        private String name;
        private String type;
        private String description;
        private boolean required;
        private Object defaultValue;
    }

    /**
     * 工具类型枚举
     */
    enum ToolType {
        /** 文件系统 */
        FILESYSTEM,
        /** 命令执行 */
        COMMAND,
        /** 浏览器 */
        BROWSER,
        /** 记忆 */
        MEMORY,
        /** 会话 */
        SESSION,
        /** 渠道 */
        CHANNEL,
        /** 定时任务 */
        CRON,
        /** 媒体 */
        MEDIA,
        /** 自定义 */
        CUSTOM
    }
}

package org.dragon.tools.policy;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 工具调用前钩子 — 在工具执行前运行插件钩子，可以阻止或
 * 修改参数。
 * 对应 TypeScript pi-tools.before-tool-call.ts。
 */
@Slf4j
public final class ToolBeforeCallHook {

    private ToolBeforeCallHook() {
    }

    /** 工具调用前钩子的结果。 */
    public sealed interface HookOutcome permits Blocked, Allowed {
    }

    public record Blocked(String reason) implements HookOutcome {
    }

    public record Allowed(Map<String, Object> params) implements HookOutcome {
    }

    /** 钩子的上下文。 */
    public record HookContext(String agentId, String sessionKey) {
    }

    /**
     * 运行工具调用前钩子。
     * 当前为存根实现 — 插件尚未在 Java 中集成。
     */
    @SuppressWarnings("unchecked")
    public static HookOutcome runBeforeToolCallHook(
            String toolName, Object params, String toolCallId, HookContext ctx) {
        // TODO: integrate with plugin hook runner when available
        Map<String, Object> paramsMap = params instanceof Map<?, ?> m
                ? (Map<String, Object>) m
                : Map.of();
        return new Allowed(paramsMap);
    }

    /**
     * 检查是否为 before_tool_call 注册了任何钩子。
     * 当前始终返回 false（存根实现）。
     */
    public static boolean hasBeforeToolCallHooks() {
        // TODO: check global hook runner
        return false;
    }
}

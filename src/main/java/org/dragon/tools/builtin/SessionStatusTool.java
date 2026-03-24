package org.dragon.tools.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dragon.tools.AgentTool;
import org.dragon.tools.ToolParamUtils;
import org.dragon.config.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 会话状态工具 — 显示会话信息、模型、时间。
 * 对应 TypeScript 的 tools/session-status-tool.ts（简化版）。
 *
 * <p>
 * 操作：
 * </p>
 * <ul>
 * <li>显示会话状态（sessionKey、模型、时间）</li>
 * <li>设置按会话的模型覆盖（未来功能）</li>
 * </ul>
 */
@Slf4j
public class SessionStatusTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TOOL_NAME = "session_status";

    private final String agentSessionKey;
    private final ConfigProperties config;

    private SessionStatusTool(String agentSessionKey, ConfigProperties config) {
        this.agentSessionKey = agentSessionKey;
        this.config = config;
    }

    public static SessionStatusTool create(String agentSessionKey, ConfigProperties config) {
        return new SessionStatusTool(agentSessionKey, config);
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "Show a /status-equivalent session status card (usage + time + cost when available). "
                + "Use for model-use questions (📊 session_status). "
                + "Optional: set per-session model override (model=default resets overrides).";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = MAPPER.createObjectNode();
        ObjectNode sessionKeyProp = MAPPER.createObjectNode();
        sessionKeyProp.put("type", "string");
        sessionKeyProp.put("description", "Session key to inspect (defaults to current session)");
        props.set("sessionKey", sessionKeyProp);
        ObjectNode modelProp = MAPPER.createObjectNode();
        modelProp.put("type", "string");
        modelProp.put("description",
                "Set model override for this session (e.g. 'gpt-4o', 'claude-3.5-sonnet'). Use 'default' to reset.");
        props.set("model", modelProp);
        schema.set("properties", props);
        return schema;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode params = context.getParameters();
                String requestedKey = ToolParamUtils.readStringParam(params, "sessionKey");
                String sessionKey = requestedKey != null ? requestedKey : agentSessionKey;
                String modelParam = ToolParamUtils.readStringParam(params, "model");

                if (sessionKey == null || sessionKey.isBlank()) {
                    return ToolResult.fail("sessionKey required");
                }

                // Resolve model info
                String defaultModel = resolveDefaultModel();
                String currentModel = defaultModel;
                boolean changedModel = false;

                if (modelParam != null && !modelParam.isBlank()) {
                    if ("default".equalsIgnoreCase(modelParam)) {
                        currentModel = defaultModel;
                        changedModel = true;
                        log.info("Session {} model reset to default: {}", sessionKey, defaultModel);
                    } else {
                        currentModel = modelParam;
                        changedModel = true;
                        log.info("Session {} model overridden to: {}", sessionKey, currentModel);
                    }
                }

                // Build status card
                StringBuilder sb = new StringBuilder();
                sb.append("📊 **Session Status**\n\n");
                sb.append("🔑 Session: `").append(sessionKey).append("`\n");
                sb.append("🤖 Model: `").append(currentModel).append("`");
                if (changedModel) {
                    sb.append(" (changed)");
                }
                sb.append("\n");

                // Time info
                String timezone = resolveTimezone();
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
                String timeStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                sb.append("🕒 Time: ").append(timeStr).append(" (").append(timezone).append(")\n");

                // Config path
                sb.append("⚙️ Config: loaded\n");

                String statusText = sb.toString();

                ObjectNode details = MAPPER.createObjectNode();
                details.put("ok", true);
                details.put("sessionKey", sessionKey);
                details.put("changedModel", changedModel);
                details.put("model", currentModel);
                details.put("statusText", statusText);

                return ToolResult.ok(statusText, details);

            } catch (Exception e) {
                log.error("[session_status] failed: {}", e.getMessage());
                return ToolResult.fail("session_status error: " + e.getMessage());
            }
        });
    }

    private String resolveDefaultModel() {
        // ConfigProperties doesn't have agents config, return default
        return "anthropic/claude-sonnet-4-20250514";
    }

    private String resolveTimezone() {
        // Use system default timezone (no userTimezone field in config yet)
        return ZoneId.systemDefault().getId();
    }
}

package org.dragon.tools.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dragon.tools.AgentTool;
import org.dragon.tools.ToolParamUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * 画布工具 — 控制节点画布（显示/隐藏/导航/求值/截图/A2UI）。
 * 对应 TypeScript 的 tools/canvas-tool.ts。
 */
@Slf4j
public class CanvasTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] CANVAS_ACTIONS = {
            "present", "hide", "navigate", "eval", "snapshot", "a2ui_push", "a2ui_reset"
    };

    @Override
    public String getName() {
        return "canvas";
    }

    @Override
    public String getDescription() {
        return "Control node canvases (present/hide/navigate/eval/snapshot/A2UI).";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        ObjectNode action = properties.putObject("action");
        action.put("type", "string");
        ArrayNode ae = action.putArray("enum");
        for (String a : CANVAS_ACTIONS)
            ae.add(a);

        addStr(properties, "node", "Target node id");
        addStr(properties, "gatewayUrl", "Gateway URL override");
        addStr(properties, "gatewayToken", "Gateway auth token");
        addStr(properties, "target", "URL to present");
        addStr(properties, "url", "URL to navigate to");
        addStr(properties, "javaScript", "JavaScript to evaluate");
        addStr(properties, "outputFormat", "Snapshot format: png, jpg");
        addStr(properties, "jsonl", "JSONL payload for A2UI");
        addStr(properties, "jsonlPath", "File path to JSONL");

        addNum(properties, "x", "X position");
        addNum(properties, "y", "Y position");
        addNum(properties, "width", "Canvas width");
        addNum(properties, "height", "Canvas height");
        addNum(properties, "maxWidth", "Max snapshot width");
        addNum(properties, "quality", "JPEG quality (0-100)");
        addNum(properties, "delayMs", "Delay before snapshot");

        schema.putArray("required").add("action");
        return schema;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolContext context) {
        return CompletableFuture.supplyAsync(() -> doExecute(context));
    }

    private ToolResult doExecute(ToolContext context) {
        try {
            JsonNode params = context.getParameters();
            String action = ToolParamUtils.readStringParam(params, "action");
            if (action == null || action.isBlank())
                return ToolResult.fail("'action' is required");

            log.info("canvas-tool: action={}", action);

            // TODO: Wire up to gateway node.invoke canvas.* API
            ObjectNode result = MAPPER.createObjectNode();
            result.put("action", action);
            result.put("ok", true);
            result.put("note", "Canvas stub — wire up gateway canvas API");
            return ToolResult.ok(ToolParamUtils.toJsonString(result), result);

        } catch (Exception e) {
            log.error("canvas-tool error: {}", e.getMessage(), e);
            return ToolResult.fail("Canvas tool error: " + e.getMessage());
        }
    }

    private static void addStr(ObjectNode props, String key, String desc) {
        ObjectNode p = props.putObject(key);
        p.put("type", "string");
        p.put("description", desc);
    }

    private static void addNum(ObjectNode props, String key, String desc) {
        ObjectNode p = props.putObject(key);
        p.put("type", "number");
        p.put("description", desc);
    }
}

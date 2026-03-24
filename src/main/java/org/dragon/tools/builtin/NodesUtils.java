package org.dragon.tools.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点工具的节点解析工具类。
 * 对应 TypeScript agents/tools/nodes-utils.ts。
 */
@Slf4j
public final class NodesUtils {

    private NodesUtils() {
    }

    /**
     * 节点列表条目。
     */
    public record NodeInfo(
            String nodeId,
            String displayName,
            String platform,
            String version,
            String coreVersion,
            String uiVersion,
            String remoteIp,
            String deviceFamily,
            String modelIdentifier,
            List<String> caps,
            List<String> commands,
            boolean paired,
            boolean connected) {
    }

    /**
     * 从网关响应解析节点列表。
     */
    public static List<NodeInfo> parseNodeList(JsonNode value) {
        if (value == null || !value.isObject())
            return List.of();
        JsonNode nodesArr = value.get("nodes");
        if (nodesArr == null || !nodesArr.isArray())
            return List.of();

        List<NodeInfo> result = new ArrayList<>();
        for (JsonNode n : nodesArr) {
            if (!n.isObject())
                continue;
            result.add(new NodeInfo(
                    n.path("nodeId").asText(""),
                    n.path("displayName").asText(null),
                    n.path("platform").asText(null),
                    n.path("version").asText(null),
                    n.path("coreVersion").asText(null),
                    n.path("uiVersion").asText(null),
                    n.path("remoteIp").asText(null),
                    n.path("deviceFamily").asText(null),
                    n.path("modelIdentifier").asText(null),
                    parseStringArray(n.get("caps")),
                    parseStringArray(n.get("commands")),
                    n.path("paired").asBoolean(false),
                    n.path("connected").asBoolean(false)));
        }
        return result;
    }

    /**
     * 规范化节点键以便匹配。
     */
    public static String normalizeNodeKey(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "").replaceAll("-+$", "");
    }

    /**
     * 从节点列表中选择默认节点。
     * 优先选择已连接且具有画布功能的节点，然后是本地 Mac 节点。
     */
    public static NodeInfo pickDefaultNode(List<NodeInfo> nodes) {
        List<NodeInfo> withCanvas = nodes.stream()
                .filter(n -> n.caps == null || n.caps.isEmpty() || n.caps.contains("canvas"))
                .toList();
        if (withCanvas.isEmpty())
            return null;

        List<NodeInfo> connected = withCanvas.stream().filter(NodeInfo::connected).toList();
        List<NodeInfo> candidates = connected.isEmpty() ? withCanvas : connected;
        if (candidates.size() == 1)
            return candidates.get(0);

        List<NodeInfo> local = candidates.stream()
                .filter(n -> n.platform != null && n.platform.toLowerCase().startsWith("mac")
                        && n.nodeId.startsWith("mac-"))
                .toList();
        if (local.size() == 1)
            return local.get(0);

        return null;
    }

    /**
     * 通过查询字符串从列表中解析节点 ID。
     *
     * @param nodes        可用节点
     * @param query        节点查询（ID、名称、IP 或前缀）
     * @param allowDefault 如果为 true 且查询为空，则选择默认节点
     * @return 解析后的节点 ID
     * @throws IllegalArgumentException 如果节点未找到或不明确
     */
    public static String resolveNodeIdFromList(List<NodeInfo> nodes, String query, boolean allowDefault) {
        String q = (query == null ? "" : query).trim();
        if (q.isEmpty()) {
            if (allowDefault) {
                NodeInfo picked = pickDefaultNode(nodes);
                if (picked != null)
                    return picked.nodeId;
            }
            throw new IllegalArgumentException("node required");
        }

        String qNorm = normalizeNodeKey(q);
        List<NodeInfo> matches = nodes.stream().filter(n -> {
            if (n.nodeId.equals(q))
                return true;
            if (n.remoteIp != null && n.remoteIp.equals(q))
                return true;
            String name = n.displayName != null ? n.displayName : "";
            if (!name.isEmpty() && normalizeNodeKey(name).equals(qNorm))
                return true;
            if (q.length() >= 6 && n.nodeId.startsWith(q))
                return true;
            return false;
        }).toList();

        if (matches.size() == 1)
            return matches.get(0).nodeId;
        if (matches.isEmpty()) {
            String known = nodes.stream()
                    .map(n -> n.displayName != null ? n.displayName : (n.remoteIp != null ? n.remoteIp : n.nodeId))
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new IllegalArgumentException("unknown node: " + q +
                    (known.isEmpty() ? "" : " (known: " + known + ")"));
        }
        String matchNames = matches.stream()
                .map(n -> n.displayName != null ? n.displayName : (n.remoteIp != null ? n.remoteIp : n.nodeId))
                .reduce((a, b) -> a + ", " + b).orElse("");
        throw new IllegalArgumentException("ambiguous node: " + q + " (matches: " + matchNames + ")");
    }

    private static List<String> parseStringArray(JsonNode node) {
        if (node == null || !node.isArray())
            return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual())
                result.add(item.asText());
        }
        return result;
    }
}

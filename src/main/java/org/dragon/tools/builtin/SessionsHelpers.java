package org.dragon.tools.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import org.dragon.config.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 会话工具的共享辅助类。
 * 对应 TypeScript sessions-helpers.ts。
 */
@Slf4j
public class SessionsHelpers {

    private static final Pattern SESSION_ID_RE = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE);

    // --- Session kind classification ---

    public enum SessionKind {
        MAIN("main"), GROUP("group"), CRON("cron"), HOOK("hook"), NODE("node"), OTHER("other");

        private final String value;

        SessionKind(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SessionKind fromString(String s) {
            if (s == null)
                return OTHER;
            return switch (s.toLowerCase()) {
                case "main" -> MAIN;
                case "group" -> GROUP;
                case "cron" -> CRON;
                case "hook" -> HOOK;
                case "node" -> NODE;
                default -> OTHER;
            };
        }
    }

    // --- Main session alias resolution ---

    public static String normalizeMainKey(String mainKey) {
        if (mainKey == null)
            return "main";
        String trimmed = mainKey.trim();
        return trimmed.isEmpty() ? "main" : trimmed;
    }

    public static MainSessionAlias resolveMainSessionAlias(ConfigProperties cfg) {
        // ConfigProperties doesn't have getSession(), use defaults
        String mainKey = normalizeMainKey(null);
        String scope = "per-sender";
        String alias = "global".equals(scope) ? "global" : mainKey;
        return new MainSessionAlias(mainKey, alias, scope);
    }

    public record MainSessionAlias(String mainKey, String alias, String scope) {
    }

    // --- Session key display/internal resolution ---

    public static String resolveDisplaySessionKey(String key, String alias, String mainKey) {
        if (key.equals(alias) || key.equals(mainKey)) {
            return "main";
        }
        return key;
    }

    public static String resolveInternalSessionKey(String key, String alias, String mainKey) {
        if ("main".equals(key)) {
            return alias;
        }
        return key;
    }

    // --- Session key classification ---

    public static boolean looksLikeSessionId(String value) {
        return SESSION_ID_RE.matcher(value.trim()).matches();
    }

    public static boolean looksLikeSessionKey(String value) {
        String raw = value.trim();
        if (raw.isEmpty())
            return false;
        if ("main".equals(raw) || "global".equals(raw) || "unknown".equals(raw))
            return true;
        if (raw.startsWith("agent:"))
            return true;
        if (raw.startsWith("cron:") || raw.startsWith("hook:"))
            return true;
        if (raw.startsWith("node-") || raw.startsWith("node:"))
            return true;
        if (raw.contains(":group:") || raw.contains(":channel:"))
            return true;
        return false;
    }

    public static boolean shouldResolveSessionIdInput(String value) {
        return looksLikeSessionId(value) || !looksLikeSessionKey(value);
    }

    // --- Session kind classification ---

    public static SessionKind classifySessionKind(String key, String gatewayKind,
            String alias, String mainKey) {
        if (key.equals(alias) || key.equals(mainKey))
            return SessionKind.MAIN;
        if (key.startsWith("cron:"))
            return SessionKind.CRON;
        if (key.startsWith("hook:"))
            return SessionKind.HOOK;
        if (key.startsWith("node-") || key.startsWith("node:"))
            return SessionKind.NODE;
        if ("group".equals(gatewayKind))
            return SessionKind.GROUP;
        if (key.contains(":group:") || key.contains(":channel:"))
            return SessionKind.GROUP;
        return SessionKind.OTHER;
    }

    // --- Channel derivation ---

    public static String deriveChannel(String key, SessionKind kind,
            String channel, String lastChannel) {
        if (kind == SessionKind.CRON || kind == SessionKind.HOOK || kind == SessionKind.NODE) {
            return "internal";
        }
        if (channel != null && !channel.isBlank())
            return channel.trim();
        if (lastChannel != null && !lastChannel.isBlank())
            return lastChannel.trim();
        String[] parts = key.split(":");
        List<String> nonEmpty = Arrays.stream(parts).filter(p -> !p.isEmpty()).toList();
        if (nonEmpty.size() >= 3 && ("group".equals(nonEmpty.get(1)) || "channel".equals(nonEmpty.get(1)))) {
            return nonEmpty.get(0);
        }
        return "unknown";
    }

    // --- Agent-to-agent policy ---

    public static AgentToAgentPolicy createAgentToAgentPolicy(ConfigProperties cfg) {
        // ConfigProperties doesn't have tools config, return default policy (disabled)
        return new AgentToAgentPolicy(false, List.of());
    }

    public static class AgentToAgentPolicy {
        private final boolean enabled;
        private final List<String> allowPatterns;

        public AgentToAgentPolicy(boolean enabled, List<String> allowPatterns) {
            this.enabled = enabled;
            this.allowPatterns = allowPatterns != null ? allowPatterns : List.of();
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean matchesAllow(String agentId) {
            if (allowPatterns.isEmpty())
                return true;
            for (String pattern : allowPatterns) {
                String raw = pattern == null ? "" : pattern.trim();
                if (raw.isEmpty())
                    continue;
                if ("*".equals(raw))
                    return true;
                if (!raw.contains("*")) {
                    if (raw.equals(agentId))
                        return true;
                } else {
                    // Convert glob to regex
                    String regex = Pattern.quote(raw).replace("\\*", ".*");
                    if (Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE)
                            .matcher(agentId).matches()) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean isAllowed(String requesterAgentId, String targetAgentId) {
            if (Objects.equals(requesterAgentId, targetAgentId))
                return true;
            if (!enabled)
                return false;
            return matchesAllow(requesterAgentId) && matchesAllow(targetAgentId);
        }
    }

    // --- Agent ID resolution from session key ---

    public static String resolveAgentIdFromSessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank())
            return "default";
        String trimmed = sessionKey.trim();
        if (trimmed.startsWith("agent:")) {
            String[] parts = trimmed.split(":");
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                return parts[1];
            }
        }
        return "default";
    }

    public static boolean isSubagentSessionKey(String sessionKey) {
        return sessionKey != null && sessionKey.contains(":subagent:");
    }

    // --- Message filtering ---

    public static List<JsonNode> stripToolMessages(List<JsonNode> messages) {
        if (messages == null)
            return List.of();
        return messages.stream()
                .filter(msg -> {
                    if (msg == null || !msg.isObject())
                        return true;
                    String role = msg.has("role") ? msg.get("role").asText("") : "";
                    return !"toolResult".equals(role);
                })
                .toList();
    }

    // --- Text extraction ---

    public static String extractAssistantText(JsonNode message) {
        if (message == null || !message.isObject())
            return null;
        String role = message.has("role") ? message.get("role").asText("") : "";
        if (!"assistant".equals(role))
            return null;
        JsonNode content = message.get("content");
        if (content == null || !content.isArray())
            return null;
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : content) {
            if (block == null || !block.isObject())
                continue;
            if (!"text".equals(block.path("type").asText("")))
                continue;
            String text = block.path("text").asText("");
            if (!text.isBlank()) {
                sb.append(text);
            }
        }
        String joined = sb.toString().trim();
        return joined.isEmpty() ? null : joined;
    }

    // --- Sandbox visibility ---

    public static String resolveSandboxSessionToolsVisibility(ConfigProperties cfg) {
        // ConfigProperties doesn't have agents config, return default
        return "spawned";
    }

    // --- Session reference resolution (stub — gateway call) ---

    /**
     * Resolve a session reference (key or sessionId) via the gateway.
     * Currently returns a simple direct resolution without gateway call.
     */
    public static SessionReferenceResolution resolveSessionReference(
            String sessionKey, String alias, String mainKey,
            String requesterInternalKey, boolean restrictToSpawned) {
        String raw = sessionKey.trim();

        if (shouldResolveSessionIdInput(raw)) {
            // TODO: call gateway sessions.resolve for sessionId lookup
            log.debug("Session reference looks like sessionId, direct resolution: {}", raw);
        }

        String resolvedKey = resolveInternalSessionKey(raw, alias, mainKey);
        String displayKey = resolveDisplaySessionKey(resolvedKey, alias, mainKey);
        return SessionReferenceResolution.ok(resolvedKey, displayKey, false);
    }

    public sealed interface SessionReferenceResolution {
        record Ok(String key, String displayKey, boolean resolvedViaSessionId)
                implements SessionReferenceResolution {
        }

        record Error(String status, String error) implements SessionReferenceResolution {
        }

        static SessionReferenceResolution ok(String key, String displayKey, boolean resolvedViaSessionId) {
            return new Ok(key, displayKey, resolvedViaSessionId);
        }

        static SessionReferenceResolution error(String status, String error) {
            return new Error(status, error);
        }

        default boolean isOk() {
            return this instanceof Ok;
        }
    }
}

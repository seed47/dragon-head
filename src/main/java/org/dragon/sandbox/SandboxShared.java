package org.dragon.sandbox;

import org.dragon.sandbox.SandboxTypes.SandboxScope;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 沙箱共享工具函数
 * 提供会话键处理和作用域解析等功能
 */
public final class SandboxShared {

    private SandboxShared() {
    }

    /**
     * 将会话键转换为安全的目录/容器名称后缀
     * 格式：{slugified-key}-{hash}
     *
     * @param value 原始会话键
     * @return 安全的文件名/容器名后缀
     */
    public static String slugifySessionKey(String value) {
        String trimmed = value == null || value.isBlank() ? "session" : value.trim();
        String hash = sha1Hex(trimmed).substring(0, 8);
        String safe = trimmed.toLowerCase()
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        String base = safe.length() > 32 ? safe.substring(0, 32) : safe;
        if (base.isEmpty())
            base = "session";
        return base + "-" + hash;
    }

    /**
     * 解析给定会话键对应的工作空间目录
     *
     * @param root 工作空间根目录
     * @param sessionKey 会话键
     * @return 工作空间目录路径
     */
    public static String resolveSandboxWorkspaceDir(String root, String sessionKey) {
        String resolvedRoot = resolvePath(root);
        String slug = slugifySessionKey(sessionKey);
        return Paths.get(resolvedRoot, slug).toString();
    }

    /**
     * 解析沙箱作用域键
     * 用于确定容器的分组方式
     *
     * @param scope 沙箱作用域
     * @param sessionKey 会话键
     * @return 作用域键
     */
    public static String resolveSandboxScopeKey(SandboxScope scope, String sessionKey) {
        String trimmed = sessionKey == null || sessionKey.isBlank() ? "main" : sessionKey.trim();
        if (scope == SandboxScope.SHARED) {
            return "shared";
        }
        if (scope == SandboxScope.SESSION) {
            return trimmed;
        }
        // AGENT 作用域：从会话键中提取 Agent ID
        String agentId = resolveAgentIdFromSessionKey(trimmed);
        return "agent:" + agentId;
    }

    /**
     * 从作用域键解析 Agent ID
     *
     * @param scopeKey 作用域键
     * @return Agent ID，如果不存在则返回 null
     */
    public static String resolveSandboxAgentId(String scopeKey) {
        if (scopeKey == null || scopeKey.isBlank() || "shared".equals(scopeKey.trim())) {
            return null;
        }
        String trimmed = scopeKey.trim();
        String[] parts = trimmed.split(":");
        if (parts.length >= 2 && "agent".equals(parts[0]) && !parts[1].isEmpty()) {
            return normalizeAgentId(parts[1]);
        }
        return resolveAgentIdFromSessionKey(trimmed);
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────

    /**
     * 从会话键中提取 Agent ID
     * 格式：agentId/sessionName 或只有 sessionName
     */
    static String resolveAgentIdFromSessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank())
            return "main";
        String trimmed = sessionKey.trim();
        int slashIdx = trimmed.indexOf('/');
        if (slashIdx > 0 && slashIdx < trimmed.length() - 1) {
            return normalizeAgentId(trimmed.substring(0, slashIdx));
        }
        return "main";
    }

    /**
     * 标准化 Agent ID
     */
    static String normalizeAgentId(String id) {
        if (id == null || id.isBlank())
            return "main";
        return id.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "-");
    }

    /**
     * 解析路径，将 ~ 展开为用户主目录
     */
    static String resolvePath(String filePath) {
        if (filePath == null)
            return System.getProperty("user.home");
        String normalized = filePath.trim();
        if ("~".equals(normalized)) {
            return System.getProperty("user.home");
        }
        if (normalized.startsWith("~/")) {
            return System.getProperty("user.home") + normalized.substring(1);
        }
        return normalized;
    }

    /**
     * 计算字符串的 SHA-1 哈希值（十六进制表示）
     */
    private static String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 不可用", e);
        }
    }
}

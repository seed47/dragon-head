package org.dragon.sandbox;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 沙箱配置默认常量
 */
public final class SandboxConstants {

    private SandboxConstants() {
    }

    // ── 工作空间 ───────────────────────────────────────────────────

    /** 默认沙箱工作空间根目录 */
    public static final String DEFAULT_SANDBOX_WORKSPACE_ROOT = Paths
            .get(System.getProperty("user.home"), ".dragon", "sandboxes").toString();

    // ── Docker ──────────────────────────────────────────────────────

    /** 默认沙箱镜像 */
    public static final String DEFAULT_SANDBOX_IMAGE = "dragonhead-sandbox:bookworm-slim";
    /** 容器名称前缀 */
    public static final String DEFAULT_SANDBOX_CONTAINER_PREFIX = "dragon-sbx-";
    /** 容器内工作目录 */
    public static final String DEFAULT_SANDBOX_WORKDIR = "/workspace";
    /** 空闲容器清理阈值（小时） */
    public static final int DEFAULT_SANDBOX_IDLE_HOURS = 24;
    /** 容器最大保留天数 */
    public static final int DEFAULT_SANDBOX_MAX_AGE_DAYS = 7;

    // ── 工具允许/拒绝列表默认值 ────────────────────────────────────

    /** 默认允许的工具列表 */
    public static final List<String> DEFAULT_TOOL_ALLOW = List.of(
            "exec", "process", "read", "write", "edit", "apply_patch",
            "image", "sessions_list", "sessions_history", "sessions_send",
            "sessions_spawn", "session_status");

    /** 默认拒绝的工具列表 */
    public static final List<String> DEFAULT_TOOL_DENY = List.of(
            "browser", "canvas", "nodes", "cron", "gateway",
            // 渠道 ID
            "discord", "slack", "telegram", "whatsapp", "line", "imessage", "signal");

    // ── 浏览器沙箱 ─────────────────────────────────────────────

    /** 浏览器沙箱镜像 */
    public static final String DEFAULT_SANDBOX_BROWSER_IMAGE = "dragonhead-sandbox-browser:bookworm-slim";
    /** 通用沙箱镜像 */
    public static final String DEFAULT_SANDBOX_COMMON_IMAGE = "dragonhead-sandbox-common:bookworm-slim";
    /** 浏览器容器名称前缀 */
    public static final String DEFAULT_SANDBOX_BROWSER_PREFIX = "dragonhead-sbx-browser-";
    /** Chrome DevTools Protocol 端口 */
    public static final int DEFAULT_SANDBOX_BROWSER_CDP_PORT = 9222;
    /** VNC 端口 */
    public static final int DEFAULT_SANDBOX_BROWSER_VNC_PORT = 5900;
    /** noVNC 端口 */
    public static final int DEFAULT_SANDBOX_BROWSER_NOVNC_PORT = 6080;
    /** 浏览器自动启动超时时间（毫秒） */
    public static final int DEFAULT_SANDBOX_BROWSER_AUTOSTART_TIMEOUT_MS = 12_000;

    /** Agent 工作空间挂载路径 */
    public static final String SANDBOX_AGENT_WORKSPACE_MOUNT = "/agent";

    // ── 状态目录 ───────────────────────────────────────────

    private static final String STATE_DIR = System.getenv().getOrDefault("DRAGON_STATE_DIR",
            Paths.get(System.getProperty("user.home"), ".dragon").toString());

    public static final Path SANDBOX_STATE_DIR = Paths.get(STATE_DIR, "sandbox");
    public static final Path SANDBOX_REGISTRY_PATH = SANDBOX_STATE_DIR.resolve("containers.json");
    public static final Path SANDBOX_BROWSER_REGISTRY_PATH = SANDBOX_STATE_DIR.resolve("browsers.json");
}

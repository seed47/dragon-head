package org.dragon.sandbox;

import org.dragon.sandbox.SandboxDocker.ContainerState;
import org.dragon.sandbox.SandboxRegistry.SandboxBrowserRegistryEntry;
import org.dragon.sandbox.SandboxRegistry.SandboxRegistryEntry;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * 沙箱容器管理
 * 提供容器的列表、删除、查看等操作
 */
@Slf4j
public final class SandboxManager {

    private SandboxManager() {
    }

    // ── 浏览器桥接缓存（内存中） ────────────────────────────

    /** 浏览器桥接条目 */
    @Data
    @Builder
    public static class BrowserBridgeEntry {
        private String bridgeUrl;
        private String containerName;
    }

    /** 浏览器桥接缓存 */
    private static final Map<String, BrowserBridgeEntry> BROWSER_BRIDGES = new LinkedHashMap<>();

    /**
     * 获取浏览器桥接缓存
     *
     * @return 浏览器桥接映射
     */
    public static Map<String, BrowserBridgeEntry> getBrowserBridges() {
        return BROWSER_BRIDGES;
    }

    // ── 容器信息类型 ────────────────────────────────────────

    /** 沙箱容器信息 */
    @Data
    @Builder
    public static class SandboxContainerInfo {
        private String containerName;
        private String sessionKey;
        private long createdAtMs;
        private long lastUsedAtMs;
        private String image;
        private String configHash;
        private boolean running;
        private boolean imageMatch;
    }

    /** 沙箱浏览器信息 */
    @Data
    @Builder
    public static class SandboxBrowserInfo {
        private String containerName;
        private String sessionKey;
        private long createdAtMs;
        private long lastUsedAtMs;
        private String image;
        private int cdpPort;
        private Integer noVncPort;
        private boolean running;
        private boolean imageMatch;
    }

    // ── 列表操作 ─────────────────────────────────────────────

    /**
     * 列出所有沙箱容器及其当前 Docker 状态
     *
     * @return 容器信息列表
     */
    public static List<SandboxContainerInfo> listSandboxContainers() throws IOException {
        List<SandboxRegistryEntry> entries = SandboxRegistry.readRegistry();
        List<SandboxContainerInfo> results = new ArrayList<>();

        for (SandboxRegistryEntry entry : entries) {
            results.add(buildContainerInfo(entry));
        }

        return results;
    }

    /**
     * 列出所有沙箱浏览器容器及其当前 Docker 状态
     *
     * @return 浏览器信息列表
     */
    public static List<SandboxBrowserInfo> listSandboxBrowsers() throws IOException {
        List<SandboxBrowserRegistryEntry> entries = SandboxRegistry.readBrowserRegistry();
        List<SandboxBrowserInfo> results = new ArrayList<>();

        for (SandboxBrowserRegistryEntry entry : entries) {
            results.add(buildBrowserInfo(entry));
        }

        return results;
    }

    /**
     * 构建容器信息
     */
    private static SandboxContainerInfo buildContainerInfo(SandboxRegistryEntry entry) throws IOException {
        ContainerState state = SandboxDocker.dockerContainerState(entry.getContainerName());
        String actualImage = getActualImage(entry.getContainerName(), entry.getImage(), state.isExists());

        return SandboxContainerInfo.builder()
                .containerName(entry.getContainerName())
                .sessionKey(entry.getSessionKey())
                .createdAtMs(entry.getCreatedAtMs())
                .lastUsedAtMs(entry.getLastUsedAtMs())
                .image(actualImage)
                .configHash(entry.getConfigHash())
                .running(state.isRunning())
                .imageMatch(actualImage.equals(entry.getImage()))
                .build();
    }

    /**
     * 构建浏览器信息
     */
    private static SandboxBrowserInfo buildBrowserInfo(SandboxBrowserRegistryEntry entry) throws IOException {
        ContainerState state = SandboxDocker.dockerContainerState(entry.getContainerName());
        String actualImage = getActualImage(entry.getContainerName(), entry.getImage(), state.isExists());

        return SandboxBrowserInfo.builder()
                .containerName(entry.getContainerName())
                .sessionKey(entry.getSessionKey())
                .createdAtMs(entry.getCreatedAtMs())
                .lastUsedAtMs(entry.getLastUsedAtMs())
                .image(actualImage)
                .cdpPort(entry.getCdpPort())
                .noVncPort(entry.getNoVncPort())
                .running(state.isRunning())
                .imageMatch(actualImage.equals(entry.getImage()))
                .build();
    }

    /**
     * 获取容器的实际镜像
     */
    private static String getActualImage(String containerName, String registeredImage, boolean exists) {
        if (!exists) {
            return registeredImage;
        }
        try {
            var result = SandboxDocker.execDocker(
                    List.of("inspect", "-f", "{{.Config.Image}}", containerName), true);
            if (result.getCode() == 0) {
                return result.getStdout().trim();
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return registeredImage;
    }

    // ── 删除操作 ───────────────────────────────────────────

    /**
     * 删除沙箱容器及其注册表条目
     *
     * @param containerName 容器名称
     */
    public static void removeSandboxContainer(String containerName) {
        try {
            SandboxDocker.execDocker(List.of("rm", "-f", containerName), true);
        } catch (Exception e) {
            // 忽略删除失败
        }
        SandboxRegistry.removeRegistryEntry(containerName);
    }

    /**
     * 删除沙箱浏览器容器及其注册表条目
     *
     * @param containerName 容器名称
     */
    public static void removeSandboxBrowserContainer(String containerName) {
        try {
            SandboxDocker.execDocker(List.of("rm", "-f", containerName), true);
        } catch (Exception e) {
            // 忽略删除失败
        }
        SandboxRegistry.removeBrowserRegistryEntry(containerName);

        // 移除浏览器桥接（如果存在）
        BROWSER_BRIDGES.entrySet().removeIf(entry -> containerName.equals(entry.getValue().getContainerName()));
    }
}

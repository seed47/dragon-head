package org.dragon.sandbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * 沙箱容器和浏览器注册表
 * 使用 JSON 文件持久化存储容器信息
 */
@Slf4j
public final class SandboxRegistry {

    private SandboxRegistry() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── 注册表条目类型 ────────────────────────────────────────

    /** 容器注册表条目 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SandboxRegistryEntry {
        private String containerName;
        private String sessionKey;
        private long createdAtMs;
        private long lastUsedAtMs;
        private String image;
        private String configHash;
    }

    /** 浏览器容器注册表条目 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SandboxBrowserRegistryEntry {
        private String containerName;
        private String sessionKey;
        private long createdAtMs;
        private long lastUsedAtMs;
        private String image;
        private int cdpPort;
        private Integer noVncPort;
    }

    /** 注册表泛型容器 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Registry<T> {
        private List<T> entries = new ArrayList<>();
    }

    // ── 辅助方法 ──────────────────────────────────────────────

    /**
     * 根据容器名称查找注册表条目
     */
    private static <T> T findByContainerName(List<T> entries, String containerName) {
        for (T entry : entries) {
            if (entry instanceof SandboxRegistryEntry) {
                if (((SandboxRegistryEntry) entry).getContainerName().equals(containerName)) {
                    return entry;
                }
            } else if (entry instanceof SandboxBrowserRegistryEntry) {
                if (((SandboxBrowserRegistryEntry) entry).getContainerName().equals(containerName)) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * 合并容器注册表条目
     */
    private static SandboxRegistryEntry mergeEntry(SandboxRegistryEntry entry, SandboxRegistryEntry existing, Integer cdpPort) {
        return SandboxRegistryEntry.builder()
                .containerName(entry.getContainerName())
                .sessionKey(entry.getSessionKey())
                .createdAtMs(existing != null ? existing.getCreatedAtMs() : entry.getCreatedAtMs())
                .lastUsedAtMs(entry.getLastUsedAtMs())
                .image(existing != null ? existing.getImage() : entry.getImage())
                .configHash(entry.getConfigHash() != null ? entry.getConfigHash()
                        : existing != null ? existing.getConfigHash() : null)
                .build();
    }

    /**
     * 合并浏览器注册表条目
     */
    private static SandboxBrowserRegistryEntry mergeEntry(SandboxBrowserRegistryEntry entry, SandboxBrowserRegistryEntry existing, int cdpPort, Integer noVncPort) {
        return SandboxBrowserRegistryEntry.builder()
                .containerName(entry.getContainerName())
                .sessionKey(entry.getSessionKey())
                .createdAtMs(existing != null ? existing.getCreatedAtMs() : entry.getCreatedAtMs())
                .lastUsedAtMs(entry.getLastUsedAtMs())
                .image(existing != null ? existing.getImage() : entry.getImage())
                .cdpPort(cdpPort)
                .noVncPort(noVncPort)
                .build();
    }

    // ── 容器注册表 ──────────────────────────────────────────

    /**
     * 读取容器注册表
     *
     * @return 容器注册表条目列表
     */
    public static List<SandboxRegistryEntry> readRegistry() {
        return readJsonList(SandboxConstants.SANDBOX_REGISTRY_PATH.toString(),
                new TypeReference<Registry<SandboxRegistryEntry>>() {
                });
    }

    /**
     * 更新容器注册表
     *
     * @param entry 容器注册表条目
     */
    public static void updateRegistry(SandboxRegistryEntry entry) {
        List<SandboxRegistryEntry> entries = readRegistry();
        SandboxRegistryEntry existing = findByContainerName(entries, entry.getContainerName());

        entries.removeIf(e -> e.getContainerName().equals(entry.getContainerName()));
        entries.add(mergeEntry(entry, existing, null));

        writeJsonList(SandboxConstants.SANDBOX_REGISTRY_PATH.toString(), entries, "entries");
    }

    /**
     * 从注册表中移除容器
     *
     * @param containerName 容器名称
     */
    public static void removeRegistryEntry(String containerName) {
        List<SandboxRegistryEntry> entries = readRegistry();
        int before = entries.size();
        entries.removeIf(e -> e.getContainerName().equals(containerName));
        if (entries.size() < before) {
            writeJsonList(SandboxConstants.SANDBOX_REGISTRY_PATH.toString(), entries, "entries");
        }
    }

    // ── 浏览器注册表 ────────────────────────────────────────────

    /**
     * 读取浏览器注册表
     *
     * @return 浏览器注册表条目列表
     */
    public static List<SandboxBrowserRegistryEntry> readBrowserRegistry() {
        return readJsonList(SandboxConstants.SANDBOX_BROWSER_REGISTRY_PATH.toString(),
                new TypeReference<Registry<SandboxBrowserRegistryEntry>>() {
                });
    }

    /**
     * 更新浏览器注册表
     *
     * @param entry 浏览器注册表条目
     */
    public static void updateBrowserRegistry(SandboxBrowserRegistryEntry entry) {
        List<SandboxBrowserRegistryEntry> entries = readBrowserRegistry();
        SandboxBrowserRegistryEntry existing = findByContainerName(entries, entry.getContainerName());

        entries.removeIf(e -> e.getContainerName().equals(entry.getContainerName()));
        entries.add(mergeEntry(entry, existing, entry.getCdpPort(), entry.getNoVncPort()));

        writeJsonList(SandboxConstants.SANDBOX_BROWSER_REGISTRY_PATH.toString(), entries, "entries");
    }

    /**
     * 从注册表中移除浏览器
     *
     * @param containerName 容器名称
     */
    public static void removeBrowserRegistryEntry(String containerName) {
        List<SandboxBrowserRegistryEntry> entries = readBrowserRegistry();
        int before = entries.size();
        entries.removeIf(e -> e.getContainerName().equals(containerName));
        if (entries.size() < before) {
            writeJsonList(SandboxConstants.SANDBOX_BROWSER_REGISTRY_PATH.toString(), entries, "entries");
        }
    }

    // ── JSON 辅助方法 ────────────────────────────────────────────────

    /**
     * 从 JSON 文件读取列表
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> readJsonList(String path, TypeReference<Registry<T>> type) {
        try {
            String raw = Files.readString(Path.of(path));
            Registry<T> registry = MAPPER.readValue(raw, type);
            if (registry != null && registry.getEntries() != null) {
                return new ArrayList<>(registry.getEntries());
            }
        } catch (IOException e) {
            // 文件不存在或损坏，返回空列表
        }
        return new ArrayList<>();
    }

    /**
     * 写入列表到 JSON 文件
     */
    private static <T> void writeJsonList(String path, List<T> entries, String key) {
        try {
            Path p = Path.of(path);
            Files.createDirectories(p.getParent());
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put(key, entries);
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper) + "\n";
            Files.writeString(p, json);
        } catch (IOException e) {
            log.error("写入沙箱注册表失败: {}", e.getMessage());
        }
    }
}

package org.dragon.sandbox;

import org.dragon.sandbox.SandboxTypes.SandboxConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * 沙箱容器清理器
 * 自动清理空闲超时或超过最大保留时间的容器和浏览器
 */
@Slf4j
public final class SandboxPruner {

    private SandboxPruner() {
    }

    /** 上次清理时间戳 */
    private static volatile long lastPruneAtMs = 0;
    /** 清理间隔（5分钟） */
    private static final long PRUNE_INTERVAL_MS = 5 * 60 * 1000;

    /**
     * 定时清理沙箱容器
     * 如果距离上次清理已超过间隔时间，则执行清理操作
     *
     * @param cfg 沙箱配置
     */
    public static void maybePruneSandboxes(SandboxConfig cfg) {
        long now = System.currentTimeMillis();
        if (now - lastPruneAtMs < PRUNE_INTERVAL_MS) {
            return;
        }
        lastPruneAtMs = now;
        try {
            pruneSandboxContainers(cfg);
            pruneSandboxBrowsers(cfg);
        } catch (Exception e) {
            log.error("沙箱清理失败: {}", e.getMessage());
        }
    }

    /**
     * 清理空闲或过期的沙箱容器
     */
    private static void pruneSandboxContainers(SandboxConfig cfg) throws IOException {
        List<SandboxRegistry.SandboxRegistryEntry> entries = SandboxRegistry.readRegistry();
        pruneEntries(entries, cfg, SandboxRegistry::removeRegistryEntry);
    }

    /**
     * 清理空闲或过期的浏览器容器
     */
    private static void pruneSandboxBrowsers(SandboxConfig cfg) throws IOException {
        List<SandboxRegistry.SandboxBrowserRegistryEntry> entries = SandboxRegistry.readBrowserRegistry();
        pruneBrowserEntries(entries, cfg, SandboxRegistry::removeBrowserRegistryEntry);
    }

    /**
     * 判断是否应该删除容器
     */
    private static boolean shouldPrune(long lastUsedAtMs, long createdAtMs, int idleHours, int maxAgeDays) {
        long now = System.currentTimeMillis();
        long idleMs = now - lastUsedAtMs;
        long ageMs = now - createdAtMs;
        return (idleHours > 0 && idleMs > (long) idleHours * 60 * 60 * 1000) ||
                (maxAgeDays > 0 && ageMs > (long) maxAgeDays * 24 * 60 * 60 * 1000);
    }

    /**
     * 清理容器条目
     */
    private static void pruneEntries(
            List<SandboxRegistry.SandboxRegistryEntry> entries,
            SandboxConfig cfg,
            java.util.function.Consumer<String> remover) throws IOException {
        int idleHours = cfg.getPrune().getIdleHours();
        int maxAgeDays = cfg.getPrune().getMaxAgeDays();
        if (idleHours == 0 && maxAgeDays == 0)
            return;

        for (var entry : entries) {
            if (shouldPrune(entry.getLastUsedAtMs(), entry.getCreatedAtMs(), idleHours, maxAgeDays)) {
                try {
                    SandboxDocker.execDocker(List.of("rm", "-f", entry.getContainerName()), true);
                } catch (Exception e) {
                    // 忽略清理失败
                } finally {
                    remover.accept(entry.getContainerName());
                }
            }
        }
    }

    /**
     * 清理浏览器容器条目
     */
    private static void pruneBrowserEntries(
            List<SandboxRegistry.SandboxBrowserRegistryEntry> entries,
            SandboxConfig cfg,
            java.util.function.Consumer<String> remover) throws IOException {
        int idleHours = cfg.getPrune().getIdleHours();
        int maxAgeDays = cfg.getPrune().getMaxAgeDays();
        if (idleHours == 0 && maxAgeDays == 0)
            return;

        for (var entry : entries) {
            if (shouldPrune(entry.getLastUsedAtMs(), entry.getCreatedAtMs(), idleHours, maxAgeDays)) {
                try {
                    SandboxDocker.execDocker(List.of("rm", "-f", entry.getContainerName()), true);
                } catch (Exception e) {
                    // 忽略清理失败
                } finally {
                    remover.accept(entry.getContainerName());
                }
            }
        }
    }

    /**
     * 确保指定的 Docker 容器正在运行
     * 如果容器存在但已停止，则尝试启动它
     *
     * @param containerName 容器名称
     */
    public static void ensureDockerContainerIsRunning(String containerName) throws IOException {
        var state = SandboxDocker.dockerContainerState(containerName);
        if (state.isExists() && !state.isRunning()) {
            SandboxDocker.execDocker(List.of("start", containerName));
        }
    }
}

package org.dragon.character.mind.skills;

import org.dragon.config.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 技能文件监视器和快照版本控制。
 * 监视工作区/托管/额外技能目录的更改，并增加快照版本以触发提示词重建。
 * 对应 TypeScript 中的 skills/refresh.ts。
 */
@Slf4j
public final class SkillRefresh {

    private SkillRefresh() {
    }

    // ── 忽略的模式 (与 TS DEFAULT_SKILLS_WATCH_IGNORED 相同) ──
    public static final List<Pattern> DEFAULT_WATCH_IGNORED = Arrays.asList(
            Pattern.compile("(^|[\\\\/])\\.git([\\\\/]|$)"),
            Pattern.compile("(^|[\\\\/])node_modules([\\\\/]|$)"),
            Pattern.compile("(^|[\\\\/])dist([\\\\/]|$)"));

    // ── 变更事件 (Change event) ────────────────────────────────────────────────

    public static class SkillsChangeEvent {
        private final String workspaceDir;
        private final ChangeReason reason;
        private final String changedPath;

        public SkillsChangeEvent(String workspaceDir, ChangeReason reason, String changedPath) {
            this.workspaceDir = workspaceDir;
            this.reason = reason;
            this.changedPath = changedPath;
        }

        public String getWorkspaceDir() {
            return workspaceDir;
        }

        public ChangeReason getReason() {
            return reason;
        }

        public String getChangedPath() {
            return changedPath;
        }

        public enum ChangeReason {
            WATCH, MANUAL, REMOTE_NODE
        }
    }

    // ── 状态 (State) ───────────────────────────────────────────────────────

    private static final Set<Consumer<SkillsChangeEvent>> listeners = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> workspaceVersions = new ConcurrentHashMap<>();
    private static final Map<String, WatchState> watchers = new ConcurrentHashMap<>();
    private static volatile long globalVersion = 0;

    private static class WatchState {
        private final WatchService service;
        private final String pathsKey;
        private final long debounceMs;
        private final ScheduledFuture<?> timer;
        private final Thread watchThread;

        public WatchState(WatchService service, String pathsKey, long debounceMs, ScheduledFuture<?> timer, Thread watchThread) {
            this.service = service;
            this.pathsKey = pathsKey;
            this.debounceMs = debounceMs;
            this.timer = timer;
            this.watchThread = watchThread;
        }

        public WatchService getService() {
            return service;
        }

        public String getPathsKey() {
            return pathsKey;
        }

        public long getDebounceMs() {
            return debounceMs;
        }

        public ScheduledFuture<?> getTimer() {
            return timer;
        }

        public Thread getWatchThread() {
            return watchThread;
        }
    }

    // ── 监听器 (Listeners) ───────────────────────────────────────────────────

    /**
     * 注册技能变更事件的监听器。
     * 返回一个用于取消注册的 Runnable。
     */
    public static Runnable registerChangeListener(Consumer<SkillsChangeEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    // ── 版本管理 (Version management) ──────────────────────────────────────────

    /**
     * 增加工作区的快照版本（如果为 null 则增加全局版本）。
     */
    public static long bumpSnapshotVersion(String workspaceDir,
            SkillsChangeEvent.ChangeReason reason,
            String changedPath) {
        if (workspaceDir != null && !workspaceDir.trim().isEmpty()) {
            long current = workspaceVersions.getOrDefault(workspaceDir, 0L);
            long next = bumpVersion(current);
            workspaceVersions.put(workspaceDir, next);
            emit(new SkillsChangeEvent(workspaceDir, reason, changedPath));
            return next;
        }
        globalVersion = bumpVersion(globalVersion);
        emit(new SkillsChangeEvent(null, reason, changedPath));
        return globalVersion;
    }

    /**
     * 获取工作区的当前快照版本。
     */
    public static long getSnapshotVersion(String workspaceDir) {
        if (workspaceDir == null || workspaceDir.trim().isEmpty()) {
            return globalVersion;
        }
        long local = workspaceVersions.getOrDefault(workspaceDir, 0L);
        return Math.max(globalVersion, local);
    }

    // ── 监视器管理 (Watcher management) ──────────────────────────────────────────

    /**
     * 确保给定工作区的文件监视器正在运行。
     */
    public static void ensureWatcher(String workspaceDir, ConfigProperties config) {
        if (workspaceDir == null || workspaceDir.trim().isEmpty())
            return;

        boolean watchEnabled = config == null
                || config.getSkills() == null
                || config.getSkills().getLoad() == null
                || config.getSkills().getLoad().getWatch() != Boolean.FALSE;

        long debounceMs = 250;
        if (config != null && config.getSkills() != null
                && config.getSkills().getLoad() != null
                && config.getSkills().getLoad().getWatchDebounceMs() != null) {
            debounceMs = Math.max(0, config.getSkills().getLoad().getWatchDebounceMs());
        }

        WatchState existing = watchers.get(workspaceDir);
        if (!watchEnabled) {
            if (existing != null) {
                stopWatcher(workspaceDir, existing);
            }
            return;
        }

        List<String> watchPaths = resolveWatchPaths(workspaceDir, config);
        String pathsKey = String.join("|", watchPaths);
        if (existing != null && existing.getPathsKey().equals(pathsKey)
                && existing.getDebounceMs() == debounceMs) {
            return; // 已经使用相同的配置进行监视
        }

        if (existing != null) {
            stopWatcher(workspaceDir, existing);
        }

        // 启动新的监视器
        try {
            startWatcher(workspaceDir, watchPaths, pathsKey, debounceMs);
        } catch (IOException e) {
            log.warn("启动技能监视器失败 {}: {}", workspaceDir, e.getMessage());
        }
    }

    // ── 内部方法 (Internal) ────────────────────────────────────────────────────

    private static long bumpVersion(long current) {
        long now = System.currentTimeMillis();
        return now <= current ? current + 1 : now;
    }

    private static void emit(SkillsChangeEvent event) {
        for (Consumer<SkillsChangeEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.warn("技能变更监听器执行失败: {}", e.getMessage());
            }
        }
    }

    private static List<String> resolveWatchPaths(String workspaceDir, ConfigProperties config) {
        List<String> paths = new ArrayList<>();
        if (!workspaceDir.trim().isEmpty()) {
            paths.add(Paths.get(workspaceDir, "skills").toString());
        }
        // 托管技能目录
        String configDir = System.getProperty("user.home") + "/.dragonhead";
        paths.add(Paths.get(configDir, "skills").toString());

        // 来自配置的额外目录
        if (config != null && config.getSkills() != null
                && config.getSkills().getLoad() != null
                && config.getSkills().getLoad().getExtraDirs() != null) {
            for (String dir : config.getSkills().getLoad().getExtraDirs()) {
                if (dir != null && !dir.trim().isEmpty()) {
                    paths.add(dir.trim());
                }
            }
        }
        return paths;
    }

    private static void startWatcher(String workspaceDir, List<String> watchPaths,
            String pathsKey, long debounceMs) throws IOException {
        WatchService service = FileSystems.getDefault().newWatchService();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "skills-watcher-" + workspaceDir.hashCode());
            t.setDaemon(true);
            return t;
        });

        for (String watchPath : watchPaths) {
            Path p = Paths.get(watchPath);
            if (Files.isDirectory(p)) {
                try {
                    p.register(service,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE);
                } catch (IOException e) {
                    log.debug("无法监视 {}: {}", watchPath, e.getMessage());
                }
            }
        }

        Thread watchThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WatchKey key = service.take();
                    List<WatchEvent<?>> events = key.pollEvents();
                    for (WatchEvent<?> event : events) {
                        String changed = event.context() != null ? event.context().toString() : null;
                        scheduler.schedule(() -> bumpSnapshotVersion(workspaceDir,
                                SkillsChangeEvent.ChangeReason.WATCH, changed),
                                debounceMs, TimeUnit.MILLISECONDS);
                    }
                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ClosedWatchServiceException e) {
                    break;
                }
            }
        }, "skills-watch-poll-" + workspaceDir.hashCode());
        watchThread.setDaemon(true);
        watchThread.start();

        watchers.put(workspaceDir, new WatchState(service, pathsKey, debounceMs, null, watchThread));
    }

    private static void stopWatcher(String workspaceDir, WatchState state) {
        watchers.remove(workspaceDir);
        try {
            state.getService().close();
        } catch (IOException e) {
            // 忽略
        }
        if (state.getWatchThread() != null) {
            state.getWatchThread().interrupt();
        }
    }
}
package org.dragon.sandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Docker 命令执行、镜像管理和容器生命周期
 */
@Slf4j
public final class SandboxDocker {

    private SandboxDocker() {
    }

    /** Docker 命令执行结果 */
    @Data
    @Builder
    public static class ExecResult {
        private String stdout;
        private String stderr;
        private int code;
    }

    /**
     * 执行 Docker 命令并捕获输出
     *
     * @param args Docker 命令参数
     * @param allowFailure 是否允许命令失败
     * @return 执行结果
     */
    public static ExecResult execDocker(List<String> args, boolean allowFailure) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(command)
                .redirectErrorStream(false);
        Process proc = pb.start();

        String stdout;
        String stderr;
        try (InputStream stdoutStream = proc.getInputStream();
                InputStream stderrStream = proc.getErrorStream()) {
            stdout = new String(stdoutStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            stderr = new String(stderrStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        try {
            proc.waitFor(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Docker command interrupted");
        }

        int code = proc.exitValue();
        if (code != 0 && !allowFailure) {
            throw new IOException(stderr.isBlank() ? "docker " + String.join(" ", args) + " failed" : stderr.trim());
        }
        return ExecResult.builder().stdout(stdout).stderr(stderr).code(code).build();
    }

    public static ExecResult execDocker(List<String> args) throws IOException {
        return execDocker(args, false);
    }

    /**
     * 读取容器映射的 Docker 端口
     *
     * @param containerName 容器名称
     * @param port 内部端口
     * @return 映射的主机端口，如果不存在则返回 null
     */
    public static Integer readDockerPort(String containerName, int port) throws IOException {
        ExecResult result = execDocker(List.of("port", containerName, port + "/tcp"), true);
        if (result.code != 0)
            return null;
        String line = result.stdout.trim().split("\\R")[0];
        var matcher = java.util.regex.Pattern.compile(":(\\d+)\\s*$").matcher(line);
        if (!matcher.find())
            return null;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 检查 Docker 镜像是否在本地存在
     *
     * @param image 镜像名称
     * @return 是否存在
     */
    public static boolean dockerImageExists(String image) throws IOException {
        ExecResult result = execDocker(List.of("image", "inspect", image), true);
        if (result.code == 0)
            return true;
        if (result.stderr.contains("No such image"))
            return false;
        throw new IOException("Failed to inspect sandbox image: " + result.stderr.trim());
    }

    /**
     * 确保沙箱 Docker 镜像可用
     * 如果镜像不存在，则拉取默认镜像
     *
     * @param image 镜像名称
     */
    public static void ensureDockerImage(String image) throws IOException {
        if (dockerImageExists(image))
            return;
        if (SandboxConstants.DEFAULT_SANDBOX_IMAGE.equals(image)) {
            execDocker(List.of("pull", "debian:bookworm-slim"));
            execDocker(List.of("tag", "debian:bookworm-slim", SandboxConstants.DEFAULT_SANDBOX_IMAGE));
            return;
        }
        throw new IOException("Sandbox image not found: " + image + ". Build or pull it first.");
    }

    /** 容器运行状态 */
    @Data
    @Builder
    public static class ContainerState {
        private boolean exists;
        private boolean running;
    }

    /**
     * 获取容器的运行状态
     *
     * @param name 容器名称
     * @return 容器状态
     */
    public static ContainerState dockerContainerState(String name) throws IOException {
        ExecResult result = execDocker(List.of("inspect", "-f", "{{.State.Running}}", name), true);
        if (result.code != 0) {
            return ContainerState.builder().exists(false).running(false).build();
        }
        return ContainerState.builder()
                .exists(true)
                .running("true".equals(result.stdout.trim()))
                .build();
    }

    /**
     * 构建沙箱容器的 Docker create 命令参数
     */
    public static List<String> buildSandboxCreateArgs(
            String name,
            SandboxTypes.SandboxDockerConfig cfg,
            String scopeKey,
            long createdAtMs,
            Map<String, String> labels,
            String configHash) {

        List<String> args = new ArrayList<>();
        args.addAll(List.of("create", "--name", name));
        args.addAll(List.of("--label", "dragon.sandbox=1"));
        args.addAll(List.of("--label", "dragon.sessionKey=" + scopeKey));
        args.addAll(List.of("--label", "dragon.createdAtMs=" + createdAtMs));
        if (configHash != null) {
            args.addAll(List.of("--label", "dragon.configHash=" + configHash));
        }
        if (labels != null) {
            labels.forEach((k, v) -> {
                if (k != null && !k.isBlank() && v != null && !v.isBlank()) {
                    args.addAll(List.of("--label", k + "=" + v));
                }
            });
        }
        if (cfg.isReadOnlyRoot()) {
            args.add("--read-only");
        }
        if (cfg.getTmpfs() != null) {
            cfg.getTmpfs().forEach(t -> args.addAll(List.of("--tmpfs", t)));
        }
        if (cfg.getNetwork() != null && !cfg.getNetwork().isBlank()) {
            args.addAll(List.of("--network", cfg.getNetwork()));
        }
        if (cfg.getUser() != null && !cfg.getUser().isBlank()) {
            args.addAll(List.of("--user", cfg.getUser()));
        }
        if (cfg.getCapDrop() != null) {
            cfg.getCapDrop().forEach(cap -> args.addAll(List.of("--cap-drop", cap)));
        }
        args.addAll(List.of("--security-opt", "no-new-privileges"));
        if (cfg.getSeccompProfile() != null) {
            args.addAll(List.of("--security-opt", "seccomp=" + cfg.getSeccompProfile()));
        }
        if (cfg.getApparmorProfile() != null) {
            args.addAll(List.of("--security-opt", "apparmor=" + cfg.getApparmorProfile()));
        }
        if (cfg.getDns() != null) {
            cfg.getDns().stream().filter(d -> !d.isBlank()).forEach(d -> args.addAll(List.of("--dns", d)));
        }
        if (cfg.getExtraHosts() != null) {
            cfg.getExtraHosts().stream().filter(h -> !h.isBlank()).forEach(h -> args.addAll(List.of("--add-host", h)));
        }
        if (cfg.getPidsLimit() != null && cfg.getPidsLimit() > 0) {
            args.addAll(List.of("--pids-limit", String.valueOf(cfg.getPidsLimit())));
        }
        if (cfg.getMemory() != null && !cfg.getMemory().isBlank()) {
            args.addAll(List.of("--memory", cfg.getMemory()));
        }
        if (cfg.getMemorySwap() != null && !cfg.getMemorySwap().isBlank()) {
            args.addAll(List.of("--memory-swap", cfg.getMemorySwap()));
        }
        if (cfg.getCpus() != null && cfg.getCpus() > 0) {
            args.addAll(List.of("--cpus", String.valueOf(cfg.getCpus())));
        }
        if (cfg.getBinds() != null) {
            cfg.getBinds().forEach(bind -> args.addAll(List.of("-v", bind)));
        }
        return args;
    }

    /**
     * 从运行中的容器读取配置哈希标签
     *
     * @param containerName 容器名称
     * @return 配置哈希值，如果不存在则返回 null
     */
    public static String readContainerConfigHash(String containerName) throws IOException {
        ExecResult result = execDocker(
                List.of("inspect", "-f", "{{ index .Config.Labels \"dragon.configHash\" }}", containerName),
                true);
        if (result.code != 0)
            return null;
        String raw = result.stdout.trim();
        if (raw.isEmpty() || "<no value>".equals(raw))
            return null;
        return raw;
    }
}

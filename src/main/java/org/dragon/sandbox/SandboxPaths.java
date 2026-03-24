package org.dragon.sandbox;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;

/**
 * 沙箱路径解析与安全检查
 * 确保路径不会逃逸出沙箱根目录，并阻止符号链接
 */
public final class SandboxPaths {

    private SandboxPaths() {
    }

    private static final Pattern UNICODE_SPACES = Pattern.compile("[\\u00A0\\u2000-\\u200A\\u202F\\u205F\\u3000]");
    private static final Pattern HTTP_URL_RE = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_URL_RE = Pattern.compile("^data:", Pattern.CASE_INSENSITIVE);

    /** 路径解析结果 */
    public record ResolvedPath(String resolved, String relative) {
    }

    /**
     * 解析沙箱内的文件路径，验证不会逃逸出沙箱根目录
     *
     * @param filePath 文件路径
     * @param cwd 当前工作目录
     * @param root 沙箱根目录
     * @return 解析后的路径
     * @throws SandboxPathEscapeException 路径逃逸时抛出
     */
    public static ResolvedPath resolveSandboxPath(String filePath, String cwd, String root)
            throws SandboxPathEscapeException {
        String resolved = resolveToCwd(filePath, cwd);
        String rootResolved = Path.of(root).toAbsolutePath().normalize().toString();
        Path resolvedPath = Path.of(resolved).toAbsolutePath().normalize();
        Path rootPath = Path.of(rootResolved);

        try {
            Path relative = rootPath.relativize(resolvedPath);
            String relStr = relative.toString();

            if (relStr.isEmpty()) {
                return new ResolvedPath(resolved, "");
            }
            if (relStr.startsWith("..") || Path.of(relStr).isAbsolute()) {
                throw new SandboxPathEscapeException(
                        String.format("路径逃逸沙箱根目录 (%s): %s", shortPath(rootResolved), filePath));
            }
            return new ResolvedPath(resolvedPath.toString(), relStr);
        } catch (IllegalArgumentException e) {
            throw new SandboxPathEscapeException(
                    String.format("路径逃逸沙箱根目录 (%s): %s", shortPath(rootResolved), filePath));
        }
    }

    /**
     * 验证路径在沙箱根目录内且不是符号链接
     *
     * @param filePath 文件路径
     * @param cwd 当前工作目录
     * @param root 沙箱根目录
     * @return 解析后的路径
     */
    public static ResolvedPath assertSandboxPath(String filePath, String cwd, String root)
            throws SandboxPathEscapeException, IOException {
        ResolvedPath resolved = resolveSandboxPath(filePath, cwd, root);
        assertNoSymlink(resolved.relative(), Path.of(root).toAbsolutePath().normalize().toString());
        return resolved;
    }

    /**
     * 断言媒体不是 data: URL
     *
     * @param media 媒体路径
     */
    public static void assertMediaNotDataUrl(String media) {
        if (media != null && DATA_URL_RE.matcher(media.trim()).find()) {
            throw new IllegalArgumentException("不支持 data: URL 作为媒体，请使用 buffer 代替");
        }
    }

    /**
     * 解析沙箱媒体源
     * HTTP URL 直接放行，文件路径需要验证
     *
     * @param media 媒体路径
     * @param sandboxRoot 沙箱根目录
     * @return 解析后的媒体路径
     */
    public static String resolveSandboxedMediaSource(String media, String sandboxRoot)
            throws SandboxPathEscapeException, IOException {
        String raw = media == null ? "" : media.trim();
        if (raw.isEmpty())
            return raw;
        if (HTTP_URL_RE.matcher(raw).find())
            return raw;

        String candidate = raw;
        if (candidate.toLowerCase().startsWith("file://")) {
            // 将 file:// URL 转换为路径
            try {
                candidate = java.net.URI.create(candidate).getPath();
            } catch (Exception e) {
                throw new IllegalArgumentException("无效的沙箱媒体 file:// URL: " + raw);
            }
        }

        ResolvedPath resolved = assertSandboxPath(candidate, sandboxRoot, sandboxRoot);
        return resolved.resolved();
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────

    /**
     * 标准化 Unicode 空格
     */
    private static String normalizeUnicodeSpaces(String str) {
        return UNICODE_SPACES.matcher(str).replaceAll(" ");
    }

    /**
     * 展开路径（处理 ~ 扩展）
     */
    private static String expandPath(String filePath) {
        String normalized = normalizeUnicodeSpaces(filePath);
        if ("~".equals(normalized))
            return System.getProperty("user.home");
        if (normalized.startsWith("~/")) {
            return System.getProperty("user.home") + normalized.substring(1);
        }
        return normalized;
    }

    /**
     * 解析到当前工作目录
     */
    private static String resolveToCwd(String filePath, String cwd) {
        String expanded = expandPath(filePath);
        Path p = Path.of(expanded);
        if (p.isAbsolute())
            return p.normalize().toString();
        return Path.of(cwd).resolve(expanded).normalize().toString();
    }

    /**
     * 断言没有符号链接
     */
    private static void assertNoSymlink(String relative, String root) throws IOException {
        if (relative == null || relative.isEmpty())
            return;
        String[] parts = relative.split(java.util.regex.Pattern.quote(java.io.File.separator));
        Path current = Path.of(root);
        for (String part : parts) {
            if (part.isEmpty())
                continue;
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(current)) {
                    throw new IOException("沙箱路径中不允许符号链接: " + current);
                }
            } else {
                return; // 路径尚不存在，无需继续检查
            }
        }
    }

    /**
     * 缩短路径（将用户主目录替换为 ~）
     */
    private static String shortPath(String value) {
        String home = System.getProperty("user.home");
        if (value.startsWith(home)) {
            return "~" + value.substring(home.length());
        }
        return value;
    }

    /**
     * 路径逃逸异常
     * 当路径试图逃逸出沙箱根目录时抛出
     */
    public static class SandboxPathEscapeException extends RuntimeException {
        public SandboxPathEscapeException(String message) {
            super(message);
        }
    }
}

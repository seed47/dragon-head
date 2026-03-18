package org.dragon.character.mind.skills;

import org.dragon.character.mind.skills.SkillTypes.Skill;
import org.dragon.character.mind.skills.SkillTypes.SkillSource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 内置技能目录解析和上下文加载。
 * 对应 TypeScript 中的 skills/bundled-dir.ts + bundled-context.ts。
 */
@Slf4j
public final class SkillBundledDir {

    private SkillBundledDir() {
    }

    private static boolean hasWarnedMissing = false;

    // ── 目录解析 (Directory resolution) ────────────────────────────────────────

    /**
     * 解析内置技能目录。
     * 优先级：环境变量覆盖 → 可执行文件的同级目录 → 模块相对搜索。
     */
    public static String resolveBundledSkillsDir() {
        // 1. 环境变量覆盖
        String override = System.getenv("OPENCLAW_BUNDLED_SKILLS_DIR");
        if (override != null && !override.trim().isEmpty()) {
            return override.trim();
        }

        // 2. JAR/可执行文件的同级目录
        try {
            Path jarDir = resolveJarDir();
            if (jarDir != null) {
                Path sibling = jarDir.resolve("skills");
                if (Files.isDirectory(sibling)) {
                    return sibling.toString();
                }
            }
        } catch (Exception e) {
            // 忽略
        }

        // 3. Classpath 资源（用于开发环境）
        try {
            URL resource = SkillBundledDir.class.getClassLoader().getResource("skills");
            if (resource != null) {
                Path resourcePath = Paths.get(resource.toURI());
                if (Files.isDirectory(resourcePath) && looksLikeSkillsDir(resourcePath)) {
                    return resourcePath.toString();
                }
            }
        } catch (Exception e) {
            // 忽略
        }

        // 4. 从工作目录向上查找
        try {
            Path current = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
            for (int depth = 0; depth < 6; depth++) {
                Path candidate = current.resolve("skills");
                if (looksLikeSkillsDir(candidate)) {
                    return candidate.toString();
                }
                Path parent = current.getParent();
                if (parent == null || parent.equals(current))
                    break;
                current = parent;
            }
        } catch (Exception e) {
            // 忽略
        }

        return null;
    }

    // ── 上下文 (Context) ─────────────────────────────────────────────────────

    /**
     * 内置技能上下文：目录路径 + 技能名称集合。
     */
    public static class BundledSkillsContext {
        private final String dir;
        private final Set<String> names;

        public BundledSkillsContext(String dir, Set<String> names) {
            this.dir = dir;
            this.names = names;
        }

        public String getDir() {
            return dir;
        }

        public Set<String> getNames() {
            return names;
        }
    }

    /**
     * 解析内置技能上下文：加载所有内置技能并收集名称。
     */
    public static BundledSkillsContext resolveBundledSkillsContext() {
        String dir = resolveBundledSkillsDir();
        Set<String> names = new LinkedHashSet<>();

        if (dir == null) {
            if (!hasWarnedMissing) {
                hasWarnedMissing = true;
                log.warn("无法解析内置技能目录；可能缺少内置技能。");
            }
            return new BundledSkillsContext(null, names);
        }

        List<Skill> skills = SkillLoader.loadSkillsFromDir(Paths.get(dir), SkillSource.BUNDLED);
        for (Skill skill : skills) {
            if (skill.getName() != null && !skill.getName().trim().isEmpty()) {
                names.add(skill.getName());
            }
        }

        return new BundledSkillsContext(dir, names);
    }

    // ── 辅助方法 (Helpers) ─────────────────────────────────────────────────────

    private static boolean looksLikeSkillsDir(Path dir) {
        if (!Files.isDirectory(dir))
            return false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (name.startsWith("."))
                    continue;
                if (Files.isRegularFile(entry) && name.endsWith(".md"))
                    return true;
                if (Files.isDirectory(entry)) {
                    if (Files.isRegularFile(entry.resolve("SKILL.md")))
                        return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private static Path resolveJarDir() {
        try {
            URL location = SkillBundledDir.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            if (location != null) {
                return Paths.get(location.toURI()).getParent();
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }
}
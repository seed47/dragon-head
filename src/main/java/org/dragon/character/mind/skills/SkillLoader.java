package org.dragon.character.mind.skills;

import org.dragon.character.mind.skills.SkillTypes.Skill;
import org.dragon.character.mind.skills.SkillTypes.SkillEntry;
import org.dragon.character.mind.skills.SkillTypes.SkillInvocationPolicy;
import org.dragon.character.mind.skills.SkillTypes.SkillMetadata;
import org.dragon.character.mind.skills.SkillTypes.SkillSnapshot;
import org.dragon.character.mind.skills.SkillTypes.SkillSource;
import org.dragon.character.mind.skills.SkillTypes.SkillSummary;
import org.dragon.config.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 从工作区、内置和托管目录中发现并加载技能。
 * 对应 TypeScript 中的 skills/workspace.ts。
 */
@Slf4j
public class SkillLoader {

    /** 每个技能目录中必须存在的技能定义文件。 */
    private static final String SKILL_FILE = "SKILL.md";

    /** 默认的工作区技能子目录。 */
    private static final String WORKSPACE_SKILLS_DIR = "skills";

    // =========================================================================
    // 单目录加载 (Single-directory loading)
    // =========================================================================

    /**
     * 从单个目录加载所有技能。
     * 包含 SKILL.md 文件的每个子目录都被视为一个技能。
     *
     * @param dir    要扫描的目录
     * @param source 这些技能的来源
     * @return 已加载的技能列表（绝不为 null）
     */
    public static List<Skill> loadSkillsFromDir(Path dir, SkillSource source) {
        if (dir == null || !Files.isDirectory(dir)) {
            return Collections.emptyList();
        }

        List<Skill> skills = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (!Files.isDirectory(child))
                    continue;

                Path skillFile = child.resolve(SKILL_FILE);
                if (!Files.isRegularFile(skillFile))
                    continue;

                try {
                    String content = new String(Files.readAllBytes(skillFile), StandardCharsets.UTF_8);
                    String name = child.getFileName().toString();
                    Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);
                    String description = frontmatter.getOrDefault("description", "");
                    String body = SkillFrontmatterParser.extractBody(content);

                    skills.add(new Skill(
                            name,
                            description,
                            source,
                            skillFile.toAbsolutePath().toString(),
                            child.toAbsolutePath().toString(),
                            body));
                } catch (IOException e) {
                    log.warn("读取技能文件失败 {}: {}", skillFile, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("扫描技能目录失败 {}: {}", dir, e.getMessage());
        }

        // 按名称排序以保证确定性顺序
        skills.sort(Comparator.comparing(Skill::getName));
        return skills;
    }

    // =========================================================================
    // 多来源加载 (Multi-source loading)
    // =========================================================================

    /**
     * 从所有来源（工作区、内置、托管）加载技能条目。
     *
     * @param workspaceDir     工作区根目录
     * @param bundledSkillsDir 可选的内置技能路径
     * @param managedSkillsDir 可选的托管技能路径
     * @return 聚合的技能条目列表
     */
    public static List<SkillEntry> loadSkillEntries(
            String workspaceDir,
            String bundledSkillsDir,
            String managedSkillsDir) {

        List<SkillEntry> entries = new ArrayList<>();

        // 1. 内置技能
        if (bundledSkillsDir != null) {
            loadAndAppend(entries, Paths.get(bundledSkillsDir), SkillSource.BUNDLED);
        }

        // 2. 托管技能
        if (managedSkillsDir != null) {
            loadAndAppend(entries, Paths.get(managedSkillsDir), SkillSource.MANAGED);
        }

        // 3. 工作区技能
        if (workspaceDir != null) {
            Path wsSkills = Paths.get(workspaceDir, WORKSPACE_SKILLS_DIR);
            loadAndAppend(entries, wsSkills, SkillSource.WORKSPACE);
        }

        return entries;
    }

    /**
     * 便捷重载：仅使用工作区目录加载。
     */
    public static List<SkillEntry> loadSkillEntries(String workspaceDir) {
        return loadSkillEntries(workspaceDir, null, null);
    }

    private static void loadAndAppend(List<SkillEntry> entries, Path dir, SkillSource source) {
        List<Skill> skills = loadSkillsFromDir(dir, source);
        for (Skill skill : skills) {
            String content = readSkillContent(skill.getFilePath());
            Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);
            SkillMetadata metadata = SkillFrontmatterParser.resolveMetadata(frontmatter);
            SkillInvocationPolicy invocation = SkillFrontmatterParser.resolveInvocationPolicy(frontmatter);
            entries.add(new SkillEntry(skill, frontmatter, metadata, invocation));
        }
    }

    // =========================================================================
    // 过滤 (Filtering)
    // =========================================================================

    /**
     * 根据资格过滤技能条目：操作系统检查、依赖要求、配置、always 标志。
     *
     * @param entries     完整的条目列表
     * @param config      可选的配置，用于特定技能的覆盖
     * @param skillFilter 可选的名称过滤器（仅包含这些名称）
     * @return 过滤后的条目
     */
    public static List<SkillEntry> filterSkillEntries(
            List<SkillEntry> entries,
            ConfigProperties config,
            List<String> skillFilter) {

        if (entries == null || entries.isEmpty())
            return Collections.emptyList();

        String osName = System.getProperty("os.name", "").toLowerCase();

        return entries.stream()
                .filter(entry -> {
                    // 名称过滤
                    if (skillFilter != null && !skillFilter.isEmpty()) {
                        if (!skillFilter.contains(entry.getSkill().getName())) {
                            return false;
                        }
                    }

                    // 操作系统过滤
                    SkillMetadata meta = entry.getMetadata();
                    if (meta != null && meta.getOs() != null && !meta.getOs().isEmpty()) {
                        boolean osMatch = meta.getOs().stream().anyMatch(os -> osName.contains(os.toLowerCase()));
                        if (!osMatch) {
                            log.debug("技能 '{}' 被过滤：操作系统不匹配 (需要 {}, 当前 {})",
                                    entry.getSkill().getName(), meta.getOs(), osName);
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 不使用名称过滤器的过滤。
     */
    public static List<SkillEntry> filterSkillEntries(
            List<SkillEntry> entries, ConfigProperties config) {
        return filterSkillEntries(entries, config, null);
    }

    // =========================================================================
    // 提示词构建 (Prompt building)
    // =========================================================================

    /**
     * 构建技能提示词字符串，用于注入到系统提示词中。
     *
     * @param entries 过滤后的技能条目
     * @return 格式化后的技能提示词
     */
    public static String buildSkillsPrompt(List<SkillEntry> entries) {
        if (entries == null || entries.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        for (SkillEntry entry : entries) {
            Skill skill = entry.getSkill();
            String content = skill.getContent();

            if (content == null || content.trim().isEmpty())
                continue;

            sb.append("\n<skill name=\"").append(skill.getName()).append("\">\n");
            sb.append(content.trim());
            sb.append("\n</skill>\n");
        }

        return sb.toString().trim();
    }

    /**
     * 构建用于缓存的技能快照。
     */
    public static SkillSnapshot buildSkillSnapshot(List<SkillEntry> entries) {
        String prompt = buildSkillsPrompt(entries);
        List<SkillSummary> summaries = entries.stream()
                .map(e -> new SkillSummary(
                        e.getSkill().getName(),
                        e.getMetadata() != null ? e.getMetadata().getPrimaryEnv() : null))
                .collect(Collectors.toList());
        List<Skill> resolvedSkills = entries.stream().map(SkillEntry::getSkill).collect(Collectors.toList());
        return new SkillSnapshot(prompt, summaries, resolvedSkills, 1);
    }

    // =========================================================================
    // 一次性流水线 (One-shot pipeline)
    // =========================================================================

    /**
     * 完整流水线：加载 → 过滤 → 构建提示词。
     * TODO 这里需要对一下workspaceDir的具体目录地址，看是否还需要增加类似/.xxx的路径
     * @param workspaceDir 工作区根目录
     * @param config       可选配置
     * @return 准备好注入系统提示词的技能提示词字符串
     */
    public static String resolveSkillsPromptForRun(
            String workspaceDir, ConfigProperties config) {
        List<SkillEntry> entries = loadSkillEntries(workspaceDir);
        List<SkillEntry> filtered = filterSkillEntries(entries, config);
        return buildSkillsPrompt(filtered);
    }

    // =========================================================================
    // 辅助方法 (Helpers)
    // =========================================================================

    private static String readSkillContent(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("读取技能文件失败 {}: {}", filePath, e.getMessage());
            return "";
        }
    }
}
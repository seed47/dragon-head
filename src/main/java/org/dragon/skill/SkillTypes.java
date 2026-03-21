package org.dragon.skill;

import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Agent 技能系统的类型定义。
 * 对应 TypeScript 中的 skills/types.ts。
 */
public final class SkillTypes {

    private SkillTypes() {
    }

    // =========================================================================
    // 技能来源 (Skill source)
    // =========================================================================

    public enum SkillSource {
        BUNDLED("bundled"),
        MANAGED("managed"),
        WORKSPACE("workspace"),
        EXTRA("extra"),
        PLUGIN("plugin");

        private final String label;

        SkillSource(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    // =========================================================================
    // 核心技能定义 (Core Skill)
    // =========================================================================

    /**
     * 已加载的技能定义。
     */
    @Value
    public static class Skill {
        /** 技能名称（通常是目录名） */
        String name;
        /** 简短描述（来自 frontmatter） */
        String description;
        /** 技能加载来源 */
        SkillSource source;
        /** SKILL.md 文件的绝对路径 */
        String filePath;
        /** 包含该技能的目录路径 */
        String baseDir;
        /** SKILL.md 的完整内容（frontmatter 之后的正文） */
        String content;
    }

    // =========================================================================
    // 安装规范 (Install spec)
    // =========================================================================

    /**
     * 技能依赖的安装规范。
     */
    @Value
    public static class SkillInstallSpec {
        /** 安装类型：brew | node | go | uv | download */
        String kind;
        String id;
        String label;
        List<String> bins;
        List<String> os;
        /** brew 公式 */
        String formula;
        /** node 包名 */
        String pkg;
        /** go 模块 */
        String module;
        /** 下载链接 */
        String url;
        /** 压缩包名称 */
        String archive;
        /** 是否解压 */
        Boolean extract;
        Integer stripComponents;
        String targetDir;
    }

    // =========================================================================
    // 元数据 (Metadata)
    // =========================================================================

    /**
     * 解析后的 OpenClaw 专属技能元数据。
     */
    @Value
    public static class SkillMetadata {
        Boolean always;
        String skillKey;
        String primaryEnv;
        String emoji;
        String homepage;
        List<String> os;
        SkillRequires requires;
        List<SkillInstallSpec> install;
    }

    /**
     * 技能的依赖要求。
     */
    @Value
    public static class SkillRequires {
        List<String> bins;
        List<String> anyBins;
        List<String> env;
        List<String> config;
    }

    // =========================================================================
    // 调用策略 (Invocation policy)
    // =========================================================================

    /**
     * 控制技能如何/是否可以被调用。
     */
    @Value
    public static class SkillInvocationPolicy {
        /** 用户是否可以直接调用此技能 */
        boolean userInvocable;
        /** 模型是否应禁止自动调用此技能 */
        boolean disableModelInvocation;

        public static final SkillInvocationPolicy DEFAULT = new SkillInvocationPolicy(true, false);
    }

    // =========================================================================
    // 命令规范 (Command specs)
    // =========================================================================

    /**
     * 技能命令的调度行为。
     */
    @Value
    public static class SkillCommandDispatchSpec {
        /** 调度类型，例如 "tool" */
        String kind;
        String toolName;
        /** 参数模式，例如 "raw" */
        String argMode;
    }

    /**
     * 用户可调用的技能命令规范。
     */
    @Value
    public static class SkillCommandSpec {
        String name;
        String skillName;
        String description;
        SkillCommandDispatchSpec dispatch;
    }

    // =========================================================================
    // 安装偏好 (Install preferences)
    // =========================================================================

    /**
     * 用户对技能安装的偏好设置。
     */
    @Value
    public static class SkillsInstallPreferences {
        boolean preferBrew;
        /** node 包管理器：npm | pnpm | yarn | bun */
        String nodeManager;
    }

    // =========================================================================
    // 技能条目 (Skill entry)
    // =========================================================================

    /**
     * 完整解析后的技能条目，包含 frontmatter、元数据和调用策略。
     */
    @Value
    public static class SkillEntry {
        Skill skill;
        Map<String, String> frontmatter;
        SkillMetadata metadata;
        SkillInvocationPolicy invocation;
    }

    // =========================================================================
    // 资格上下文 (Eligibility context)
    // =========================================================================

    /**
     * 用于在远程平台上评估技能资格的上下文。
     */
    @Value
    public static class SkillEligibilityContext {
        RemoteContext remote;

        @Value
        public static class RemoteContext {
            List<String> platforms;
            Predicate<String> hasBin;
            Function<List<String>, Boolean> hasAnyBin;
            String note;
        }
    }

    // =========================================================================
    // 技能快照 (Skill snapshot)
    // =========================================================================

    /**
     * 解析后的技能快照，用于嵌入到系统提示词中。
     */
    @Value
    public static class SkillSnapshot {
        String prompt;
        List<SkillSummary> skills;
        List<Skill> resolvedSkills;
        Integer version;
    }

    /**
     * 用于快照的最小技能摘要。
     */
    @Value
    public static class SkillSummary {
        String name;
        String primaryEnv;
    }
}

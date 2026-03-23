package org.dragon.workspace.commons;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.character.Character;
import org.dragon.config.PromptManager;
import org.dragon.workspace.built_ins.character.commonsense_writer.CommonSenseWriterCharacterFactory;
import org.dragon.workspace.commons.content.CommonSenseContent;
import org.dragon.workspace.commons.content.CommonSenseContentParser;
import org.dragon.workspace.commons.store.WorkspaceCommonSenseStore;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * CommonSenseService 常识服务
 * 管理 Workspace 下的 CommonSense 和 CommonSenseFolder
 * 提供 Prompt 生成和缓存管理功能
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class CommonSenseService {

    private final WorkspaceCommonSenseStore store;
    private final CommonSenseWriterCharacterFactory commonSenseWriterCharacterFactory;
    private final CharacterCaller characterCaller;
    private final PromptManager promptManager;
    private final CommonSenseContentParser contentParser;

    /**
     * Prompt 缓存
     * key: workspaceId
     */
    private final Map<String, CachedPrompt> promptCache = new ConcurrentHashMap<>();

    public CommonSenseService(WorkspaceCommonSenseStore store,
                              CommonSenseWriterCharacterFactory commonSenseWriterCharacterFactory,
                              CharacterCaller characterCaller,
                              PromptManager promptManager,
                              CommonSenseContentParser contentParser) {
        this.store = store;
        this.commonSenseWriterCharacterFactory = commonSenseWriterCharacterFactory;
        this.characterCaller = characterCaller;
        this.promptManager = promptManager;
        this.contentParser = contentParser;
    }

    // ==================== 文件夹管理 ====================

    /**
     * 创建文件夹
     */
    public CommonSenseFolder createFolder(CommonSenseFolder folder) {
        if (folder.getId() == null || folder.getId().isEmpty()) {
            folder.setId(generateFolderId(folder.getWorkspaceId()));
        }
        folder.setCreatedAt(LocalDateTime.now());
        folder.setUpdatedAt(LocalDateTime.now());
        return store.saveFolder(folder);
    }

    /**
     * 获取文件夹
     */
    public Optional<CommonSenseFolder> getFolder(String id) {
        return store.findFolderById(id);
    }

    /**
     * 获取工作空间的所有文件夹
     */
    public List<CommonSenseFolder> getFolders(String workspaceId) {
        return store.findFoldersByWorkspace(workspaceId);
    }

    /**
     * 获取工作空间的根文件夹
     */
    public List<CommonSenseFolder> getRootFolders(String workspaceId) {
        return store.findRootFolders(workspaceId);
    }

    /**
     * 获取子文件夹
     */
    public List<CommonSenseFolder> getChildFolders(String parentId) {
        return store.findChildFolders(parentId);
    }

    /**
     * 删除文件夹
     */
    public boolean deleteFolder(String id) {
        return store.deleteFolder(id);
    }

    // ==================== 常识管理 ====================

    /**
     * 保存常识
     */
    public CommonSense save(CommonSense commonSense) {
        if (commonSense.getId() == null || commonSense.getId().isEmpty()) {
            commonSense.setId(generateCommonSenseId(commonSense.getWorkspaceId()));
        }
        commonSense.setUpdatedAt(LocalDateTime.now());
        if (commonSense.getCreatedAt() == null) {
            commonSense.setCreatedAt(LocalDateTime.now());
        }

        CommonSense result = store.save(commonSense);

        // 自动失效缓存
        invalidateCache(commonSense.getWorkspaceId());

        return result;
    }

    /**
     * 获取常识
     */
    public Optional<CommonSense> get(String id) {
        return store.findById(id);
    }

    /**
     * 获取工作空间的所有常识
     */
    public List<CommonSense> getByWorkspace(String workspaceId) {
        return store.findByWorkspace(workspaceId);
    }

    /**
     * 获取文件夹下的所有常识
     */
    public List<CommonSense> getByFolder(String folderId) {
        return store.findByFolder(folderId);
    }

    /**
     * 获取工作空间所有启用的常识
     */
    public List<CommonSense> getEnabled(String workspaceId) {
        return store.findEnabled(workspaceId);
    }

    /**
     * 获取工作空间指定类别的常识
     */
    public List<CommonSense> getByCategory(String workspaceId, CommonSense.Category category) {
        return store.findByWorkspaceAndCategory(workspaceId, category);
    }

    /**
     * 删除常识
     */
    public boolean delete(String id) {
        Optional<CommonSense> commonSense = store.findById(id);
        boolean result = store.delete(id);

        // 如果删除成功，失效缓存
        if (result && commonSense.isPresent()) {
            invalidateCache(commonSense.get().getWorkspaceId());
        }

        return result;
    }

    // ==================== 缓存管理 ====================

    /**
     * 获取缓存的 Prompt
     */
    public String getCachedPrompt(String workspaceId) {
        CachedPrompt cached = promptCache.get(workspaceId);
        if (cached != null && cached.getPrompt() != null && !cached.getPrompt().isEmpty()) {
            return cached.getPrompt();
        }
        return null;
    }

    /**
     * 检查是否有缓存
     */
    public boolean hasCache(String workspaceId) {
        CachedPrompt cached = promptCache.get(workspaceId);
        return cached != null && cached.getPrompt() != null && !cached.getPrompt().isEmpty();
    }

    /**
     * 失效缓存
     */
    public void invalidateCache(String workspaceId) {
        promptCache.remove(workspaceId);
    }

    // ==================== Prompt 生成 ====================

    /**
     * 生成工作空间的完整 common sense prompt
     * 优先从缓存获取，缓存为空才生成
     */
    public String generatePrompt(String workspaceId) {
        // 1. 尝试从缓存获取
        String cached = getCachedPrompt(workspaceId);
        if (cached != null) {
            return cached;
        }

        // 2. 生成新的 prompt
        String generated = doGeneratePrompt(workspaceId);

        // 3. 存入缓存
        promptCache.put(workspaceId, new CachedPrompt(
                generated,
                LocalDateTime.now(),
                computeVersion(workspaceId)
        ));

        return generated;
    }

    /**
     * 执行实际的 prompt 生成逻辑
     * 使用 CommonSenseWriter Character 生成 prompt
     */
    String doGeneratePrompt(String workspaceId) {
        List<CommonSense> enabledCommonSenses = getEnabled(workspaceId);

        if (enabledCommonSenses.isEmpty()) {
            return "";
        }

        // 构建输入给 CommonSenseWriter Character
        String input = buildCommonSenseWriterInput(workspaceId, enabledCommonSenses);

        try {
            // 获取 CommonSenseWriter Character
            Character commonSenseWriter = commonSenseWriterCharacterFactory
                    .getOrCreateCommonSenseWriterCharacter(workspaceId);

            // 调用 Character 生成 prompt
            String generatedPrompt = characterCaller.call(commonSenseWriter, input);

            log.info("[CommonSenseService] Generated prompt for workspace: {}", workspaceId);

            return generatedPrompt;
        } catch (Exception e) {
            log.warn("[CommonSenseService] Failed to generate prompt via Character, using fallback: {}", e.getMessage());
            // 如果 Character 调用失败，使用 fallback 逻辑
            return doGeneratePromptFallback(enabledCommonSenses);
        }
    }

    /**
     * 构建给 CommonSenseWriter Character 的输入
     * 模板从 PromptManager 读取，支持按 workspace 配置
     */
    private String buildCommonSenseWriterInput(String workspaceId, List<CommonSense> commonSenses) {
        // 从 PromptManager 读取模板，默认值使用原有硬编码内容
        String template = promptManager.getWorkspacePrompt(
                workspaceId,
                "commonsense_writer.input_template",
                "请根据以下常识信息，生成适合的 prompt。\n\n" +
                        "## 工作空间 ID\n{workspaceId}\n\n" +
                        "## 常识列表\n{commonSenses}\n\n" +
                        "请生成一个结构化的 prompt，包含所有常识信息。"
        );

        // 组装 commonSenses 部分
        StringBuilder csPart = new StringBuilder();
        for (CommonSense cs : commonSenses) {
            csPart.append("- 名称: ").append(cs.getName()).append("\n");
            csPart.append("  类别: ").append(cs.getCategory()).append("\n");
            csPart.append("  严重程度: ").append(cs.getSeverity()).append("\n");
            if (cs.getDescription() != null && !cs.getDescription().isEmpty()) {
                csPart.append("  描述: ").append(cs.getDescription()).append("\n");
            }
            // 优先使用新的 content 字段
            if (cs.getContent() != null && !cs.getContent().isEmpty()) {
                csPart.append("  内容: ").append(cs.getContent()).append("\n");
            } else {
                // 向后兼容旧字段
                if (cs.getRule() != null && !cs.getRule().isEmpty()) {
                    csPart.append("  规则: ").append(cs.getRule()).append("\n");
                }
                if (cs.getPromptTemplate() != null && !cs.getPromptTemplate().isEmpty()) {
                    csPart.append("  模板: ").append(cs.getPromptTemplate()).append("\n");
                }
            }
            csPart.append("\n");
        }

        // 替换模板占位符
        return template
                .replace("{workspaceId}", workspaceId)
                .replace("{commonSenses}", csPart.toString());
    }

    /**
     * Fallback: 当 Character 调用失败时使用
     */
    private String doGeneratePromptFallback(List<CommonSense> commonSenses) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Workspace Common Sense\n\n");
        sb.append("以下是本工作空间的常识规则：\n\n");

        // 按类别分组
        Map<CommonSense.Category, List<CommonSense>> grouped = commonSenses.stream()
                .collect(Collectors.groupingBy(CommonSense::getCategory));

        for (CommonSense.Category category : CommonSense.Category.values()) {
            List<CommonSense> categoryList = grouped.get(category);
            if (categoryList != null && !categoryList.isEmpty()) {
                sb.append("## ").append(getCategoryName(category)).append("\n\n");
                for (CommonSense cs : categoryList) {
                    sb.append(toPromptFragment(cs));
                    sb.append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 转换单条 CommonSense 为 prompt 片段
     */
    public String toPromptFragment(CommonSense commonSense) {
        StringBuilder sb = new StringBuilder();

        // 名称和严重程度
        sb.append("### ").append(commonSense.getName());
        if (commonSense.getSeverity() != null) {
            sb.append(" [").append(commonSense.getSeverity()).append("]");
        }
        sb.append("\n");

        // 描述
        if (commonSense.getDescription() != null && !commonSense.getDescription().isEmpty()) {
            sb.append(commonSense.getDescription()).append("\n");
        }

        // 优先使用新的 content 字段
        String content;
        if (commonSense.getContent() != null && !commonSense.getContent().isEmpty()) {
            CommonSenseContent parsed = contentParser.parse(commonSense.getContent());
            if (parsed != null) {
                content = contentParser.toPromptText(parsed);
            } else {
                content = "";
            }
        } else if (commonSense.getPromptTemplate() != null && !commonSense.getPromptTemplate().isEmpty()) {
            // 向后兼容旧字段
            content = resolveTemplate(commonSense.getPromptTemplate(), commonSense.getPromptVariables());
        } else if (commonSense.getRule() != null && !commonSense.getRule().isEmpty()) {
            content = commonSense.getRule();
        } else {
            content = "";
        }

        if (!content.isEmpty()) {
            sb.append("规则: ").append(content);
        }

        return sb.toString();
    }

    // ==================== 辅助方法 ====================

    private String generateFolderId(String workspaceId) {
        return "csf_" + workspaceId + "_" + System.currentTimeMillis();
    }

    private String generateCommonSenseId(String workspaceId) {
        return "cs_" + workspaceId + "_" + System.currentTimeMillis();
    }

    private String getCategoryName(CommonSense.Category category) {
        switch (category) {
            case PRIVACY: return "隐私保护";
            case SAFETY: return "安全合规";
            case PERFORMANCE: return "性能约束";
            case BUSINESS: return "业务规则";
            case ETHICS: return "伦理道德";
            case SYSTEM: return "系统约束";
            default: return category.name();
        }
    }

    private String resolveTemplate(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private String computeVersion(String workspaceId) {
        List<CommonSense> commonSenses = store.findEnabled(workspaceId);
        return commonSenses.stream()
                .map(CommonSense::getVersion)
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
    }

    // ==================== 缓存对象 ====================

    /**
     * 缓存的 Prompt 对象
     */
    public static class CachedPrompt {
        private final String prompt;
        private final LocalDateTime cachedAt;
        private final String commonSenseVersion;

        public CachedPrompt(String prompt, LocalDateTime cachedAt, String commonSenseVersion) {
            this.prompt = prompt;
            this.cachedAt = cachedAt;
            this.commonSenseVersion = commonSenseVersion;
        }

        public String getPrompt() {
            return prompt;
        }

        public LocalDateTime getCachedAt() {
            return cachedAt;
        }

        public String getCommonSenseVersion() {
            return commonSenseVersion;
        }
    }
}

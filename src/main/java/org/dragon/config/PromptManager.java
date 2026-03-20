package org.dragon.config;

import org.dragon.config.store.ConfigKey;
import org.dragon.config.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Prompt 管理器
 * 基于 ConfigStore 实现 workspace-Organization-Character 层级的 prompt 管理
 *
 * <p>Key 编码格式:
 * <ul>
 *   <li>Global: "prompt/{promptKey}"</li>
 *   <li>Workspace: "workspace:{ws}/prompt/{promptKey}"</li>
 *   <li>Organization: "workspace:{ws}/org:{org}/prompt/{promptKey}"</li>
 *   <li>CharacterInOrg: "workspace:{ws}/org:{org}/char:{char}/prompt/{promptKey}"</li>
 * </ul>
 *
 * <p>查找优先级:
 * <ol>
 *   <li>Character在Organization下 (最高优先级)</li>
 *   <li>Organization级别</li>
 *   <li>Workspace级别</li>
 *   <li>Global级别 (默认)</li>
 * </ol>
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class PromptManager {

    private static final Logger log = LoggerFactory.getLogger(PromptManager.class);

    private static final String PROMPT_PREFIX = "prompt/";

    private final ConfigStore configStore;

    public PromptManager(ConfigStore configStore) {
        this.configStore = configStore;
    }

    /**
     * 获取 prompt - 支持 workspace-Organization-Character 层级查找
     *
     * @param workspace      工作空间ID (可为空)
     * @param organizationId 组织ID (可为空)
     * @param characterId    角色ID (可为空)
     * @param promptKey      prompt键 (如 "observer.suggestion")
     * @return 找到的prompt或null
     */
    public String getPrompt(String workspace, String organizationId, String characterId, String promptKey) {
        // 1. 优先查找 Character在Organization下的 prompt
        if (characterId != null && organizationId != null) {
            String key = buildKey(workspace, organizationId, characterId, promptKey);
            Optional<Object> value = configStore.get(ConfigKey.of(key));
            if (value.isPresent()) {
                log.debug("[PromptManager] Found prompt at CharacterInOrg level: {}", key);
                return value.get().toString();
            }
        }

        // 2. 查找 Organization 级别的 prompt
        if (organizationId != null) {
            String key = buildKey(workspace, organizationId, null, promptKey);
            Optional<Object> value = configStore.get(ConfigKey.of(key));
            if (value.isPresent()) {
                log.debug("[PromptManager] Found prompt at Organization level: {}", key);
                return value.get().toString();
            }
        }

        // 3. 查找 Workspace 级别的 prompt
        if (workspace != null) {
            String key = buildKey(workspace, null, null, promptKey);
            Optional<Object> value = configStore.get(ConfigKey.of(key));
            if (value.isPresent()) {
                log.debug("[PromptManager] Found prompt at Workspace level: {}", key);
                return value.get().toString();
            }
        }

        // 4. 查找 Global 级别的 prompt
        String globalKey = PROMPT_PREFIX + promptKey;
        Optional<Object> value = configStore.get(ConfigKey.of(globalKey));
        if (value.isPresent()) {
            log.debug("[PromptManager] Found prompt at Global level: {}", globalKey);
            return value.get().toString();
        }

        log.debug("[PromptManager] No prompt found for key: {}", promptKey);
        return null;
    }

    /**
     * 获取全局默认 prompt
     *
     * @param promptKey     prompt键
     * @param defaultValue  默认值
     * @return prompt或默认值
     */
    public String getGlobalPrompt(String promptKey, String defaultValue) {
        String value = getPrompt(null, null, null, promptKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 设置 Character在Organization下的 prompt (最高优先级)
     */
    public void setCharacterInOrgPrompt(String workspace, String organizationId, String characterId,
                                         String promptKey, String content) {
        String key = buildKey(workspace, organizationId, characterId, promptKey);
        configStore.set(ConfigKey.of(key), content);
        log.info("[PromptManager] Set CharacterInOrg prompt: workspace={}, org={}, char={}, key={}",
                workspace, organizationId, characterId, promptKey);
    }

    /**
     * 设置 Organization 级别的 prompt
     */
    public void setOrganizationPrompt(String workspace, String organizationId, String promptKey, String content) {
        String key = buildKey(workspace, organizationId, null, promptKey);
        configStore.set(ConfigKey.of(key), content);
        log.info("[PromptManager] Set Organization prompt: workspace={}, org={}, key={}",
                workspace, organizationId, promptKey);
    }

    /**
     * 设置 Workspace 级别的 prompt
     */
    public void setWorkspacePrompt(String workspace, String promptKey, String content) {
        String key = buildKey(workspace, null, null, promptKey);
        configStore.set(ConfigKey.of(key), content);
        log.info("[PromptManager] Set Workspace prompt: workspace={}, key={}", workspace, promptKey);
    }

    /**
     * 设置全局默认 prompt
     */
    public void setGlobalPrompt(String promptKey, String content) {
        String key = PROMPT_PREFIX + promptKey;
        configStore.set(ConfigKey.of(key), content);
        log.info("[PromptManager] Set Global prompt: key={}", promptKey);
    }

    /**
     * 删除 Character在Organization下的 prompt
     */
    public void deleteCharacterInOrgPrompt(String workspace, String organizationId, String characterId, String promptKey) {
        String key = buildKey(workspace, organizationId, characterId, promptKey);
        configStore.delete(ConfigKey.of(key));
    }

    /**
     * 删除 Organization 级别的 prompt
     */
    public void deleteOrganizationPrompt(String workspace, String organizationId, String promptKey) {
        String key = buildKey(workspace, organizationId, null, promptKey);
        configStore.delete(ConfigKey.of(key));
    }

    /**
     * 删除 Workspace 级别的 prompt
     */
    public void deleteWorkspacePrompt(String workspace, String promptKey) {
        String key = buildKey(workspace, null, null, promptKey);
        configStore.delete(ConfigKey.of(key));
    }

    /**
     * 删除全局 prompt
     */
    public void deleteGlobalPrompt(String promptKey) {
        String key = PROMPT_PREFIX + promptKey;
        configStore.delete(ConfigKey.of(key));
    }

    /**
     * 获取所有 Workspace 级别的 prompts
     */
    public Map<String, Object> getAllWorkspacePrompts(String workspace) {
        String prefix = "workspace:" + workspace + "/prompt/";
        return configStore.getAll(ConfigKey.of(prefix));
    }

    /**
     * 获取所有 Organization 级别的 prompts
     */
    public Map<String, Object> getAllOrganizationPrompts(String workspace, String organizationId) {
        String prefix = "workspace:" + workspace + "/org:" + organizationId + "/prompt/";
        return configStore.getAll(ConfigKey.of(prefix));
    }

    /**
     * 构建编码后的 key
     */
    private String buildKey(String workspace, String organizationId, String characterId, String promptKey) {
        StringBuilder sb = new StringBuilder();

        if (workspace != null) {
            sb.append("workspace:").append(workspace).append("/");
        }
        if (organizationId != null) {
            sb.append("org:").append(organizationId).append("/");
        }
        if (characterId != null) {
            sb.append("char:").append(characterId).append("/");
        }
        sb.append(PROMPT_PREFIX).append(promptKey);

        return sb.toString();
    }
}

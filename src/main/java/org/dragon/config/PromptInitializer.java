package org.dragon.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Prompt 初始化器
 * 系统启动时将默认 prompt 加载到 ConfigStore
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class PromptInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PromptInitializer.class);

    private final PromptManager promptManager;

    public PromptInitializer(PromptManager promptManager) {
        this.promptManager = promptManager;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("[PromptInitializer] Starting prompt initialization...");

        // 初始化 Observer 相关 prompts
        initObserverPrompts();

        // 初始化 ReAct 相关 prompts
        initReActPrompts();

        // 初始化 Character 相关 prompts
        initCharacterPrompts();

        // 初始化 HR 相关 prompts
        initHrPrompts();

        // 初始化 MemberSelector 相关 prompts
        initMemberSelectorPrompts();

        // 初始化 ProjectManager 相关 prompts
        initProjectManagerPrompts();

        log.info("[PromptInitializer] Prompt initialization completed");
    }

    private void initObserverPrompts() {
        // Observer Suggestion Prompt
        String observerSuggestion = loadPromptFromFile("prompts/observer-suggestion-prompt.txt");
        if (observerSuggestion != null) {
            promptManager.setGlobalPrompt(PromptKeys.OBSERVER_SUGGESTION, observerSuggestion);
        } else {
            // 使用内联默认
            promptManager.setGlobalPrompt(PromptKeys.OBSERVER_SUGGESTION,
                    "你是一个专业的AI优化顾问，擅长分析AI角色的行为模式并提供改进建议。请根据提供的数据生成具体、可执行的优化建议。");
        }

        // Observer Personality Enhancement Prompt
        String personalityEnhancement = loadPromptFromFile("prompts/observer-personality-enhancement-prompt.txt");
        if (personalityEnhancement != null) {
            promptManager.setGlobalPrompt(PromptKeys.OBSERVER_PERSONALITY_ENHANCEMENT, personalityEnhancement);
        }
    }

    private void initReActPrompts() {
        // ReAct Task Decompose Prompt
        promptManager.setGlobalPrompt(PromptKeys.REACT_TASK_DECOMPOSE,
                "你是一个组织调度专家，负责把复杂任务拆解为可执行的子任务。");

        // ReAct Execute Prompt
        promptManager.setGlobalPrompt(PromptKeys.REACT_EXECUTE,
                "你是一个专业的AI助手，负责根据用户输入执行任务。");
    }

    private void initCharacterPrompts() {
        // Character Default System Prompt
        promptManager.setGlobalPrompt(PromptKeys.CHARACTER_SYSTEM,
                "你是一个专业的AI数字员工，有自己的性格特点和价值观。");

        // Character Task Prompt
        promptManager.setGlobalPrompt(PromptKeys.CHARACTER_TASK,
                "请根据要求完成以下任务：");
    }

    private void initHrPrompts() {
        // HR 雇佣决策 Prompt
        promptManager.setGlobalPrompt(PromptKeys.HR_HIRE_DECISION,
                """
                请评估是否应该雇佣以下 Character 到工作空间：

                Character 名称: %s
                Character 描述: %s

                请返回以下格式的决策：
                - APPROVE：批准雇佣
                - DENY：拒绝雇佣
                - 需要更多信息

                如果批准，请简要说明理由。
                """);

        // HR 雇佣选择 Prompt
        promptManager.setGlobalPrompt(PromptKeys.HR_HIRE_SELECT,
                "请从以下候选 Character 中选择一个最合适雇佣的：");

        // HR 解雇决策 Prompt
        promptManager.setGlobalPrompt(PromptKeys.HR_FIRE_DECISION,
                """
                请评估是否应该解雇工作空间中的以下 Character：

                Character ID: %s

                请返回以下格式的决策：
                - APPROVE：批准解雇
                - DENY：拒绝解雇
                - 需要更多信息

                如果批准，请简要说明理由。
                """);

        // HR 生成职责描述 Prompt
        promptManager.setGlobalPrompt(PromptKeys.HR_DUTY_GENERATE,
                """
                请为以下 Character 生成一个合适的职责描述：

                Character 名称: %s
                Character 描述: %s

                请用 1-2 句话简洁描述该 Character 在工作空间中的职责。
                """);

        // 通用选择 Prompt
        promptManager.setGlobalPrompt(PromptKeys.SELECTION_GENERIC,
                "请从以下候选中选择一个最合适的：");
    }

    private void initMemberSelectorPrompts() {
        // MemberSelector 选择成员 Prompt
        String memberSelectorSelect = loadPromptFromFile("prompts/member-selector-select-prompt.txt");
        if (memberSelectorSelect != null) {
            promptManager.setGlobalPrompt(PromptKeys.MEMBER_SELECTOR_SELECT, memberSelectorSelect);
        } else {
            promptManager.setGlobalPrompt(PromptKeys.MEMBER_SELECTOR_SELECT,
                    "请从以下候选成员中选择最合适的执行者来完成指定任务。考虑技能匹配度、历史成功率和工作负载。");
        }
    }

    private void initProjectManagerPrompts() {
        // ProjectManager 任务拆解 Prompt
        String projectManagerDecompose = loadPromptFromFile("prompts/project-manager-decompose-prompt.txt");
        if (projectManagerDecompose != null) {
            promptManager.setGlobalPrompt(PromptKeys.PROJECT_MANAGER_DECOMPOSE, projectManagerDecompose);
        } else {
            promptManager.setGlobalPrompt(PromptKeys.PROJECT_MANAGER_DECOMPOSE,
                    "请将以下任务拆解为可执行的子任务。");
        }
    }

    /**
     * 从 classpath 加载 prompt 文件
     */
    private String loadPromptFromFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("[PromptInitializer] Failed to load prompt from: {}", path, e);
        }
        return null;
    }
}

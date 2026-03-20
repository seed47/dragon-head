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

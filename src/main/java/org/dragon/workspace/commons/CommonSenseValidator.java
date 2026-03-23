package org.dragon.workspace.commons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dragon.workspace.commons.store.WorkspaceCommonSenseStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * CommonSense 常识校验器
 * 用于校验优化动作是否符合常识库约束
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class CommonSenseValidator {

    private static final Logger log = LoggerFactory.getLogger(CommonSenseValidator.class);

    private static final String GLOBAL_WORKSPACE_ID = "_global";

    private final WorkspaceCommonSenseStore commonSenseStore;

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<Violation> violations;

        public ValidationResult(boolean valid, List<Violation> violations) {
            this.valid = valid;
            this.violations = violations;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }

        public static ValidationResult failure(List<Violation> violations) {
            return new ValidationResult(false, violations);
        }

        public boolean isValid() {
            return valid;
        }

        public List<Violation> getViolations() {
            return violations;
        }
    }

    /**
     * 违规信息
     */
    public static class Violation {
        private final String commonSenseId;
        private final String commonSenseName;
        private final String message;
        private final CommonSense.Severity severity;

        public Violation(String commonSenseId, String commonSenseName, String message, CommonSense.Severity severity) {
            this.commonSenseId = commonSenseId;
            this.commonSenseName = commonSenseName;
            this.message = message;
            this.severity = severity;
        }

        public String getCommonSenseId() {
            return commonSenseId;
        }

        public String getCommonSenseName() {
            return commonSenseName;
        }

        public String getMessage() {
            return message;
        }

        public CommonSense.Severity getSeverity() {
            return severity;
        }
    }

    /**
     * 校验优化动作
     *
     * @param actionTargetType 目标类型 (CHARACTER 或 WORKSPACE)
     * @param actionType       动作类型
     * @param parameters       修改参数
     * @return 校验结果
     */
    public ValidationResult validate(String actionTargetType, String actionType, Map<String, Object> parameters) {
        List<Violation> violations = new ArrayList<>();

        // 获取所有启用的常识
        List<CommonSense> enabledCommonSense = getEnabledCommonSense();

        for (CommonSense cs : enabledCommonSense) {
            Violation violation = checkViolation(cs, parameters);
            if (violation != null) {
                violations.add(violation);
                // 关键级别违反直接返回
                if (cs.isCritical()) {
                    log.warn("[CommonSenseValidator] Critical violation detected: {}", cs.getName());
                    return ValidationResult.failure(violations);
                }
            }
        }

        if (violations.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(violations);
    }

    /**
     * 获取全局工作空间的启用的常识列表
     */
    private List<CommonSense> getEnabledCommonSense() {
        return commonSenseStore.findEnabled(GLOBAL_WORKSPACE_ID);
    }

    /**
     * 批量校验
     *
     * @param actions 动作列表
     * @return 校验结果
     */
    public ValidationResult validateBatch(List<Triplet> actions) {
        List<Violation> allViolations = new ArrayList<>();

        for (Triplet action : actions) {
            ValidationResult result = validate(action.targetType, action.actionType, action.parameters);
            if (!result.isValid()) {
                allViolations.addAll(result.getViolations());
            }
        }

        if (allViolations.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(allViolations);
    }

    /**
     * 检查单个常识是否违反
     */
    private Violation checkViolation(CommonSense cs, Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }

        String rule = cs.getRule();
        if (rule == null || rule.isEmpty()) {
            return null;
        }

        // 根据类别进行不同检查
        switch (cs.getCategory()) {
            case PRIVACY:
                return checkPrivacyViolation(cs, parameters);
            case PERFORMANCE:
                return checkPerformanceViolation(cs, rule, parameters);
            case BUSINESS:
                return checkBusinessViolation(cs, parameters);
            case SYSTEM:
                return checkSystemViolation(cs, rule, parameters);
            default:
                return null;
        }
    }

    /**
     * 检查隐私违规
     */
    private Violation checkPrivacyViolation(CommonSense cs, Map<String, Object> parameters) {
        // 检查是否包含敏感字段
        String paramStr = parameters.toString().toLowerCase();
        if (paramStr.contains("password") || paramStr.contains("token")
                || paramStr.contains("secret") || paramStr.contains("api_key")) {
            return new Violation(cs.getId(), cs.getName(),
                    "参数中包含敏感信息", cs.getSeverity());
        }
        return null;
    }

    /**
     * 检查性能违规
     */
    private Violation checkPerformanceViolation(CommonSense cs, String rule, Map<String, Object> parameters) {
        // 解析 maxResponseTime: 30000 格式
        if (rule.contains("maxResponseTime")) {
            int maxValue = extractIntValue(rule, "maxResponseTime");
            Object actualValue = parameters.get("responseTime");
            if (actualValue != null) {
                int actual = Integer.parseInt(actualValue.toString());
                if (actual > maxValue) {
                    return new Violation(cs.getId(), cs.getName(),
                            String.format("响应时间 %dms 超过限制 %dms", actual, maxValue),
                            cs.getSeverity());
                }
            }
        }
        return null;
    }

    /**
     * 检查业务规则违规
     */
    private Violation checkBusinessViolation(CommonSense cs, Map<String, Object> parameters) {
        // 检查持久化要求
        String paramStr = parameters.toString().toLowerCase();
        if (paramStr.contains("persist")) {
            Object shouldPersist = parameters.get("shouldPersist");
            if (shouldPersist != null && "false".equals(shouldPersist.toString().toLowerCase())) {
                return new Violation(cs.getId(), cs.getName(),
                        "核心数据必须持久化存储", cs.getSeverity());
            }
        }
        return null;
    }

    /**
     * 检查系统违规
     */
    private Violation checkSystemViolation(CommonSense cs, String rule, Map<String, Object> parameters) {
        // 检查 token 限制
        if (rule.contains("maxTokens")) {
            int maxValue = extractIntValue(rule, "maxTokens");
            Object tokens = parameters.get("tokens");
            if (tokens != null) {
                int actualTokens = Integer.parseInt(tokens.toString());
                if (actualTokens > maxValue) {
                    return new Violation(cs.getId(), cs.getName(),
                            String.format("Token 消耗 %d 超过限制 %d", actualTokens, maxValue),
                            cs.getSeverity());
                }
            }
        }
        return null;
    }

    /**
     * 从规则字符串中提取整数值
     */
    private int extractIntValue(String rule, String key) {
        try {
            String pattern = key + ":";
            int index = rule.indexOf(pattern);
            if (index >= 0) {
                int start = index + pattern.length();
                int end = start;
                while (end < rule.length() && Character.isDigit(rule.charAt(end))) {
                    end++;
                }
                if (end > start) {
                    return Integer.parseInt(rule.substring(start, end));
                }
            }
        } catch (Exception e) {
            log.warn("[CommonSenseValidator] Failed to extract value for key: {}", key);
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 参数三元组
     */
    public static class Triplet {
        public final String targetType;
        public final String actionType;
        public final Map<String, Object> parameters;

        public Triplet(String targetType, String actionType, Map<String, Object> parameters) {
            this.targetType = targetType;
            this.actionType = actionType;
            this.parameters = parameters;
        }
    }
}

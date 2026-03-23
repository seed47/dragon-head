package org.dragon.workspace.commons.content;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.extern.slf4j.Slf4j;

/**
 * CommonSense 内容解析器
 * 处理 CommonSense content JSON 的解析、序列化和验证
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class CommonSenseContentParser {

    private final Gson gson;

    public CommonSenseContentParser() {
        this.gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .disableHtmlEscaping()
                .create();
    }

    /**
     * 解析 JSON 字符串为 CommonSenseContent
     *
     * @param jsonContent JSON 字符串
     * @return CommonSenseContent 对象，解析失败返回 null
     */
    public CommonSenseContent parse(String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(jsonContent, CommonSenseContent.class);
        } catch (Exception e) {
            log.warn("[CommonSenseContentParser] Failed to parse content: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 序列化 CommonSenseContent 为 JSON 字符串
     *
     * @param content CommonSenseContent 对象
     * @return JSON 字符串
     */
    public String serialize(CommonSenseContent content) {
        if (content == null) {
            return null;
        }
        return gson.toJson(content);
    }

    /**
     * 验证 JSON 内容结构
     *
     * @param jsonContent JSON 字符串
     * @return 是否有效
     */
    public boolean validate(String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            return false;
        }
        try {
            JsonObject obj = JsonParser.parseString(jsonContent).getAsJsonObject();
            if (!obj.has("type")) {
                return false;
            }
            String type = obj.get("type").getAsString();
            try {
                ContentType.valueOf(type);
            } catch (IllegalArgumentException e) {
                log.warn("[CommonSenseContentParser] Unknown content type: {}", type);
                return false;
            }
            if (!obj.has("data")) {
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("[CommonSenseContentParser] Validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 提取用于 prompt 构建的纯文本描述
     *
     * @param content CommonSenseContent 对象
     * @return 用于 prompt 的文本描述
     */
    public String toPromptText(CommonSenseContent content) {
        if (content == null) {
            return "";
        }

        switch (content.getType()) {
            case SIMPLE:
                return extractSimpleText(content.getData());
            case CONSTRAINT:
                return extractConstraintText(content.getData());
            case FORBIDDEN:
                return extractForbiddenText(content.getData());
            case CONDITIONAL:
                return extractConditionalText(content.getData());
            case TEMPLATE:
                return extractTemplateText(content.getData());
            default:
                return "";
        }
    }

    /**
     * 根据 JSON 数据检测内容类型
     *
     * @param jsonData JSON 数据字符串
     * @return 推测的内容类型
     */
    public ContentType detectContentType(String jsonData) {
        if (jsonData == null || jsonData.isEmpty()) {
            return ContentType.SIMPLE;
        }
        try {
            JsonObject obj = JsonParser.parseString(jsonData).getAsJsonObject();
            if (obj.has("forbidden") || obj.has("items")) {
                return ContentType.FORBIDDEN;
            }
            if (obj.has("constraints")) {
                return ContentType.CONSTRAINT;
            }
            if (obj.has("when") || obj.has("condition")) {
                return ContentType.CONDITIONAL;
            }
            if (obj.has("template")) {
                return ContentType.TEMPLATE;
            }
            return ContentType.SIMPLE;
        } catch (Exception e) {
            return ContentType.SIMPLE;
        }
    }

    private String extractSimpleText(Object data) {
        if (data == null) {
            return "";
        }
        try {
            String json = gson.toJson(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) gson.fromJson(json, Map.class);
            if (map == null || map.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            map.forEach((k, v) -> {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(k).append(": ").append(v);
            });
            return sb.toString();
        } catch (Exception e) {
            log.warn("[CommonSenseContentParser] Failed to extract simple text: {}", e.getMessage());
            return "";
        }
    }

    private String extractConstraintText(Object data) {
        if (data == null) {
            return "";
        }
        try {
            String json = gson.toJson(data);
            ConstraintContent cc = gson.fromJson(json, ConstraintContent.class);
            if (cc == null || cc.getConstraints() == null || cc.getConstraints().isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (ConstraintContent.Constraint c : cc.getConstraints()) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                String unit = c.getUnit() != null ? " " + c.getUnit() : "";
                sb.append(c.getKey()).append(": ").append(c.getValue()).append(unit);
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[CommonSenseContentParser] Failed to extract constraint text: {}", e.getMessage());
            return "";
        }
    }

    private String extractForbiddenText(Object data) {
        if (data == null) {
            return "";
        }
        try {
            String json = gson.toJson(data);
            ForbiddenContent fc = gson.fromJson(json, ForbiddenContent.class);
            if (fc == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder("禁止: ");
            if (fc.getItems() != null && !fc.getItems().isEmpty()) {
                sb.append(String.join(", ", fc.getItems()));
            }
            if (fc.getReason() != null && !fc.getReason().isEmpty()) {
                sb.append(" (原因: ").append(fc.getReason()).append(")");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[CommonSenseContentParser] Failed to extract forbidden text: {}", e.getMessage());
            return "";
        }
    }

    private String extractConditionalText(Object data) {
        if (data == null) {
            return "";
        }
        try {
            String json = gson.toJson(data);
            ConditionalContent cc = gson.fromJson(json, ConditionalContent.class);
            if (cc == null || cc.getWhen() == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            ConditionalContent.Condition condition = cc.getWhen();
            sb.append("当 ").append(condition.getField());

            if (condition.getEquals() != null) {
                sb.append(" ").append(condition.getEquals());
            }
            if (condition.getValue() != null) {
                sb.append(" ").append(condition.getValue());
            }
            sb.append(" 时");
            return sb.toString();
        } catch (Exception e) {
            log.warn("[CommonSenseContentParser] Failed to extract conditional text: {}", e.getMessage());
            return "";
        }
    }

    private String extractTemplateText(Object data) {
        if (data == null) {
            return "";
        }
        try {
            String json = gson.toJson(data);
            TemplateContent tc = gson.fromJson(json, TemplateContent.class);
            if (tc == null) {
                return "";
            }
            return resolveTemplate(tc.getTemplate(), tc.getVariables());
        } catch (Exception e) {
            log.warn("[CommonSenseContentParser] Failed to extract template text: {}", e.getMessage());
            return "";
        }
    }

    private String resolveTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }
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

    /**
     * 解析规则 JSON 并转换为 CommonSenseContent
     *
     * @param ruleJson 规则 JSON 字符串（旧格式）
     * @return CommonSenseContent 对象
     */
    public CommonSenseContent parseRuleToContent(String ruleJson) {
        if (ruleJson == null || ruleJson.isEmpty()) {
            return null;
        }
        try {
            ContentType type = detectContentType(ruleJson);
            JsonObject dataObj = JsonParser.parseString(ruleJson).getAsJsonObject();

            Object data = switch (type) {
                case SIMPLE -> gson.fromJson(dataObj, SimpleContent.class);
                case CONSTRAINT -> gson.fromJson(dataObj, ConstraintContent.class);
                case FORBIDDEN -> gson.fromJson(dataObj, ForbiddenContent.class);
                case CONDITIONAL -> gson.fromJson(dataObj, ConditionalContent.class);
                case TEMPLATE -> gson.fromJson(dataObj, TemplateContent.class);
            };

            return CommonSenseContent.builder()
                    .type(type)
                    .data(data)
                    .build();
        } catch (Exception e) {
            log.warn("[CommonSenseContentParser] Failed to parse rule to content: {}", e.getMessage());
            return null;
        }
    }
}

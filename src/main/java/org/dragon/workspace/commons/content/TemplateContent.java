package org.dragon.workspace.commons.content;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模板内容
 * 用于存储带变量的模板
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateContent {

    /**
     * 模板字符串，支持 ${变量名} 占位符
     */
    private String template;

    /**
     * 模板变量
     */
    private Map<String, Object> variables;
}
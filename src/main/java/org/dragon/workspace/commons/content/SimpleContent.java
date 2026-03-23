package org.dragon.workspace.commons.content;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单键值对内容
 * 用于存储简单的配置或限制
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleContent {

    /**
     * 键值对映射
     */
    private Map<String, Object> value;
}
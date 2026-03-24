package org.dragon.workspace.material;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * MaterialParser 物料解析器接口
 * 负责解析上传的物料（文档、图片等），提取可用的文本内容
 *
 * @author wyj
 * @version 1.0
 */
public interface MaterialParser {

    /**
     * 解析物料
     *
     * @param material 物料
     * @param inputStream 物料输入流
     * @return 解析结果
     */
    ParseResult parse(Material material, InputStream inputStream);

    /**
     * 批量解析物料
     *
     * @param materials 物料列表
     * @return 解析结果映射（materialId -> ParseResult）
     */
    Map<String, ParseResult> parseAll(List<Material> materials);

    /**
     * 支持的物料类型
     *
     * @return MIME 类型列表
     */
    List<String> supportedTypes();

    /**
     * 解析结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class ParseResult {
        /**
         * 物料 ID
         */
        private String materialId;
        /**
         * 是否成功
         */
        private boolean success;
        /**
         * 提取的文本内容
         */
        private String textContent;
        /**
         * 结构化数据（如果是 JSON/XML 等）
         */
        private Object structuredContent;
        /**
         * 错误信息
         */
        private String errorMessage;
        /**
         * 元数据
         */
        private Map<String, Object> metadata;
    }
}
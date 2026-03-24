package org.dragon.workspace.material;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认物料解析器实现
 * 支持文本文件的解析
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class DefaultMaterialParser implements MaterialParser {

    private static final List<String> SUPPORTED_TYPES = List.of(
            "text/plain",
            "text/html",
            "text/markdown",
            "application/json",
            "application/xml",
            "text/csv"
    );

    @Override
    public ParseResult parse(Material material, InputStream inputStream) {
        if (material == null || inputStream == null) {
            return ParseResult.builder()
                    .success(false)
                    .errorMessage("Material or input stream is null")
                    .build();
        }

        String contentType = material.getType();
        if (contentType == null || !supportedTypes().contains(contentType)) {
            return ParseResult.builder()
                    .materialId(material.getId())
                    .success(false)
                    .errorMessage("Unsupported content type: " + contentType)
                    .build();
        }

        try {
            String textContent = readTextContent(inputStream);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("size", material.getSize());
            metadata.put("name", material.getName());
            metadata.put("contentType", contentType);
            metadata.put("parsedLength", textContent.length());

            log.info("[DefaultMaterialParser] Successfully parsed material {} ({} bytes)",
                    material.getId(), material.getSize());

            return ParseResult.builder()
                    .materialId(material.getId())
                    .success(true)
                    .textContent(textContent)
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("[DefaultMaterialParser] Failed to parse material {}: {}",
                    material.getId(), e.getMessage(), e);
            return ParseResult.builder()
                    .materialId(material.getId())
                    .success(false)
                    .errorMessage("Parse error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public Map<String, ParseResult> parseAll(List<Material> materials) {
        Map<String, ParseResult> results = new HashMap<>();

        for (Material material : materials) {
            try {
                // 获取输入流
                InputStream inputStream = null; // TODO: 从 MaterialStorage 获取
                if (inputStream != null) {
                    ParseResult result = parse(material, inputStream);
                    results.put(material.getId(), result);
                }
            } catch (Exception e) {
                log.error("[DefaultMaterialParser] Error parsing material {}: {}",
                        material.getId(), e.getMessage());
                results.put(material.getId(), ParseResult.builder()
                        .materialId(material.getId())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return results;
    }

    @Override
    public List<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    /**
     * 读取文本内容
     */
    private String readTextContent(InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
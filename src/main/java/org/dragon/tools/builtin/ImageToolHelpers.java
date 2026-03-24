package org.dragon.tools.builtin;

import org.dragon.config.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图像工具辅助类：数据 URL 解码、模型配置、视觉模型解析。
 * 对应 TypeScript agents/tools/image-tool.helpers.ts。
 */
@Slf4j
public final class ImageToolHelpers {

    private static final Pattern DATA_URL_RE = Pattern.compile("^data:([^;,]+);base64,([a-zA-Z0-9+/=\\r\\n]+)$");

    private ImageToolHelpers() {
    }

    /**
     * 图像模型配置。
     */
    public record ImageModelConfig(String primary, List<String> fallbacks) {
    }

    /**
     * 解码后的数据 URL 结果。
     */
    public record DecodedDataUrl(byte[] data, String mimeType) {
    }

    /**
     * 解码 base64 数据 URL。
     *
     * @param dataUrl 数据 URL 字符串
     * @return 解码后的数据和 mime 类型
     * @throws IllegalArgumentException 如果 URL 无效或不是图像
     */
    public static DecodedDataUrl decodeDataUrl(String dataUrl) {
        String trimmed = dataUrl.trim();
        Matcher m = DATA_URL_RE.matcher(trimmed);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid data URL (expected base64 data: URL).");
        }
        String mimeType = m.group(1).trim().toLowerCase();
        if (!mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("Unsupported data URL type: " + mimeType);
        }
        String b64 = m.group(2).trim();
        byte[] data = Base64.getDecoder().decode(b64);
        if (data.length == 0) {
            throw new IllegalArgumentException("Invalid data URL: empty payload.");
        }
        return new DecodedDataUrl(data, mimeType);
    }

    /**
     * 从 OpenClaw 配置中提取图像模型配置。
     */
    public static ImageModelConfig coerceImageModelConfig(ConfigProperties cfg) {
        // TODO: once OpenClawConfig has agents.defaults.imageModel, extract it
        log.debug("coerceImageModelConfig: stub");
        return new ImageModelConfig(null, List.of());
    }

    /**
     * 强制图像生成的助手消息文本，遇到错误时抛出异常。
     *
     * @param text         助手文本
     * @param stopReason   模型的停止原因
     * @param errorMessage 模型的错误消息
     * @param provider     提供商 ID
     * @param model        模型 ID
     * @return 修剪后的文本
     * @throws RuntimeException 如果模型出错或返回空文本
     */
    public static String coerceImageAssistantText(
            String text, String stopReason, String errorMessage,
            String provider, String model) {
        String trimmedError = (errorMessage != null) ? errorMessage.trim() : null;
        if ("error".equals(stopReason) || "aborted".equals(stopReason)) {
            String msg = (trimmedError != null && !trimmedError.isEmpty())
                    ? String.format("Image model failed (%s/%s): %s", provider, model, trimmedError)
                    : String.format("Image model failed (%s/%s)", provider, model);
            throw new RuntimeException(msg);
        }
        if (trimmedError != null && !trimmedError.isEmpty()) {
            throw new RuntimeException(
                    String.format("Image model failed (%s/%s): %s", provider, model, trimmedError));
        }
        if (text != null && !text.trim().isEmpty()) {
            return text.trim();
        }
        throw new RuntimeException(
                String.format("Image model returned no text (%s/%s).", provider, model));
    }

    /**
     * 从提供商配置中解析支持视觉的模型。
     *
     * @return provider/modelId 字符串，如果未找到则返回 null
     */
    public static String resolveProviderVisionModel(ConfigProperties cfg, String provider) {
        // TODO: once OpenClawConfig has models.providers with model lists, look up
        // vision models
        log.debug("resolveProviderVisionModel: stub for provider={}", provider);
        return null;
    }
}

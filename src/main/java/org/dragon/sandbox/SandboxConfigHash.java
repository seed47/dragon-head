package org.dragon.sandbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.dragon.sandbox.SandboxTypes.SandboxDockerConfig;
import org.dragon.sandbox.SandboxTypes.SandboxWorkspaceAccess;
import lombok.Builder;
import lombok.Data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 沙箱配置哈希计算
 * 用于检测配置变化，确定是否需要重建容器
 */
public final class SandboxConfigHash {

    private SandboxConfigHash() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    /** 哈希计算的输入 */
    @Data
    @Builder
    public static class SandboxHashInput {
        private SandboxDockerConfig docker;
        private SandboxWorkspaceAccess workspaceAccess;
        private String workspaceDir;
        private String agentWorkspaceDir;
    }

    /**
     * 计算标准化沙箱配置的 SHA-1 哈希值
     * 用于检测配置变化
     *
     * @param input 哈希输入
     * @return 哈希值（十六进制字符串）
     */
    public static String computeSandboxConfigHash(SandboxHashInput input) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("docker", MAPPER.convertValue(input.getDocker(), Map.class));
            payload.put("workspaceAccess", input.getWorkspaceAccess() != null
                    ? input.getWorkspaceAccess().name().toLowerCase()
                    : "none");
            payload.put("workspaceDir", input.getWorkspaceDir());
            payload.put("agentWorkspaceDir", input.getAgentWorkspaceDir());

            // 标准化：排序键、移除 null 值
            Object normalized = normalizeForHash(payload);
            String raw = MAPPER.writeValueAsString(normalized);

            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("计算沙箱配置哈希失败", e);
        }
    }

    /**
     * 标准化对象以便计算哈希
     */
    @SuppressWarnings("unchecked")
    private static Object normalizeForHash(Object value) {
        if (value == null)
            return null;

        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>();
            for (Object item : list) {
                Object n = normalizeForHash(item);
                if (n != null)
                    normalized.add(n);
            }
            // 排序基本类型
            if (normalized.stream().allMatch(SandboxConfigHash::isPrimitive)) {
                normalized.sort(Comparator.comparing(Object::toString));
            }
            return normalized;
        }

        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object n = normalizeForHash(entry.getValue());
                if (n != null) {
                    sorted.put(String.valueOf(entry.getKey()), n);
                }
            }
            return sorted;
        }

        return value;
    }

    /**
     * 判断是否为基本类型
     */
    private static boolean isPrimitive(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }
}

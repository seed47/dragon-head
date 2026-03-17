package org.dragon.agent.llm.usage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 用量统计
 * 记录每次 LLM 调用的元数据和 token 消耗
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class UsageTracker {

    /**
     * 调用记录存储
     */
    private final List<LLMCallRecord> records = new ArrayList<>();

    /**
     * 记录调用
     *
     * @param record 调用记录
     */
    public void record(LLMCallRecord record) {
        synchronized (records) {
            records.add(record);
        }
        log.debug("[UsageTracker] Recorded call: model={}, tokens={}, latency={}ms",
                record.getModelId(), record.getTotalTokens(), record.getLatencyMs());
    }

    /**
     * 获取用量汇总
     *
     * @param characterId Character ID
     * @param period      时间段
     * @return 用量汇总
     */
    public UsageSummary getSummary(String characterId, Duration period) {
        LocalDateTime from = LocalDateTime.now().minus(period);

        List<LLMCallRecord> filtered;
        synchronized (records) {
            filtered = records.stream()
                    .filter(r -> r.getCharacterId() != null && r.getCharacterId().equals(characterId))
                    .filter(r -> r.getTimestamp().isAfter(from))
                    .collect(Collectors.toList());
        }

        int totalPromptTokens = filtered.stream().mapToInt(LLMCallRecord::getPromptTokens).sum();
        int totalCompletionTokens = filtered.stream().mapToInt(LLMCallRecord::getCompletionTokens).sum();
        long totalLatency = filtered.stream().mapToLong(LLMCallRecord::getLatencyMs).sum();
        long successCount = filtered.stream()
                .filter(r -> r.getStatus() == LLMCallRecord.ResponseStatus.SUCCESS)
                .count();
        long errorCount = filtered.size() - successCount;

        return UsageSummary.builder()
                .characterId(characterId)
                .from(from)
                .to(LocalDateTime.now())
                .totalCalls(filtered.size())
                .successCalls((int) successCount)
                .errorCalls((int) errorCount)
                .totalPromptTokens(totalPromptTokens)
                .totalCompletionTokens(totalCompletionTokens)
                .totalTokens(totalPromptTokens + totalCompletionTokens)
                .totalLatencyMs(totalLatency)
                .averageLatencyMs(filtered.isEmpty() ? 0 : totalLatency / filtered.size())
                .build();
    }

    /**
     * 获取模型用量
     *
     * @param modelId 模型 ID
     * @param period  时间段
     * @return 模型用量
     */
    public Map<String, Integer> getModelUsage(String modelId, Duration period) {
        LocalDateTime from = LocalDateTime.now().minus(period);

        synchronized (records) {
            return records.stream()
                    .filter(r -> r.getModelId().equals(modelId))
                    .filter(r -> r.getTimestamp().isAfter(from))
                    .collect(Collectors.groupingBy(
                            LLMCallRecord::getModelId,
                            Collectors.summingInt(LLMCallRecord::getTotalTokens)
                    ));
        }
    }

    /**
     * 用量汇总
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UsageSummary {
        private String characterId;
        private LocalDateTime from;
        private LocalDateTime to;
        private int totalCalls;
        private int successCalls;
        private int errorCalls;
        private int totalPromptTokens;
        private int totalCompletionTokens;
        private int totalTokens;
        private long totalLatencyMs;
        private long averageLatencyMs;
    }
}

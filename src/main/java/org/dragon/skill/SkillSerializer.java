package org.dragon.skill;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 技能任务序列化器。
 * 按键序列化异步任务，确保每个键一次只运行一个任务。
 * 具有相同键的后续调用将在当前任务之后排队。
 *
 * @see <a href="https://github.com/xxx/skills/serialize.ts">TypeScript 对应实现</a>
 * @since 1.0
 */
public final class SkillSerializer {

    private SkillSerializer() {
    }

    private static final Map<String, CompletableFuture<?>> SYNC_QUEUE = new ConcurrentHashMap<>();

    /**
     * 按键串行执行任务。如果具有相同键的任务已经在运行，
     * 新任务将等待其首先完成。
     *
     * @param key  序列化键
     * @param task 要执行的任务
     * @param <T>  结果类型
     * @return 包含任务结果的 CompletableFuture
     */
    public static <T> CompletableFuture<T> serializeByKey(String key, Supplier<CompletableFuture<T>> task) {
        CompletableFuture<?> prev = SYNC_QUEUE.getOrDefault(key, CompletableFuture.completedFuture(null));

        CompletableFuture<T> next = prev
                .handle((ignored, ex) -> null) // 即使失败也始终继续
                .thenCompose(ignored -> task.get());

        SYNC_QUEUE.put(key, next);

        // 完成后清理
        return next.whenComplete((result, ex) -> {
            if (SYNC_QUEUE.get(key) == next) {
                SYNC_QUEUE.remove(key);
            }
        });
    }

    /**
     * 按键串行执行同步任务。
     */
    public static <T> CompletableFuture<T> serializeByKeySync(String key, Supplier<T> task) {
        return serializeByKey(key, () -> CompletableFuture.supplyAsync(task));
    }
}

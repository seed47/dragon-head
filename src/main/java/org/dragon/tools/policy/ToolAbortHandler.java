package org.dragon.tools.policy;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 工具执行的中止信号包装器 — 在执行前检查中止，
 * 合并信号。
 * 对应 TypeScript pi-tools.abort.ts。
 *
 * 在 Java 中，我们使用 Thread.interrupted() 作为中止机制。
 */
@Slf4j
public final class ToolAbortHandler {

    private ToolAbortHandler() {
    }

    /**
     * 工具执行中止时抛出的异常。
     */
    public static class AbortException extends RuntimeException {
        public AbortException() {
            super("Aborted");
        }

        public AbortException(String message) {
            super(message);
        }
    }

    /**
     * 检查当前线程是否已被中断（请求中止）。
     *
     * @throws AbortException 如果线程被中断
     */
    public static void checkAborted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new AbortException();
        }
    }

    /**
     * 使用中止检查执行工具 — 在执行开始前检查。
     *
     * @param toolName 工具名称，用于日志记录
     * @param runner   实际的工具执行逻辑
     * @return 工具结果
     * @throws AbortException 如果在执行前或执行期间中止
     */
    public static Object executeWithAbortCheck(String toolName, ToolRunner runner) {
        checkAborted();
        try {
            return runner.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AbortException("Tool " + toolName + " was aborted");
        } catch (AbortException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Tool " + toolName + " failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface ToolRunner {
        Object run() throws Exception;
    }
}

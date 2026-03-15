package org.dragon.schedule.executor;

import lombok.extern.slf4j.Slf4j;
import org.dragon.schedule.config.ScheduleProperties;
import org.dragon.schedule.entity.*;
import org.dragon.schedule.store.ExecutionHistoryStore;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 默认任务执行器实现
 */
@Slf4j
public class DefaultJobExecutor implements JobExecutor {

    private final ScheduleProperties properties;
    private final ExecutionHistoryStore executionHistoryStore;
    private final ApplicationContext applicationContext;

    private ExecutorService executorService;
    private final Map<String, ExecutionContext> executingJobs = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger executionCounter = new AtomicInteger(0);

    public DefaultJobExecutor(ScheduleProperties properties,
                             ExecutionHistoryStore executionHistoryStore,
                             ApplicationContext applicationContext) {
        this.properties = properties;
        this.executionHistoryStore = executionHistoryStore;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        // 初始化线程池
        ScheduleProperties.ThreadPoolProperties threadPool = properties.getThreadPool();
        
        executorService = new ThreadPoolExecutor(
                threadPool.getCorePoolSize(),
                threadPool.getMaximumPoolSize(),
                threadPool.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(threadPool.getQueueCapacity()),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "cron-executor-" + counter.incrementAndGet());
                        thread.setDaemon(false);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        running.set(true);
        log.info("DefaultJobExecutor initialized with thread pool: core={}, max={}, queue={}",
                threadPool.getCorePoolSize(), threadPool.getMaximumPoolSize(), threadPool.getQueueCapacity());
    }

    @PreDestroy
    public void destroy() {
        running.set(false);
        
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("DefaultJobExecutor destroyed");
    }

    @Override
    public ExecutionHistory execute(CronDefinition definition, JobContext context) {
        String executionId = generateExecutionId();
        
        // 创建执行历史
        ExecutionHistory history = createExecutionHistory(definition, context, executionId);
        executionHistoryStore.save(history);

        // 执行
        return doExecute(definition, context, history);
    }

    @Override
    public CompletableFuture<ExecutionHistory> executeAsync(CronDefinition definition, JobContext context) {
        return CompletableFuture.supplyAsync(() -> execute(definition, context), executorService);
    }

    @Override
    public ExecutionHistory executeWithHandler(CronDefinition definition, JobContext context, JobHandler handler) {
        String executionId = generateExecutionId();
        
        // 创建执行历史
        ExecutionHistory history = createExecutionHistory(definition, context, executionId);
        executionHistoryStore.save(history);

        // 执行
        return doExecuteWithHandler(definition, context, history, handler);
    }

    @Override
    public boolean cancel(String executionId) {
        if (executionId == null) {
            return false;
        }

        ExecutionContext context = executingJobs.get(executionId);
        if (context == null) {
            return false;
        }

        context.cancelled = true;
        if (context.thread != null) {
            context.thread.interrupt();
        }

        log.info("Cancelled job execution: executionId={}", executionId);
        return true;
    }

    @Override
    public boolean isExecuting(String executionId) {
        if (executionId == null) {
            return false;
        }
        return executingJobs.containsKey(executionId);
    }

    @Override
    public List<ExecutionHistory> getExecutingJobs() {
        return executingJobs.values().stream()
                .map(ctx -> executionHistoryStore.findByExecutionId(ctx.executionId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public ThreadPoolStatus getThreadPoolStatus() {
        if (!(executorService instanceof ThreadPoolExecutor)) {
            return new ThreadPoolStatus();
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) executorService;
        ThreadPoolStatus status = new ThreadPoolStatus();
        status.setCorePoolSize(executor.getCorePoolSize());
        status.setMaximumPoolSize(executor.getMaximumPoolSize());
        status.setActiveCount(executor.getActiveCount());
        status.setCompletedTaskCount(executor.getCompletedTaskCount());
        status.setTaskCount(executor.getTaskCount());
        status.setLargestPoolSize(executor.getLargestPoolSize());
        status.setPoolSize(executor.getPoolSize());
        status.setQueueSize(executor.getQueue().size());
        status.setRemainingQueueCapacity(executor.getQueue().remainingCapacity());
        return status;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("DefaultJobExecutor started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("DefaultJobExecutor stopped");
        }
    }

    // ==================== Private Methods ====================

    private String generateExecutionId() {
        return "EXEC-" + System.currentTimeMillis() + "-" + executionCounter.incrementAndGet();
    }

    private ExecutionHistory createExecutionHistory(CronDefinition definition, JobContext context, String executionId) {
        ExecutionHistory history = new ExecutionHistory();
        history.setExecutionId(executionId);
        history.setCronId(definition.getId());
        history.setCronName(definition.getName());
        history.setTriggerTime(context.getScheduledFireTime());
        history.setStatus(ExecutionStatus.RUNNING);
        history.setRetryCount(0);
        return history;
    }

    private ExecutionHistory doExecute(CronDefinition definition, JobContext context, ExecutionHistory history) {
        // 获取处理器
        JobHandler handler = resolveHandler(definition);
        if (handler == null) {
            throw new IllegalStateException("No handler found for cron: " + definition.getId());
        }

        return doExecuteWithHandler(definition, context, history, handler);
    }

    private ExecutionHistory doExecuteWithHandler(CronDefinition definition, JobContext context, 
                                                   ExecutionHistory history, JobHandler handler) {
        ExecutionContext execContext = new ExecutionContext();
        execContext.executionId = history.getExecutionId();
        execContext.thread = Thread.currentThread();
        execContext.startTime = System.currentTimeMillis();

        executingJobs.put(history.getExecutionId(), execContext);

        try {
            // 记录开始
            history.markStarted(properties.getNodeId(), execContext.thread.getName());
            executionHistoryStore.update(history);

            // beforeExecute 回调
            handler.beforeExecute(definition, context);

            // 执行
            Object result = handler.execute(definition, context);
            context.setResult(result);

            // afterExecute 回调
            handler.afterExecute(definition, context, result, null);

            // 记录成功
            history.markSuccess(result != null ? result.toString() : null);
            log.info("Job executed successfully: executionId={}, cronId={}, duration={}ms",
                    history.getExecutionId(), definition.getId(), history.getDurationMs());

        } catch (Exception e) {
            // afterExecute 回调
            handler.afterExecute(definition, context, null, e);

            // 记录失败
            history.markFailed(e.getMessage(), getStackTrace(e));
            log.error("Job execution failed: executionId={}, cronId={}, error={}",
                    history.getExecutionId(), definition.getId(), e.getMessage(), e);

        } finally {
            executingJobs.remove(history.getExecutionId());
            history.setCompleteTime(System.currentTimeMillis());
            executionHistoryStore.update(history);
        }

        return history;
    }

    private JobHandler resolveHandler(CronDefinition definition) {
        String handler = definition.getJobHandler();
        if (handler == null || handler.trim().isEmpty()) {
            return null;
        }

        switch (definition.getJobType()) {
            case SPRING_BEAN:
                try {
                    return (JobHandler) applicationContext.getBean(handler);
                } catch (Exception e) {
                    log.error("Failed to resolve Spring bean handler: {}", handler, e);
                    return null;
                }
            case CLASS_NAME:
                try {
                    Class<?> clazz = Class.forName(handler);
                    return (JobHandler) clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    log.error("Failed to instantiate handler class: {}", handler, e);
                    return null;
                }
            default:
                return null;
        }
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }

    /**
     * 执行上下文
     */
    private static class ExecutionContext {
        String executionId;
        Thread thread;
        long startTime;
        volatile boolean cancelled = false;
    }
}

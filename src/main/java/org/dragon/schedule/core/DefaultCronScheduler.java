package org.dragon.schedule.core;

import lombok.extern.slf4j.Slf4j;
import org.dragon.schedule.config.ScheduleProperties;
import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.CronStatus;
import org.dragon.schedule.entity.ExecutionHistory;
import org.dragon.schedule.entity.JobContext;
import org.dragon.schedule.executor.JobExecutor;
import org.dragon.schedule.lock.DistributedLock;
import org.dragon.schedule.parser.CronExpression;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 默认 Cron 调度器实现
 */
@Slf4j
public class DefaultCronScheduler implements CronScheduler, TriggerManager {

    private final JobExecutor jobExecutor;
    private final DistributedLock distributedLock;
    private final ScheduleProperties properties;

    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, CronDefinition> cronDefinitions = new ConcurrentHashMap<>();
    private final Map<String, Long> nextFireTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> previousFireTimes = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger executionCounter = new AtomicInteger(0);

    public DefaultCronScheduler(JobExecutor jobExecutor,
                                DistributedLock distributedLock,
                                ScheduleProperties properties) {
        this.jobExecutor = jobExecutor;
        this.distributedLock = distributedLock;
        this.properties = properties;

        // 创建调度线程池
        this.scheduler = Executors.newScheduledThreadPool(5, r -> {
            Thread thread = new Thread(r, "cron-scheduler-" + executionCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @PostConstruct
    public void init() {
        log.info("DefaultCronScheduler initialized");
    }

    @PreDestroy
    public void destroy() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("DefaultCronScheduler destroyed");
    }

    // ==================== CronScheduler Implementation ====================

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("DefaultCronScheduler started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            // 取消所有调度任务
            for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
                entry.getValue().cancel(false);
                log.debug("Cancelled scheduled task: cronId={}", entry.getKey());
            }
            scheduledTasks.clear();
            log.info("DefaultCronScheduler stopped");
        }
    }

    @Override
    public String registerCron(CronDefinition definition) {
        if (!running.get()) {
            throw new IllegalStateException("Scheduler is not running");
        }

        String cronId = definition.getId();
        cronDefinitions.put(cronId, definition);

        // 创建触发器
        createTrigger(definition);

        log.info("Registered cron job: id={}, name={}, expression={}",
                cronId, definition.getName(), definition.getCronExpression());

        return cronId;
    }

    @Override
    public void unregisterCron(String cronId) {
        // 移除触发器
        removeTrigger(cronId);

        // 从内存中移除
        cronDefinitions.remove(cronId);
        nextFireTimes.remove(cronId);
        previousFireTimes.remove(cronId);

        log.info("Unregistered cron job: id={}", cronId);
    }

    @Override
    public void updateCron(CronDefinition definition) {
        String cronId = definition.getId();

        // 更新内存中的定义
        cronDefinitions.put(cronId, definition);

        // 更新触发器
        updateTrigger(definition);

        log.info("Updated cron job: id={}, name={}", cronId, definition.getName());
    }

    @Override
    public void pauseCron(String cronId) {
        pauseTrigger(cronId);
        log.info("Paused cron job: id={}", cronId);
    }

    @Override
    public void resumeCron(String cronId) {
        resumeTrigger(cronId);
        log.info("Resumed cron job: id={}", cronId);
    }

    @Override
    public Optional<CronDefinition> getCron(String cronId) {
        return Optional.ofNullable(cronDefinitions.get(cronId));
    }

    @Override
    public List<CronDefinition> listCrons() {
        return new ArrayList<>(cronDefinitions.values());
    }

    @Override
    public List<CronDefinition> listCronsByStatus(CronStatus status) {
        return cronDefinitions.values().stream()
                .filter(cron -> status.equals(cron.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    // ==================== TriggerManager Implementation ====================

    @Override
    public void createTrigger(CronDefinition definition) {
        String cronId = definition.getId();
        String cronExpression = definition.getCronExpression();


        // 计算下次触发时间
        long now = System.currentTimeMillis();
        Long nextFireTime = calculateNextFireTime(cronExpression, now);

        if (nextFireTime == null) {
            log.warn("No next fire time calculated for cron: id={}, expression={}", cronId, cronExpression);
            return;
        }

        nextFireTimes.put(cronId, nextFireTime);

        // 调度任务
        scheduleTask(cronId, definition, nextFireTime);

        log.debug("Created trigger for cron: id={}, nextFireTime={}", cronId, new Date(nextFireTime));
    }

    @Override
    public void removeTrigger(String cronId) {
        ScheduledFuture<?> future = scheduledTasks.remove(cronId);
        if (future != null) {
            future.cancel(false);
        }
        nextFireTimes.remove(cronId);
        log.debug("Removed trigger for cron: id={}", cronId);
    }

    @Override
    public void updateTrigger(CronDefinition definition) {
        // 移除旧的触发器
        removeTrigger(definition.getId());
        // 创建新的触发器
        createTrigger(definition);
    }

    @Override
    public void pauseTrigger(String cronId) {
        // 在内存中标记为暂停，调度任务检查状态
        CronDefinition definition = cronDefinitions.get(cronId);
        if (definition != null) {
            definition.setStatus(CronStatus.PAUSED);
        }
        log.debug("Paused trigger for cron: id={}", cronId);
    }

    @Override
    public void resumeTrigger(String cronId) {
        // 恢复状态
        CronDefinition definition = cronDefinitions.get(cronId);
        if (definition != null) {
            definition.setStatus(CronStatus.ENABLED);
            // 重新计算下次触发时间并调度
            createTrigger(definition);
        }
        log.debug("Resumed trigger for cron: id={}", cronId);
    }

    @Override
    public void triggerNow(String cronId) {
        CronDefinition definition = cronDefinitions.get(cronId);
        if (definition == null) {
            log.warn("Cannot trigger, cron not found: id={}", cronId);
            return;
        }

        // 立即执行
        executeJob(definition, System.currentTimeMillis());
    }

    @Override
    public boolean hasTrigger(String cronId) {
        return scheduledTasks.containsKey(cronId);
    }

    @Override
    public Long getNextFireTime(String cronId) {
        return nextFireTimes.get(cronId);
    }

    @Override
    public Long getPreviousFireTime(String cronId) {
        return previousFireTimes.get(cronId);
    }

    // ==================== Private Methods ====================

    private Long calculateNextFireTime(String cronExpression, long afterTime) {
        try {
            CronExpression expression = CronExpression.parse(cronExpression);
            return expression.getNextValidTimeAfter(afterTime);
        } catch (Exception e) {
            log.error("Failed to calculate next fire time: expression={}, error={}", 
                    cronExpression, e.getMessage());
            return null;
        }
    }

    private void scheduleTask(String cronId, CronDefinition definition, long fireTime) {
        long now = System.currentTimeMillis();
        long delay = fireTime - now;

        if (delay < 0) {
            delay = 0;
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                // 检查状态
                if (definition.getStatus() != CronStatus.ENABLED) {
                    log.debug("Skipping execution, cron not enabled: id={}, status={}", 
                            cronId, definition.getStatus());
                    return;
                }

                // 执行
                executeJob(definition, fireTime);

                // 记录上次触发时间
                previousFireTimes.put(cronId, fireTime);

                // 计算并调度下次触发
                Long nextFireTime = calculateNextFireTime(
                        definition.getCronExpression(), 
                        System.currentTimeMillis()
                );

                if (nextFireTime != null) {
                    nextFireTimes.put(cronId, nextFireTime);
                    scheduleTask(cronId, definition, nextFireTime);
                }

            } catch (Exception e) {
                log.error("Error executing scheduled task: cronId={}", cronId, e);
            }
        }, delay, TimeUnit.MILLISECONDS);

        scheduledTasks.put(cronId, future);
    }

    private void executeJob(CronDefinition definition, long fireTime) {
        try {
            // 获取分布式锁
            String lockKey = properties.getLock().getKeyPrefix() + definition.getId();
            boolean locked = distributedLock.tryLock(lockKey, 10, properties.getLock().getDefaultLockTtl(), TimeUnit.MILLISECONDS);

            if (!locked) {
                log.debug("Could not acquire lock for cron: id={}, skipping execution", definition.getId());
                return;
            }

            try {
                // 创建执行上下文
                String executionId = generateExecutionId();
                JobContext context = JobContext.from(definition, executionId, fireTime);
                context.setPrevFireTime(previousFireTimes.get(definition.getId()));

                // 计算下次触发时间
                Long nextFireTime = calculateNextFireTime(definition.getCronExpression(), fireTime);
                context.setNextFireTime(nextFireTime);

                // 执行
                log.info("Executing cron job: id={}, name={}, executionId={}",
                        definition.getId(), definition.getName(), executionId);

                ExecutionHistory history = jobExecutor.execute(definition, context);

                log.info("Cron job executed: id={}, executionId={}, status={}, duration={}ms",
                        definition.getId(), executionId, history.getStatus(), history.getDurationMs());

            } finally {
                distributedLock.unlock(lockKey);
            }

        } catch (Exception e) {
            log.error("Error executing cron job: id={}, error={}", definition.getId(), e.getMessage(), e);
        }
    }

    private String generateExecutionId() {
        return "EXEC-" + System.currentTimeMillis() + "-" + executionCounter.incrementAndGet();
    }
}

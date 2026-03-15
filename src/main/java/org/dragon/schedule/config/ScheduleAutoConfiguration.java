package org.dragon.schedule.config;

import lombok.extern.slf4j.Slf4j;
import org.dragon.schedule.core.*;
import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.executor.DefaultJobExecutor;
import org.dragon.schedule.executor.JobExecutor;
import org.dragon.schedule.lock.DistributedLock;
import org.dragon.schedule.lock.LocalDistributedLock;
import org.dragon.schedule.parser.CronExpression;
import org.dragon.schedule.store.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 调度模块自动配置
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ScheduleProperties.class)
@ConditionalOnProperty(prefix = "dragon.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleAutoConfiguration {

    @Autowired
    private ScheduleProperties properties;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        log.info("Initializing Dragon Schedule Module with properties: {}", properties);
    }

    // ==================== Storage Beans ====================

    @Bean
    @ConditionalOnMissingBean(CronStore.class)
    @ConditionalOnProperty(prefix = "dragon.schedule.store", name = "type", havingValue = "memory", matchIfMissing = true)
    public CronStore memoryCronStore() {
        log.info("Creating MemoryCronStore");
        return new MemoryCronStore();
    }

    @Bean
    @ConditionalOnMissingBean(ExecutionHistoryStore.class)
    @ConditionalOnProperty(prefix = "dragon.schedule.store", name = "type", havingValue = "memory", matchIfMissing = true)
    public ExecutionHistoryStore memoryExecutionHistoryStore() {
        log.info("Creating MemoryExecutionHistoryStore");
        return new MemoryExecutionHistoryStore();
    }

    // ==================== Distributed Lock Beans ====================

    @Bean
    @ConditionalOnMissingBean(DistributedLock.class)
    @ConditionalOnProperty(prefix = "dragon.schedule.lock", name = "type", havingValue = "memory", matchIfMissing = true)
    public DistributedLock localDistributedLock() {
        log.info("Creating LocalDistributedLock");
        return new LocalDistributedLock();
    }

    // ==================== Core Beans ====================

    @Bean
    @ConditionalOnMissingBean(JobExecutor.class)
    public JobExecutor jobExecutor(
            ScheduleProperties properties,
            ExecutionHistoryStore executionHistoryStore,
            ApplicationContext applicationContext) {
        log.info("Creating DefaultJobExecutor");
        return new DefaultJobExecutor(properties, executionHistoryStore, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(CronScheduler.class)
    public CronScheduler cronScheduler(
            CronStore cronStore,
            ExecutionHistoryStore executionHistoryStore,
            JobExecutor jobExecutor,
            DistributedLock distributedLock,
            ScheduleProperties properties) {
        log.info("Creating DefaultCronScheduler");
        return new DefaultCronScheduler(
                cronStore,
                executionHistoryStore,
                jobExecutor,
                distributedLock,
                properties);
    }

    // ==================== Lifecycle ====================

    @Bean
    public ScheduleLifecycle scheduleLifecycle(CronScheduler cronScheduler, CronStore cronStore) {
        return new ScheduleLifecycle(cronScheduler, cronStore);
    }

    /**
     * 调度生命周期管理
     */
    public static class ScheduleLifecycle {

        private final CronScheduler cronScheduler;
        private final CronStore cronStore;

        public ScheduleLifecycle(CronScheduler cronScheduler, CronStore cronStore) {
            this.cronScheduler = cronScheduler;
            this.cronStore = cronStore;
        }

        @javax.annotation.PostConstruct
        public void start() {
            log.info("Starting Cron Scheduler...");

            // 启动调度器
            cronScheduler.start();

            // 加载已启用的 Cron 任务
            List<CronDefinition> enabledCrons = cronStore.findByStatus(org.dragon.schedule.entity.CronStatus.ENABLED);
            log.info("Loading {} enabled cron jobs from store", enabledCrons.size());

            for (CronDefinition cron : enabledCrons) {
                try {
                    cronScheduler.registerCron(cron);
                    log.debug("Registered cron job: id={}, name={}", cron.getId(), cron.getName());
                } catch (Exception e) {
                    log.error("Failed to register cron job: id={}, error={}", cron.getId(), e.getMessage(), e);
                }
            }

            log.info("Cron Scheduler started successfully");
        }

        @javax.annotation.PreDestroy
        public void stop() {
            log.info("Stopping Cron Scheduler...");
            cronScheduler.stop();
            log.info("Cron Scheduler stopped");
        }
    }
}

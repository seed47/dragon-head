package org.dragon.schedule.example;

import lombok.extern.slf4j.Slf4j;
import org.dragon.schedule.core.CronScheduler;
import org.dragon.schedule.entity.*;
import org.dragon.schedule.executor.JobHandler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OpenClaw Cron 调度系统使用示例
 * 
 * 演示内容：
 * 1. 启动调度器
 * 2. 注册不同类型的 Cron 任务
 * 3. 定义 JobHandler 处理业务逻辑
 * 4. 任务状态管理（暂停、恢复、手动触发）
 */
@Slf4j
@Component
public class ScheduleExample implements CommandLineRunner {

    private final CronScheduler cronScheduler;

    public ScheduleExample(CronScheduler cronScheduler) {
        this.cronScheduler = cronScheduler;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("OpenClaw Cron 调度系统使用示例");
        log.info("========================================");

        // 1. 启动调度器
        startScheduler();

        // 2. 注册定时任务
        registerCronJobs();

        // 3. 模拟运行时操作
        simulateRuntimeOperations();

        log.info("========================================");
        log.info("示例执行完成");
        log.info("========================================");
    }

    /**
     * 1. 启动调度器
     */
    private void startScheduler() {
        log.info("\n[步骤1] 启动调度器...");
        
        if (!cronScheduler.isRunning()) {
            cronScheduler.start();
            log.info("✓ 调度器已启动");
        } else {
            log.info("✓ 调度器已在运行中");
        }
    }

    /**
     * 2. 注册各种 Cron 任务
     */
    private void registerCronJobs() {
        log.info("\n[步骤2] 注册 Cron 任务...");

        // 2.1 每5秒执行一次的任务
        registerFrequentJob();

        // 2.2 每分钟执行一次的任务
        registerMinutelyJob();

        // 2.3 每小时执行一次的任务
        registerHourlyJob();

        // 2.4 每天特定时间执行的任务
        registerDailyJob();

        // 2.5 带参数的任务
        registerJobWithParams();

        log.info("✓ 所有 Cron 任务已注册");
    }

    /**
     * 注册高频任务 (每5秒)
     */
    private void registerFrequentJob() {
        CronDefinition job = CronDefinition.builder()
                .id("frequent-job")
                .name("高频任务")
                .cronExpression("0/5 * * * * ?")  // 每5秒
                .jobType(JobType.SPRING_BEAN)
                .jobHandler("demoJobHandler")
                .status(CronStatus.ENABLED)
                .misfirePolicy(MisfirePolicy.FIRE_NOW)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .version(0)
                .build();

        String cronId = cronScheduler.registerCron(job);
        log.info("  ✓ 已注册: {} (每5秒)", job.getName());
    }

    /**
     * 注册分钟级任务
     */
    private void registerMinutelyJob() {
        CronDefinition job = CronDefinition.builder()
                .id("minutely-job")
                .name("分钟级任务")
                .cronExpression("0 * * * * ?")  // 每分钟
                .jobType(JobType.SPRING_BEAN)
                .jobHandler("dataSyncJobHandler")
                .status(CronStatus.ENABLED)
                .timeoutMs(50000)  // 50秒超时
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .version(0)
                .build();

        String cronId = cronScheduler.registerCron(job);
        log.info("  ✓ 已注册: {} (每分钟)", job.getName());
    }

    /**
     * 注册小时级任务
     */
    private void registerHourlyJob() {
        CronDefinition job = CronDefinition.builder()
                .id("hourly-job")
                .name("小时级任务")
                .cronExpression("0 0 * * * ?")  // 每小时
                .jobType(JobType.SPRING_BEAN)
                .jobHandler("reportJobHandler")
                .status(CronStatus.ENABLED)
                .timeoutMs(300000)  // 5分钟超时
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .version(0)
                .build();

        String cronId = cronScheduler.registerCron(job);
        log.info("  ✓ 已注册: {} (每小时)", job.getName());
    }

    /**
     * 注册日级任务 (特定时间)
     */
    private void registerDailyJob() {
        CronDefinition job = CronDefinition.builder()
                .id("daily-job")
                .name("每日任务")
                .cronExpression("0 0 8 * * ?")  // 每天8点
                .jobType(JobType.SPRING_BEAN)
                .jobHandler("backupJobHandler")
                .status(CronStatus.ENABLED)
                .misfirePolicy(MisfirePolicy.FIRE_NOW)  // 错过立即触发
                .timeoutMs(600000)  // 10分钟超时
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .version(0)
                .build();

        String cronId = cronScheduler.registerCron(job);
        log.info("  ✓ 已注册: {} (每天8点)", job.getName());
    }

    /**
     * 注册带参数的任务
     */
    private void registerJobWithParams() {
        Map<String, Object> jobData = new HashMap<>();
        jobData.put("targetUrl", "https://api.example.com/data");
        jobData.put("timeout", 30000);
        jobData.put("retryOnFailure", true);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("Content-Type", "application/json");
        jobData.put("headers", headers);

        CronDefinition job = CronDefinition.builder()
                .id("api-call-job")
                .name("API调用任务")
                .cronExpression("0 */5 * * * ?")  // 每5分钟
                .jobType(JobType.SPRING_BEAN)
                .jobHandler("apiCallJobHandler")
                .jobData(jobData)
                .status(CronStatus.ENABLED)
                .timeoutMs(30000)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .version(0)
                .build();

        String cronId = cronScheduler.registerCron(job);
        log.info("  ✓ 已注册: {} (带参数)", job.getName());
    }

    /**
     * 3. 模拟运行时操作
     */
    private void simulateRuntimeOperations() throws InterruptedException {
        log.info("\n[步骤3] 模拟运行时操作...");

        // 等待任务执行几次
        log.info("  等待5秒，观察任务执行...");
        TimeUnit.SECONDS.sleep(5);

        // 列出所有任务
        log.info("\n  当前注册的所有任务:");
        cronScheduler.listCrons().forEach(cron -> {
            log.info("    - {} (ID: {}, 状态: {}, 表达式: {})",
                    cron.getName(),
                    cron.getId(),
                    cron.getStatus(),
                    cron.getCronExpression());
        });

        // 暂停一个任务
        log.info("\n  暂停 '高频任务'...");
        cronScheduler.pauseCron("frequent-job");
        log.info("  ✓ 已暂停");

        // 恢复任务
        TimeUnit.SECONDS.sleep(2);
        log.info("\n  恢复 '高频任务'...");
        cronScheduler.resumeCron("frequent-job");
        log.info("  ✓ 已恢复");

        // 手动触发一个任务
        log.info("\n  手动触发 '分钟级任务'...");
        cronScheduler.triggerNow("minutely-job");
        log.info("  ✓ 已触发");

        // 再等待一段时间
        log.info("\n  再等待10秒观察执行...");
        TimeUnit.SECONDS.sleep(10);

        log.info("\n✓ 运行时操作模拟完成");
    }
}

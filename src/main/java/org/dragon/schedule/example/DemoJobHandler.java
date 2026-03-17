package org.dragon.schedule.example;

import lombok.extern.slf4j.Slf4j;
import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.JobContext;
import org.dragon.schedule.executor.JobHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component("demoJobHandler")
public class DemoJobHandler implements JobHandler {

    @Override
    public Object execute(CronDefinition definition, JobContext context) throws Exception {
        log.info("[{}] Executing job: {}, Trigger time: {}",
                context.getExecutionId(),
                definition.getName(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        simulateBusinessLogic();

        return "Success - " + System.currentTimeMillis();
    }

    private void simulateBusinessLogic() throws InterruptedException {
        long sleepTime = 100 + (long) (Math.random() * 400);
        Thread.sleep(sleepTime);
    }

    @Override
    public void beforeExecute(CronDefinition definition, JobContext context) {
        log.debug("[{}] Preparing to execute: {}", context.getExecutionId(), definition.getName());
    }

    @Override
    public void afterExecute(CronDefinition definition, JobContext context, Object result, Throwable throwable) {
        if (throwable != null) {
            log.error("[{}] Job execution failed: {}", context.getExecutionId(), throwable.getMessage());
        } else {
            log.debug("[{}] Job execution completed, result: {}", context.getExecutionId(), result);
        }
    }
}

@Slf4j
@Component("dataSyncJobHandler")
class DataSyncJobHandler implements JobHandler {

    @Override
    public Object execute(CronDefinition definition, JobContext context) throws Exception {
        log.info("[Data Sync] Starting data synchronization");

        Map<String, Object> jobData = definition.getJobData();
        if (jobData != null) {
            log.info("  Sync target: {}", jobData.get("target"));
            log.info("  Batch size: {}", jobData.get("batchSize"));
        }

        int syncedRecords = syncData();

        log.info("[Data Sync] Sync completed, total {} records synced", syncedRecords);
        return Map.of("syncedRecords", syncedRecords, "timestamp", System.currentTimeMillis());
    }

    private int syncData() throws InterruptedException {
        Thread.sleep(2000);
        return 1000 + (int) (Math.random() * 500);
    }
}

@Slf4j
@Component("reportJobHandler")
class ReportJobHandler implements JobHandler {

    @Override
    public Object execute(CronDefinition definition, JobContext context) throws Exception {
        log.info("[Report] Starting report generation");

        String reportType = "daily";
        Map<String, Object> jobData = definition.getJobData();
        if (jobData != null && jobData.get("reportType") != null) {
            reportType = (String) jobData.get("reportType");
        }

        log.info("  Report type: {}", reportType);
        log.info("  Scheduled trigger time: {}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        String reportId = generateReport(reportType);

        log.info("[Report] Report generated, ID: {}", reportId);
        return Map.of("reportId", reportId, "type", reportType, "generatedAt", System.currentTimeMillis());
    }

    private String generateReport(String reportType) throws InterruptedException {
        Thread.sleep(3000);
        return reportType + "_report_" + System.currentTimeMillis();
    }
}

@Slf4j
@Component("backupJobHandler")
class BackupJobHandler implements JobHandler {

    @Override
    public Object execute(CronDefinition definition, JobContext context) throws Exception {
        log.info("[Backup] Starting data backup");

        String backupType = "full";
        Map<String, Object> jobData = definition.getJobData();
        if (jobData != null && jobData.get("backupType") != null) {
            backupType = (String) jobData.get("backupType");
        }

        log.info("  Backup type: {}", backupType);

        String backupId = performBackup(backupType);

        log.info("[Backup] Backup completed, ID: {}", backupId);
        return Map.of("backupId", backupId, "type", backupType, "completedAt", System.currentTimeMillis());
    }

    private String performBackup(String backupType) throws InterruptedException {
        Thread.sleep(5000);
        return backupType + "_backup_" + System.currentTimeMillis();
    }
}

@Slf4j
@Component("apiCallJobHandler")
class ApiCallJobHandler implements JobHandler {

    @Override
    public Object execute(CronDefinition definition, JobContext context) throws Exception {
        log.info("[API Call] Starting API call task");

        Map<String, Object> jobData = definition.getJobData();
        if (jobData != null) {
            String targetUrl = (String) jobData.get("targetUrl");
            Integer timeout = (Integer) jobData.get("timeout");
            Boolean retryOnFailure = (Boolean) jobData.get("retryOnFailure");

            log.info("  Target URL: {}", targetUrl);
            log.info("  Timeout: {}ms", timeout);
            log.info("  Retry on failure: {}", retryOnFailure);

            String response = callApi(targetUrl, timeout);
            log.info("[API Call] Response: {}", response);
            return response;
        }

        return "No job data provided";
    }

    private String callApi(String url, Integer timeout) throws InterruptedException {
        Thread.sleep(500);
        return "{\"status\": \"success\", \"data\": \"mock_response_" + System.currentTimeMillis() + "\"}";
    }
}

@Slf4j
@Component("criticalJobHandler")
class CriticalJobHandler implements JobHandler {

    @Override
    public Object execute(CronDefinition definition, JobContext context) throws Exception {
        log.info("[Critical] Starting critical business task");
        log.info("  Execution ID: {}", context.getExecutionId());

        try {
            processCriticalBusiness();
            log.info("[Critical] Execution successful");
            return Map.of("status", "success", "processedAt", System.currentTimeMillis());
        } catch (Exception e) {
            log.error("[Critical] Execution failed: {}", e.getMessage());
            throw e;
        }
    }

    private void processCriticalBusiness() throws Exception {
        Thread.sleep(1000);
        
        if (Math.random() < 0.3) {
            throw new RuntimeException("Simulated business processing failure");
        }
    }

    @Override
    public boolean supportsConcurrentExecution() {
        return false;
    }
}

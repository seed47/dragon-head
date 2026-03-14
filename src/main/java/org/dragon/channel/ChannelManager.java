package org.dragon.channel;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.adapter.ChannelAdapter;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.gateway.Gateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:13
 * Update Date Time:
 *
 */
@Service
@Slf4j
public class ChannelManager {

    // 渠道注册表：ChannelName -> ChannelAdapter 实例
    private final Map<String, ChannelAdapter> registry = new ConcurrentHashMap<>();

    private final Gateway gateway;

    @Autowired
    public ChannelManager(List<ChannelAdapter> adapters, Gateway gateway) {
        this.gateway = gateway;
        for (ChannelAdapter adapter : adapters) {
            registry.put(adapter.getChannelName(), adapter);
            log.info("[Manager] 成功注册channel插件: " + adapter.getChannelName());
        }
    }

    @PostConstruct
    public void startAllChannels() {
        log.info("[Manager] 正在启动所有channel监听服务...");
        for (ChannelAdapter adapter : registry.values()) {
            CompletableFuture.runAsync(() -> {
                try {
                    adapter.startListening(gateway);
                } catch (Exception e) {
                    log.error("[Manager] channel: " + adapter.getChannelName() + " 启动失败: " + e.getMessage());
                }
            });
        }
    }

    @Scheduled(fixedRate = 30000)
    public void healthCheckProbes() {
        log.info("[Watchdog] 开始执行channel健康巡检...");

        for (ChannelAdapter adapter : registry.values()) {
            try {
                if (!adapter.isHealthy()) {
                    log.error("[Watchdog] 发现channel异常宕机:{}, 准备restart", adapter.getChannelName());

                    // 必须异步执行重启，千万不能卡死巡检线程！
                    CompletableFuture.runAsync(() -> {
                        try {
                            adapter.restart();
                            log.info("[Watchdog] channel:{} restart success!", adapter.getChannelName());
                        } catch (Exception e) {
                            log.error("[Watchdog] channel restart fail，可能需要人工介入: " + e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                // 防止由于某个 Channel 的 isHealthy 抛错，导致整个巡检挂掉
                e.printStackTrace();
            }
        }
    }

    // 提供给 Gateway 或大模型使用的统一发送入口 (下行路由分发)
    public CompletableFuture<Void> routeMessageOutbound(String targetChannel, String targetUser, NormalizedMessage msg) {
        ChannelAdapter adapter = registry.get(targetChannel);
        if (adapter == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("未找到对应的channel: " + targetChannel));
        }
        return adapter.sendMessage(targetUser, msg);
    }

    // Spring Boot 关闭时自动执行，优雅释放 Netty 资源
    @PreDestroy
    public void stopAllChannels() {
        log.info("[Manager] 服务中止，正在关闭所有channel...");
        registry.values().forEach(ChannelAdapter::stop);
    }
}

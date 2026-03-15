package org.dragon.channel;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.adapter.ChannelAdapter;
import org.dragon.channel.entity.ActionMessage;
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
            log.info("[Manager] 成功注册channel插件:{}", adapter.getChannelName());
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
                    log.error("[Manager] channel:{} 启动失败", adapter.getChannelName(), e);
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
                            log.error("[Watchdog] channel:{} restart fail", adapter.getChannelName(), e);
                        }
                    });
                }
            } catch (Exception e) {
                // 防止由于某个 Channel 的 isHealthy 抛错，导致整个巡检挂掉
                log.error("[Watchdog] 健康巡检失败", e);
            }
        }
    }

    // 提供给 Gateway 或大模型使用的统一发送入口 (下行路由分发)
    public CompletableFuture<Void> routeMessageOutbound(ActionMessage message) {
        ChannelAdapter adapter = registry.get(message.getChannelName());
        if (adapter == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("未找到对应的channel: " + message.getChannelName()));
        }
        return adapter.sendMessage(message);
    }

    @PreDestroy
    public void stopAllChannels() {
        log.info("[Manager] 服务中止，正在关闭所有channel...");
        registry.values().forEach(ChannelAdapter::stop);
    }
}

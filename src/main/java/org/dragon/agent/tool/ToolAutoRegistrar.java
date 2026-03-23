package org.dragon.agent.tool;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ToolConnector 自动注册器
 * 在应用启动时自动扫描并注册所有 ToolConnector Bean 到 ToolRegistry
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolAutoRegistrar {

    private final ToolRegistry toolRegistry;
    private final List<ToolConnector> toolConnectors;

    @PostConstruct
    public void registerTools() {
        int count = 0;
        for (ToolConnector connector : toolConnectors) {
            if (connector != null && connector.getName() != null) {
                toolRegistry.register(connector);
                count++;
                log.info("[ToolAutoRegistrar] Registered tool: {}", connector.getName());
            }
        }
        log.info("[ToolAutoRegistrar] Total registered {} tools", count);
    }
}

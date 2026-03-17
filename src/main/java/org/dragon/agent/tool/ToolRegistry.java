package org.dragon.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具注册中心
 * 负责管理所有可用工具连接器的注册和获取
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ToolRegistry {

    /**
     * 工具注册表
     */
    private final Map<String, ToolConnector> connectors = new ConcurrentHashMap<>();

    /**
     * 注册工具
     *
     * @param connector 工具连接器
     */
    public void register(ToolConnector connector) {
        if (connector == null || connector.getName() == null) {
            throw new IllegalArgumentException("Connector or Connector name cannot be null");
        }

        connectors.put(connector.getName(), connector);
        log.info("[ToolRegistry] Registered tool: {} (type: {})",
                connector.getName(), connector.getType());
    }

    /**
     * 注销工具
     *
     * @param name 工具名称
     */
    public void unregister(String name) {
        ToolConnector removed = connectors.remove(name);
        if (removed != null) {
            log.info("[ToolRegistry] Unregistered tool: {}", name);
        }
    }

    /**
     * 获取工具
     *
     * @param name 工具名称
     * @return Optional 工具连接器
     */
    public Optional<ToolConnector> get(String name) {
        return Optional.ofNullable(connectors.get(name));
    }

    /**
     * 获取所有工具
     *
     * @return 工具列表
     */
    public List<ToolConnector> listAll() {
        return connectors.values().stream().collect(Collectors.toList());
    }

    /**
     * 根据类型获取工具
     *
     * @param type 工具类型
     * @return 工具列表
     */
    public List<ToolConnector> listByType(ToolConnector.ToolType type) {
        return connectors.values().stream()
                .filter(c -> c.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有工具 Schema
     *
     * @return Schema 列表
     */
    public List<ToolConnector.ToolSchema> listSchemas() {
        return connectors.values().stream()
                .map(ToolConnector::getSchema)
                .collect(Collectors.toList());
    }

    /**
     * 检查工具是否存在
     *
     * @param name 工具名称
     * @return 是否存在
     */
    public boolean exists(String name) {
        return connectors.containsKey(name);
    }

    /**
     * 获取注册表大小
     *
     * @return 工具数量
     */
    public int size() {
        return connectors.size();
    }
}

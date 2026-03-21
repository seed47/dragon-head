package org.dragon.workspace.built_ins;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.dragon.workspace.built_ins.character.hr.HrCharacterFactory;
import org.springframework.stereotype.Component;

/**
 * Built-in Character 工厂管理器
 * 统一管理所有内置 Character 的工厂
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class BuiltInCharacterFactory {

    private final HrCharacterFactory hrCharacterFactory;

    /**
     * 内置 Character 工厂映射 (type -> factory)
     */
    private final Map<String, HrCharacterFactory> factoryMap = new ConcurrentHashMap<>();

    public BuiltInCharacterFactory(HrCharacterFactory hrCharacterFactory) {
        this.hrCharacterFactory = hrCharacterFactory;
        // 注册内置 Character 工厂
        registerFactory(hrCharacterFactory.getCharacterType(), hrCharacterFactory);
    }

    /**
     * 注册 Character 工厂
     *
     * @param type Character 类型
     * @param factory 工厂实例
     */
    public void registerFactory(String type, HrCharacterFactory factory) {
        factoryMap.put(type, factory);
    }

    /**
     * 获取指定类型的工厂
     *
     * @param type Character 类型
     * @return 工厂实例
     */
    public Optional<HrCharacterFactory> getFactory(String type) {
        return Optional.ofNullable(factoryMap.get(type));
    }

    /**
     * 获取 HR Character 工厂
     *
     * @return HR Character 工厂
     */
    public HrCharacterFactory getHrCharacterFactory() {
        return hrCharacterFactory;
    }

    /**
     * 获取所有已注册的工厂类型
     *
     * @return 类型列表
     */
    public List<String> getRegisteredTypes() {
        return List.copyOf(factoryMap.keySet());
    }

    /**
     * 检查是否支持指定类型
     *
     * @param type Character 类型
     * @return 是否支持
     */
    public boolean isSupported(String type) {
        return factoryMap.containsKey(type);
    }
}

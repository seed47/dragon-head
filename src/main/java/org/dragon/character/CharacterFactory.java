package org.dragon.character;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;

/**
 * Character 工厂接口
 * 定义创建和管理 Character 实例的通用规范
 *
 * @param <T> Character 子类类型
 * @author wyj
 * @version 1.0
 */
public interface CharacterFactory<T extends Character> {

    /**
     * 获取工厂创建的 Character 类型标识
     *
     * @return Character 类型名称
     */
    String getCharacterType();

    /**
     * 创建 Character 实例
     *
     * @param workspaceId Workspace ID
     * @return 创建的 Character 实例
     */
    T createCharacter(String workspaceId);

    /**
     * 获取或创建 Character 实例
     * 如果已存在则返回现有实例，否则创建新实例
     *
     * @param workspaceId Workspace ID
     * @return Character 实例
     */
    T getOrCreateCharacter(String workspaceId);

    /**
     * 检查指定 Workspace 是否已有该类型的 Character
     *
     * @param workspaceId Workspace ID
     * @return 是否存在
     */
    boolean hasCharacter(String workspaceId);

    /**
     * 获取该类型 Character 可用的工具列表
     *
     * @return 工具列表
     */
    List<ToolConnector> getAvailableTools();
}

package org.dragon.character.mind.memory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记忆访问接口
 * 提供对 Character 私有记忆的统一访问
 *
 * @author wyj
 * @version 1.0
 */
public interface MemoryAccess {

    /**
     * 获取 Character ID
     *
     * @return Character ID
     */
    String getCharacterId();

    /**
     * 语义检索
     * 基于向量相似度搜索记忆
     *
     * @param query 查询文本
     * @param topK  返回前 K 条结果
     * @return 匹配的记忆列表
     */
    List<Memory> semanticSearch(String query, int topK);

    /**
     * 结构化检索
     * 按类型和时间范围检索记忆
     *
     * @param type  记忆类型
     * @param from  开始时间
     * @param to    结束时间
     * @return 匹配的记忆列表
     */
    List<Memory> getByType(Memory.MemoryType type, LocalDateTime from, LocalDateTime to);

    /**
     * 存储记忆
     *
     * @param memory 记忆
     */
    void store(Memory memory);

    /**
     * 批量存储记忆
     *
     * @param memories 记忆列表
     */
    void storeBatch(List<Memory> memories);

    /**
     * 获取记忆
     *
     * @param memoryId 记忆 ID
     * @return 记忆
     */
    Memory get(String memoryId);

    /**
     * 删除记忆
     *
     * @param memoryId 记忆 ID
     */
    void delete(String memoryId);

    /**
     * 清空所有记忆
     */
    void clear();

    /**
     * 获取记忆数量
     *
     * @return 记忆数量
     */
    int count();
}

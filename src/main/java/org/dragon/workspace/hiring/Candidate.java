package org.dragon.workspace.hiring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Candidate 候选人抽象
 * 代表可以被雇佣执行任务的 Character
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {

    /**
     * 候选人类型
     */
    public enum Type {
        CHARACTER
    }

    /**
     * 候选人唯一标识
     */
    private String id;

    /**
     * 候选人类型
     */
    private Type type;

    /**
     * 候选人名称
     */
    private String name;

    /**
     * 候选人描述/简介
     */
    private String description;

    /**
     * 能力标签列表
     */
    private java.util.List<String> capabilities;

    /**
     * 历史表现评分（0-100）
     */
    private Integer historicalScore;

    /**
     * 当前负载（0-100）
     */
    private Integer currentLoad;

    /**
     * 匹配分数（0-100）
     */
    private Integer matchScore;
}

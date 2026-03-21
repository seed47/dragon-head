package org.dragon.workspace.hiring;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HiringRecord 雇佣记录实体
 * 一次雇佣过程的完整记录，包括需求、筛选过程、被雇佣者、录用决策等
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HiringRecord {

    /**
     * 录用决策
     */
    public enum Decision {
        ACCEPTED,   // 接受
        REJECTED,   // 拒绝
        PENDING     // 待定
    }

    /**
     * 候选人类型
     */
    public enum CandidateType {
        CHARACTER
    }

    /**
     * 雇佣记录唯一标识
     */
    private String id;

    /**
     * 雇佣请求 ID
     */
    private String hiringRequestId;

    /**
     * 候选人 ID
     */
    private String candidateId;

    /**
     * 候选人类型（CHARACTER）
     */
    private CandidateType candidateType;

    /**
     * 录用决策
     */
    private Decision decision;

    /**
     * 决策理由
     */
    private String reason;

    /**
     * 匹配分数（0-100）
     */
    private Integer matchScore;

    /**
     * 录用时间
     */
    private LocalDateTime hiredAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

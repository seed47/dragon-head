package org.dragon.tools.policy;

import java.util.*;

/**
 * 工具策略类型定义
 *
 * <p>
 * 定义工具策略、配置文件 ID 等核心类型。
 * </p>
 */
public final class ToolPolicyTypes {

    private ToolPolicyTypes() {
    }

    /**
     * 工具策略：允许列表和/或拒绝列表。
     */
    public record ToolPolicy(List<String> allow, List<String> deny) {
        public ToolPolicy {
            allow = allow != null ? List.copyOf(allow) : null;
            deny = deny != null ? List.copyOf(deny) : null;
        }

        public ToolPolicy() {
            this((List<String>) null, null);
        }
    }

    /**
     * 工具配置文件 ID。
     */
    public enum ToolProfileId {
        /** 最小配置 - 仅会话状态 */
        minimal,
        /** 编码配置 - 文件系统、运行时、会话、内存 */
        coding,
        /** 消息配置 - 消息和会话工具 */
        messaging,
        /** 完整配置 - 所有工具 */
        full
    }
}

package org.dragon.channel.enums;

/**
 * Description: 动作类型枚举
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/15 22:04
 * Update Date Time:
 */
public enum ActionType {
    // 发送
    SEND("发送"),
    // 回复
    REPLY("回复");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

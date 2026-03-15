package org.dragon.channel.entity;

import lombok.Data;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/14 23:39
 * Update Date Time:
 */
@Data
public class ActionMessage {
    // channel信息
    private String channelName;
    // 消息类型
    private ActionType actionType;// 动作类型枚举
    // 发送专属字段
    private String receiveId;      // 发给谁（chat_id 或 open_id）
    private String receiveType;    // 目标类型（group 或 p2p）
    private String chatType;      //
    // 引用回复专属字段
    private String quoteMessageId;// 如果是 REPLY，这里填入要引用的原消息ID
    // 通用字段
    private String messageType;   // 消息类型
    private String content;       // 要发送的文本内容
    private MentionConfig mentionConfig; // 需要 @ 某人，填入对方的 open_id
}

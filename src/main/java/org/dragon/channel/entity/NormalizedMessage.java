package org.dragon.channel.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Description: 客观发生的事实上下文,记录发生了什么
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:07
 * Update Date Time:
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class NormalizedMessage {
    // ================= 1. 身份与路由 (Identity & Routing) =================
    private String channel;     // 来源渠道，如 "telegram", "wechat"
    private String senderId;    // 发送者全局唯一标识
    private String chatId;      // 会话ID
    private String chatType;    // 会话类型
    private String messageId;   // 原始消息 ID (用于引用回复)
    // ================= 2. 基础载荷 (Payload) =================
    private String textContent;             // 清洗后的纯文本/语音转录文本
    private List<NormalizedFile> normalizedFiles;   // 附件列表 (图片、文件等)
    // ================= 3. 元数据信封 (Metadata) =================
    private Map<String, Object> metadata;   // 包含时间戳、原始环境信息等
}

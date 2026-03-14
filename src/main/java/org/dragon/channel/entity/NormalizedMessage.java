package org.dragon.channel.entity;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:07
 * Update Date Time:
 *
 */
@Data
public class NormalizedMessage {
    // ================= 1. 身份与路由 (Identity & Routing) =================
    private final String channel;     // 来源渠道，如 "telegram", "wechat"
    private final String senderId;    // 发送者全局唯一标识
    private final String messageId;   // 原始消息 ID (用于引用回复)
    private final String threadId;    // 线程/群组子话题 ID (可选)

    // ================= 2. 基础载荷 (Payload) =================
    private final String textContent;             // 清洗后的纯文本/语音转录文本
    private final List<Attachment> attachments;   // 附件列表 (图片、文件等)

    // ================= 3. 元数据信封 (Metadata) =================
    private final Map<String, Object> metadata;   // 包含时间戳、原始环境信息等

    // 私有构造函数，强制使用 Builder 创建
    private NormalizedMessage(Builder builder) {
        this.channel = builder.channel;
        this.senderId = builder.senderId;
        this.messageId = builder.messageId;
        this.threadId = builder.threadId;
        this.textContent = builder.textContent;
        // 使用 Collections.unmodifiableList 保证集合不可变，绝对线程安全
        this.attachments = builder.attachments != null ? Collections.unmodifiableList(builder.attachments) : Collections.emptyList();
        this.metadata = builder.metadata != null ? Collections.unmodifiableMap(builder.metadata) : Collections.emptyMap();
    }

    // ================= 静态建造者 (Builder) =================
    public static class Builder {
        private String channel;
        private String senderId;
        private String messageId;
        private String threadId;
        private String textContent;
        private List<Attachment> attachments;
        private Map<String, Object> metadata;

        public Builder channel(String channel) { this.channel = channel; return this; }
        public Builder senderId(String senderId) { this.senderId = senderId; return this; }
        public Builder messageId(String messageId) { this.messageId = messageId; return this; }
        public Builder threadId(String threadId) { this.threadId = threadId; return this; }
        public Builder textContent(String textContent) { this.textContent = textContent; return this; }
        public Builder attachments(List<Attachment> attachments) { this.attachments = attachments; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public NormalizedMessage build() {
            // 这里可以加上强制校验：核心路由字段不能为空
            if (channel == null || senderId == null) {
                throw new IllegalStateException("Channel 和 SenderId 不能为空！");
            }
            return new NormalizedMessage(this);
        }
    }
}

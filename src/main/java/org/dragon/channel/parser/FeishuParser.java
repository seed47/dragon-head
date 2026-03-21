package org.dragon.channel.parser;

import com.google.gson.JsonParser;
import com.lark.oapi.service.im.v1.model.*;
import org.apache.commons.lang3.StringUtils;
import org.dragon.channel.FileManager;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.entity.NormalizedFile;
import org.dragon.channel.entity.MentionConfig;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.channel.enums.FileSource;
import org.dragon.channel.file.FeishuImStorageAdapter;
import org.dragon.util.GsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/14 16:27
 * Update Date Time:
 */
@Component
public class FeishuParser {

    @Value("${channel.feishu.robotOpenId}")
    private String robotOpenId;

    @Autowired
    private FeishuImStorageAdapter feishuImStorageAdapter;

    public NormalizedMessage parseInbound(P2MessageReceiveV1 p2MessageReceiveV1, String channelName) {
        EventMessage message = p2MessageReceiveV1.getEvent().getMessage();
        // 获取发送者的 OpenID
        String senderId = p2MessageReceiveV1.getEvent().getSender().getSenderId().getOpenId();
        // 会话特征
        String chatId = message.getChatId();
        String chatType = message.getChatType(); // 通常群聊是 "group"，私聊是 "p2p"
        // 消息内容
        switch (message.getMessageType()) {
            case "text" -> {
                // 飞书的文本内容是包裹在 JSON 字符串里的，例如: {"text":"你好"}
                String rawContent = message.getContent();
                String textContent = JsonParser.parseString(rawContent).getAsJsonObject().get("text").getAsString();
                // 去掉多余空格
                textContent = textContent.trim();
                MentionEvent[] mentions = message.getMentions();
                if (mentions != null && mentions.length > 0) {
                    for (MentionEvent mention : mentions) {
                        String key = mention.getKey();
                        String name = mention.getName();
                        String openId = mention.getId().getOpenId();
                        if (StringUtils.equals(openId, robotOpenId)) {
                            // 如果是@机器人自己，直接从文本中剥离
                            textContent = textContent.replace(key, "");
                        } else {
                            // 如果是@其他人，将占位符替换为真实的用户名
                            textContent = textContent.replace(key, "@" + name);
                        }
                    }
                }
                // 组装系统标准的 NormalizedMessage
                NormalizedMessage normalizedMessage = new NormalizedMessage();
                normalizedMessage.setChannel(channelName);
                normalizedMessage.setSenderId(senderId);
                normalizedMessage.setChatId(chatId);
                normalizedMessage.setChatType(chatType);
                normalizedMessage.setMessageId(message.getMessageId());
                normalizedMessage.setTextContent(textContent);
                return normalizedMessage;
            }
            case "image" -> {
                // 组装系统标准的 NormalizedMessage
                NormalizedMessage normalizedMessageImg = new NormalizedMessage();
                normalizedMessageImg.setChannel(channelName);
                normalizedMessageImg.setSenderId(senderId);
                normalizedMessageImg.setChatId(chatId);
                normalizedMessageImg.setChatType(chatType);
                normalizedMessageImg.setMessageId(message.getMessageId());
                normalizedMessageImg.setTextContent("[图片]");
                NormalizedFile normalizedFileImg = new NormalizedFile();
                normalizedFileImg.setFileSource(FileSource.FEISHU_IM);
                normalizedFileImg.setFileUri(null);
                normalizedFileImg.setFileKey(JsonParser.parseString(message.getContent()).getAsJsonObject().get("image_key").getAsString());
                normalizedFileImg.setFileName(null);
                normalizedFileImg.setMimeType("image");
                normalizedFileImg.setFileSize(null);
                normalizedFileImg.setMessageId(message.getMessageId());
                try {
                    feishuImStorageAdapter.download(normalizedFileImg);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return normalizedMessageImg;
            }
            case "audio" -> {
                // 组装系统标准的 NormalizedMessage
                NormalizedMessage normalizedMessageAudio = new NormalizedMessage();
                normalizedMessageAudio.setChannel(channelName);
                normalizedMessageAudio.setSenderId(senderId);
                normalizedMessageAudio.setChatId(chatId);
                normalizedMessageAudio.setChatType(chatType);
                normalizedMessageAudio.setMessageId(message.getMessageId());
                normalizedMessageAudio.setTextContent("[语音]");
                NormalizedFile normalizedFileImg = new NormalizedFile();
                normalizedFileImg.setFileSource(FileSource.FEISHU_IM);
                normalizedFileImg.setFileUri(null);
                normalizedFileImg.setFileKey(JsonParser.parseString(message.getContent()).getAsJsonObject().get("image_key").getAsString());
                normalizedFileImg.setFileName(null);
                normalizedFileImg.setMimeType("audio");
                normalizedFileImg.setFileSize(JsonParser.parseString(message.getContent()).getAsJsonObject().get("duration").getAsLong());
                normalizedFileImg.setMessageId(message.getMessageId());
                try {
                    feishuImStorageAdapter.download(normalizedFileImg);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return normalizedMessageAudio;
            }
            case "file" -> {
                // 组装系统标准的 NormalizedMessage
                NormalizedMessage normalizedMessageFile = new NormalizedMessage();
                normalizedMessageFile.setChannel(channelName);
                normalizedMessageFile.setSenderId(senderId);
                normalizedMessageFile.setChatId(chatId);
                normalizedMessageFile.setChatType(chatType);
                normalizedMessageFile.setMessageId(message.getMessageId());
                normalizedMessageFile.setTextContent("[文件]");
                NormalizedFile normalizedFileImg = new NormalizedFile();
                normalizedFileImg.setFileSource(FileSource.FEISHU_IM);
                normalizedFileImg.setFileUri(null);
                normalizedFileImg.setFileKey(JsonParser.parseString(message.getContent()).getAsJsonObject().get("image_key").getAsString());
                normalizedFileImg.setFileName(JsonParser.parseString(message.getContent()).getAsJsonObject().get("file_name").getAsString());
                normalizedFileImg.setMimeType("file");
                normalizedFileImg.setFileSize(null);
                normalizedFileImg.setMessageId(message.getMessageId());
                try {
                    feishuImStorageAdapter.download(normalizedFileImg);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return normalizedMessageFile;
            }
            default -> {
                // 组装系统标准的 NormalizedMessage
                NormalizedMessage normalizedMessageAudioNotSupported = new NormalizedMessage();
                normalizedMessageAudioNotSupported.setChannel(channelName);
                normalizedMessageAudioNotSupported.setSenderId(senderId);
                normalizedMessageAudioNotSupported.setChatId(chatId);
                normalizedMessageAudioNotSupported.setChatType(chatType);
                normalizedMessageAudioNotSupported.setMessageId(message.getMessageId());
                normalizedMessageAudioNotSupported.setTextContent("[不支持的消息类型]");
                return normalizedMessageAudioNotSupported;
            }
        }
    }

    public ReplyMessageReq parseOutboundReplyMsg(ActionMessage message) {
        String quoteMessageId = message.getQuoteMessageId();
        String messageType = message.getMessageType();
        String content = message.getContent();
        MentionConfig mentionConfig = message.getMentionConfig();
        // 提醒信息增强
        if (mentionConfig != null) {
            content = String.format("<at user_id=\"%s\"></at> %s", mentionConfig.getMentionOpenId(), content);
        }
        // 消息转为json格式
        String jsonContent = GsonUtils.toJson(Map.of("text", content));
        return ReplyMessageReq.newBuilder()
                .messageId(quoteMessageId)
                .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                        .msgType(messageType)
                        .content(jsonContent)
                        .build())
                .build();
    }


    public CreateMessageReq parseOutboundCreateMsg(ActionMessage message) {
        String receiveId = message.getReceiveId();
        String receiveType = message.getReceiveType();
        String chatType = message.getChatType();
        String messageType = message.getMessageType();
        String content = message.getContent();
        MentionConfig mentionConfig = message.getMentionConfig();
        // 发送消息场景下，只有群组情况下，提醒@配置生效
        if (StringUtils.equals(chatType, "group") && mentionConfig != null) {
            content = String.format("<at user_id=\"%s\"></at> %s", mentionConfig.getMentionOpenId(), content);
        }
        String jsonContent = GsonUtils.toJson(Map.of("text", content));

        return CreateMessageReq.newBuilder()
                .receiveIdType(receiveType) // 动态指定类型
                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                        .receiveId(receiveId) // 动态指定目标 ID
                        .msgType(messageType)
                        .content(jsonContent)
                        .build())
                .build();
    }

}

package org.dragon.channel.adapter;

import com.lark.oapi.Client;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.enums.ActionType;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.channel.parser.FeishuParser;
import org.dragon.gateway.Gateway;
import org.dragon.util.GsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:45
 * Update Date Time:
 */
@Component
@Slf4j
public class FeishuChannelAdaptor implements ChannelAdapter{
    @Value("${channel.feishu.appId}")
    private String appId;
    @Value("${channel.feishu.appSecret}")
    private String appSecret;
    @Value("${channel.feishu.whitelist}")
    private List<String> whitelist; // 允许私信的用户 open_id 列表，逗号分隔
    @Value("${channel.feishu.wakeWord}")
    private String wakeWord; // 唤醒词，例如 "@Bot" 或 "小助手"
    @Value("${channel.feishu.robotOpenId}")
    private String robotOpenId;

    private com.lark.oapi.ws.Client wsClient; // 长连接客户端 (收)
    private Client apiClient;  // API客户端 (发)
    private Gateway gateway;

    @Autowired
    private FeishuParser feishuParser;

    @Override
    public String getChannelName() {
        return "Feishu";
    }

    @Override
    public void startListening(Gateway gateway) {
        this.gateway = gateway;
        // 1. 初始化发消息的 API Client
        this.apiClient = Client.newBuilder(appId, appSecret).build();
        // 2. 配置长连接的事件分发器
        EventDispatcher eventDispatcher = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                        processFeishuMessage(event);
                    }
                })
                .build();
        // 3. 启动长连接客户端
        this.wsClient = new com.lark.oapi.ws.Client.Builder(appId, appSecret)
                .eventHandler(eventDispatcher)
                .build();
        this.wsClient.start();
        log.info("[Feishu]长连接已建立，正在监听飞书消息...");
    }

    private void processFeishuMessage(P2MessageReceiveV1 event) {
        try {
            EventMessage message = event.getEvent().getMessage();
            String chatType = message.getChatType();
            String openId = event.getEvent().getSender().getSenderId().getOpenId();
            // 飞书消息体 content 是一个 JSON 字符串，例如 {"text":"hello"}
            String content = message.getContent();
            // 1.私信陌生人拦截 (白名单机制)
            if (StringUtils.equals("p2p", chatType)) {
                if (CollectionUtils.isNotEmpty(whitelist) && !whitelist.contains(openId)) {
                    log.warn("[Feishu]拦截非白名单私信, openId: {}", openId);
                    sendRejectReply(event);
                    return; // 直接拦截，不向下分发
                }
            }
            // 2.群聊静默过滤 (判断 @ 或唤醒词)
            if (StringUtils.equals("group", chatType)) {
                boolean isMentionMe = isMentioned(message);
                boolean hasWakeWord = StringUtils.isNotEmpty(wakeWord) && content.contains(wakeWord);

                if (!isMentionMe && !hasWakeWord) {
                    log.info("[Feishu]群聊消息未触发唤醒条件，忽略。messageId: {}", message.getMessageId());
                    return; // 忽略该消息
                }
            }
            log.info("[Feishu]接收原始消息:{}", GsonUtils.toJson(event));
            NormalizedMessage normalizedMessage = feishuParser.parseInbound(event, getChannelName());
            gateway.dispatch(normalizedMessage);
        } catch (Exception e) {
            log.error("[Feishu]解析消息失败: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Void> sendMessage(ActionMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (message.getActionType() == ActionType.SEND) {
                    CreateMessageReq createMessageReq = feishuParser.parseOutboundCreateMsg(message);
                    CreateMessageResp createMessageResp = apiClient.im().message().create(createMessageReq);
                    if (!createMessageResp.success()) {
                        throw new RuntimeException("飞书发送消息失败" + createMessageResp.getMsg());
                    }
                } else if (message.getActionType() == ActionType.REPLY) {
                    ReplyMessageReq replyMessageReq = feishuParser.parseOutboundReplyMsg(message);
                    ReplyMessageResp replyMessageResp = apiClient.im().message().reply(replyMessageReq);
                    if (!replyMessageResp.success()) {
                        throw new RuntimeException("飞书发送回复失败: " + replyMessageResp.getMsg());
                    }
                }
                log.info("[Feishu]消息成功推送给用户");
            } catch (Exception e) {
                log.error("[Feishu]异步发送异常: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private void sendRejectReply(P2MessageReceiveV1 event) {
        try {
            String content = String.format("您的openId: %s 不在白名单中，请联系庄昊哲配置", event.getEvent().getSender().getSenderId().getOpenId());
            String textContent = String.format("{\"text\":\"%s\"}", content);
            ReplyMessageReq req = ReplyMessageReq.newBuilder()
                    .messageId(event.getEvent().getMessage().getMessageId())
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .content(textContent)
                            .msgType("text")
                            .build())
                    .build();
            ReplyMessageResp resp = apiClient.im().message().reply(req);
            if (!resp.success()) {
                log.error("[Feishu]发送拦截提示失败: {}", resp.getMsg());
            }
        } catch (Exception e) {
            log.error("[Feishu]发送拦截提示异常: ", e);
        }
    }

    private boolean isMentioned(EventMessage message) {
        MentionEvent[] mentions = message.getMentions();
        if (mentions != null && mentions.length > 0) {
            return Arrays.stream(mentions).anyMatch(mentionEvent -> StringUtils.equals(mentionEvent.getId().getOpenId(), robotOpenId));
        }
        return false;
    }

    @Override
    public void stop() {
        // empty
    }

    @Override
    public boolean isHealthy() {
        // 检查客户端是否已初始化并启动
        return wsClient != null;
    }

    @Override
    public void restart() {
        // empty
    }

}

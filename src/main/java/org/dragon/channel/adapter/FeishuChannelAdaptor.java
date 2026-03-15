package org.dragon.channel.adapter;

import com.lark.oapi.Client;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.*;
import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.entity.ActionType;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.channel.parser.FeishuParser;
import org.dragon.gateway.Gateway;
import org.dragon.util.GsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    @Override
    public void stop() {
        // empty
    }

    @Override
    public boolean isHealthy() {
        // empty
        return true;
    }

    @Override
    public void restart() {
        // empty
    }

}

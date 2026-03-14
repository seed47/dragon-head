package org.dragon.channel.adapter;


import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.*;
import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.gateway.Gateway;
import org.dragon.util.GsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:45
 * Update Date Time:
 *
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

    // 处理飞书原始消息，清洗并提交给网关
    private void processFeishuMessage(P2MessageReceiveV1 event) {
        try {
            log.info("[Feishu]收到原始消息:{}", GsonUtils.toJson(event));
            EventMessage message = event.getEvent().getMessage();
            // 获取发送者的 OpenID
            String senderId = event.getEvent().getSender().getSenderId().getOpenId();
            // 飞书的文本内容是包裹在 JSON 字符串里的，例如: {"text":"你好"}
            String rawContent = message.getContent();
            String textContent = JsonParser.parseString(rawContent).getAsJsonObject().get("text").getAsString();
            // 组装系统标准的 NormalizedMessage
            NormalizedMessage normalizedMsg = new NormalizedMessage.Builder()
                    .channel(getChannelName())
                    .senderId(senderId)
                    .messageId(message.getMessageId())
                    .textContent(textContent)
                    .build();
            // 提交给gateway
            log.info("[feishu]提交到gateway：{}", GsonUtils.toJson(normalizedMsg));
            gateway.dispatch(normalizedMsg);
        } catch (Exception e) {
            log.error("[Feishu]解析消息失败: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Void> sendMessage(String targetUserId, NormalizedMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 飞书要求发送的内容也是 JSON 字符串
                String jsonContent = GsonUtils.toJson(Map.of("text", GsonUtils.toJson(message)));
                CreateMessageReq req = CreateMessageReq.newBuilder()
                        .receiveIdType("open_id") // 指定按 open_id 发送
                        .createMessageReqBody(CreateMessageReqBody.newBuilder()
                                .receiveId(targetUserId)
                                .msgType("text")
                                .content(jsonContent)
                                .build())
                        .build();

                // 调用 API 发送
                CreateMessageResp resp = apiClient.im().message().create(req);

                if (!resp.success()) {
                    throw new RuntimeException("飞书发送失败: " + resp.getMsg());
                }
                log.info("[Feishu]消息已成功推回给用户: " + targetUserId);

            } catch (Exception e) {
                log.error("[Feishu]异步发送异常: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isHealthy() {
        // 由于飞书 SDK 内部有重连机制，简单判断对象是否存活即可
        // 生产环境中可以通过定时发探针消息来验证
        return wsClient != null;
    }

    @Override
    public void restart() {
    }

}

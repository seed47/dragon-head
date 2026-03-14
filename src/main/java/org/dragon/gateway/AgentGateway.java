package org.dragon.gateway;

import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.ChannelManager;
import org.dragon.channel.entity.NormalizedMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:14
 * Update Date Time:
 *
 */
@Component
@Slf4j
public class AgentGateway implements Gateway {

    @Autowired
    @Lazy
    private ChannelManager channelManager;

    @Override
    public void dispatch(NormalizedMessage inboundMsg) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1.模拟大模型思考的过程
                String aiReplyText = mockCallLlmBrain(inboundMsg.getTextContent());
                // 2.组装返回内容
                NormalizedMessage outboundMsg = new NormalizedMessage.Builder()
                        .channel(inboundMsg.getChannel())
                        .senderId(inboundMsg.getSenderId())
                        .textContent(aiReplyText)
                        .build();
                // 3.发送消息
                channelManager.routeMessageOutbound(inboundMsg.getChannel(), inboundMsg.getSenderId(), outboundMsg);
            } catch (Exception e) {
                log.error("[gateway] 返回消息失败");
            }
        });
    }


    private String mockCallLlmBrain(String content) {
        try {
            // 模拟大模型思考了 2 秒钟
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}
        return content;
    }

}

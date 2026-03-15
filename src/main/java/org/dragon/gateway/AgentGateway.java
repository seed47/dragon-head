package org.dragon.gateway;

import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.ChannelManager;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.entity.ActionType;
import org.dragon.channel.entity.MentionConfig;
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
                ActionMessage actionMessage = mockCallLlmBrain(inboundMsg);
                // 2.返回消息
                channelManager.routeMessageOutbound(actionMessage);
            } catch (Exception e) {
                log.error("[gateway] 返回消息失败");
            }
        });
    }


    private ActionMessage mockCallLlmBrain(NormalizedMessage inboundMsg) {
        ActionMessage actionMessage = new ActionMessage();
        actionMessage.setChannelName("Feishu");
        actionMessage.setActionType(ActionType.REPLY);
        actionMessage.setQuoteMessageId(inboundMsg.getMessageId());
        actionMessage.setMessageType("text");
        actionMessage.setContent("你刚刚给我发了 '" + inboundMsg.getTextContent() +"' 我选择不回复你");
        MentionConfig mentionConfig = new MentionConfig();
        mentionConfig.setMentionOpenId(inboundMsg.getSenderId());
        actionMessage.setMentionConfig(mentionConfig);
        return actionMessage;
    }

}

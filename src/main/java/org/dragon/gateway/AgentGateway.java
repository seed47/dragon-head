package org.dragon.gateway;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.dragon.channel.ChannelManager;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.entity.ActionType;
import org.dragon.channel.entity.MentionConfig;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.WorkspaceApplicationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Gateway 实现
 * 只负责渠道协议转换，将业务执行委托给 WorkspaceService
 *
 * @author zhz
 * @version 1.0
 */
@Component
@Slf4j
public class AgentGateway implements Gateway {

    @Autowired
    @Lazy
    private ChannelManager channelManager;

    @Autowired
    private CharacterRegistry characterRegistry;

    @Autowired
    private WorkspaceApplicationProvider workspaceApplicationProvider;

    @Override
    public void dispatch(NormalizedMessage inboundMsg) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 获取默认 Character
                Optional<org.dragon.character.Character> characterOpt = characterRegistry.getDefaultCharacter();

                if (characterOpt.isEmpty()) {
                    log.warn("[Gateway] No default character configured");
                    // Fallback to mock
                    ActionMessage actionMessage = buildActionMessage(inboundMsg, "你刚刚给我发了 '" + inboundMsg.getTextContent() +"' 我选择不回复你");
                    channelManager.routeMessageOutbound(actionMessage);
                    return;
                }

                // 2. 通过 WorkspaceApplicationProvider 执行任务（统一入口）
                String characterId = characterOpt.get().getId();
                String result = workspaceApplicationProvider.executeInstantTask(characterId, inboundMsg.getTextContent());

                // 3. 返回消息
                ActionMessage actionMessage = buildActionMessage(inboundMsg, result);
                channelManager.routeMessageOutbound(actionMessage);

            } catch (Exception e) {
                log.error("[Gateway] Execution failed", e);
                ActionMessage actionMessage = buildActionMessage(inboundMsg, "处理失败: " + e.getMessage());
                channelManager.routeMessageOutbound(actionMessage);
            }
        });
    }

    /**
     * 构建回复消息
     *
     * @param inboundMsg 收到的消息
     * @param content 回复内容
     * @return 回复消息
     */
    private ActionMessage buildActionMessage(NormalizedMessage inboundMsg, String content) {
        ActionMessage actionMessage = new ActionMessage();
        actionMessage.setChannelName(inboundMsg.getChannel());
        actionMessage.setActionType(ActionType.REPLY);
        actionMessage.setQuoteMessageId(inboundMsg.getMessageId());
        actionMessage.setMessageType("text");
        actionMessage.setContent(content);

        MentionConfig mentionConfig = new MentionConfig();
        mentionConfig.setMentionOpenId(inboundMsg.getSenderId());
        actionMessage.setMentionConfig(mentionConfig);

        return actionMessage;
    }
}

package org.dragon.channel.adapter;

import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.gateway.Gateway;

import java.util.concurrent.CompletableFuture;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:09
 * Update Date Time:
 *
 */
public interface ChannelAdapter {
    // 获取channel名称
    String getChannelName();
    // 启动监听消息
    void startListening(Gateway gateway) throws Exception;
    // 异步发送消息
    CompletableFuture<Void> sendMessage(String targetUserId, NormalizedMessage message);
    // 关闭channel
    void stop();
    // 连接探活
    boolean isHealthy();
    // channel重连
    void restart() throws Exception;
}

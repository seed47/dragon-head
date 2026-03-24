package org.dragon.tools.builtin;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * Agent 到 Agent (A2A) 消息流程 — ping-pong 对话 + 公告。
 * 对应 TypeScript sessions-send-tool.a2a.ts。
 *
 * <p>
 * 这在初始 sessions_send 完成后异步运行。
 * 它在请求者和目标会话之间协调多轮 ping-pong 对话，
 * 然后向目标频道公告最终结果。
 * </p>
 */
@Slf4j
public class SessionsSendA2ATool {

    /**
     * 运行 A2A 流程：等待初始回复，执行 ping-pong 轮次，然后公告。
     * 当前为存根实现 — 需要通过网关进行 agent.wait + agent 步骤执行。
     */
    public static CompletableFuture<Void> runA2AFlow(A2AFlowParams params) {
        return CompletableFuture.runAsync(() -> {
            try {
                doRunA2AFlow(params);
            } catch (Exception e) {
                log.warn("sessions_send A2A flow failed: runId={}", params.waitRunId, e);
            }
        });
    }

    private static void doRunA2AFlow(A2AFlowParams params) {
        String primaryReply = params.roundOneReply;
        String latestReply = params.roundOneReply;

        if (primaryReply == null && params.waitRunId != null) {
            // TODO: call gateway agent.wait to get the initial reply
            log.debug("A2A flow: waiting for initial reply, runId={}", params.waitRunId);
            return;
        }

        if (latestReply == null) {
            log.debug("A2A flow: no reply available, skipping");
            return;
        }

        // Resolve announce target from session key
        SessionsSendHelpers.AnnounceTarget announceTarget = SessionsSendHelpers
                .resolveAnnounceTargetFromKey(params.targetSessionKey);
        if (announceTarget == null) {
            announceTarget = SessionsSendHelpers.resolveAnnounceTargetFromKey(params.displayKey);
        }
        String targetChannel = announceTarget != null ? announceTarget.channel() : "unknown";

        // Ping-pong turns
        if (params.maxPingPongTurns > 0
                && params.requesterSessionKey != null
                && !params.requesterSessionKey.equals(params.targetSessionKey)) {

            for (int turn = 1; turn <= params.maxPingPongTurns; turn++) {
                // TODO: implement actual ping-pong via runAgentStep
                log.debug("A2A ping-pong turn {}/{}: (stubbed)", turn, params.maxPingPongTurns);
                break; // stub: exit after logging
            }
        }

        // Announce step — build context for future gateway integration
        log.debug("A2A announce: target={}, channel={}", params.displayKey, targetChannel);
        // TODO: call runAgentStep for announce, then deliver via gateway send
    }

    /**
     * A2A 流程的参数。
     */
    public record A2AFlowParams(
            String targetSessionKey,
            String displayKey,
            String message,
            long announceTimeoutMs,
            int maxPingPongTurns,
            String requesterSessionKey,
            String requesterChannel,
            String roundOneReply,
            String waitRunId) {
    }
}

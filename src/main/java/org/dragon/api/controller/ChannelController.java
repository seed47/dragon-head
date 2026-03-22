package org.dragon.api.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.channel.ChannelManager;
import org.dragon.channel.entity.ChannelBinding;
import org.dragon.channel.entity.ChannelConfig;
import org.dragon.channel.service.ChannelBindingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ChannelController 渠道管理 API
 *
 * <p>职责范围：
 * <ul>
 *   <li>ChannelConfig（Bot 凭证）的 CRUD 及热重载</li>
 *   <li>ChannelBinding（IM 会话 → Workspace 绑定）的 CRUD</li>
 *   <li>绑定关系的启用/禁用</li>
 *   <li>路由解析调试接口</li>
 * </ul>
 *
 * <p>典型使用流程：
 * <ol>
 *   <li>POST /api/channels/configs         创建飞书 Bot 的 appId/appSecret</li>
 *   <li>POST /api/channels/configs/{id}/reload  热重载，无需重启服务</li>
 *   <li>POST /api/channels/bindings        将某个飞书群绑定到目标 Workspace</li>
 * </ol>
 *
 * @author zhz
 * @version 1.0
 */
@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelBindingService channelBindingService;
    private final ChannelManager channelManager;

    // ==================== ChannelConfig（Bot 凭证）管理 ====================

    /**
     * 新增渠道 Bot 配置
     * POST /api/channels/configs
     *
     * <p>Body 示例（飞书）：
     * <pre>
     * {
     *   "id": "feishu-main-bot",
     *   "channelType": "Feishu",
     *   "name": "主飞书机器人",
     *   "credentials": {
     *     "appId": "cli_xxx",
     *     "appSecret": "xxx",
     *     "robotOpenId": "ou_xxx",
     *     "wakeWord": "小助手"
     *   }
     * }
     * </pre>
     */
    @PostMapping("/configs")
    public ResponseEntity<ChannelConfig> createChannelConfig(@RequestBody ChannelConfig config) {
        ChannelConfig created = channelBindingService.createChannelConfig(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 查询所有渠道 Bot 配置
     * GET /api/channels/configs
     * GET /api/channels/configs?channelType=Feishu
     */
    @GetMapping("/configs")
    public ResponseEntity<List<ChannelConfig>> listChannelConfigs(
            @RequestParam(required = false) String channelType) {
        List<ChannelConfig> configs = channelBindingService.listChannelConfigs(channelType);
        return ResponseEntity.ok(configs);
    }

    /**
     * 查询指定渠道 Bot 配置
     * GET /api/channels/configs/{configId}
     */
    @GetMapping("/configs/{configId}")
    public ResponseEntity<ChannelConfig> getChannelConfig(@PathVariable String configId) {
        return channelBindingService.getChannelConfig(configId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新渠道 Bot 配置（如修改 appSecret）
     * PUT /api/channels/configs/{configId}
     */
    @PutMapping("/configs/{configId}")
    public ResponseEntity<ChannelConfig> updateChannelConfig(
            @PathVariable String configId,
            @RequestBody ChannelConfig config) {
        config.setId(configId);
        ChannelConfig updated = channelBindingService.updateChannelConfig(config);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除渠道 Bot 配置
     * DELETE /api/channels/configs/{configId}
     */
    @DeleteMapping("/configs/{configId}")
    public ResponseEntity<Void> deleteChannelConfig(@PathVariable String configId) {
        channelBindingService.deleteChannelConfig(configId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 热重载渠道配置（修改凭证后立即生效，无需重启服务）
     * POST /api/channels/configs/{configId}/reload
     *
     * <p>内部流程：stop 旧连接 → 应用新凭证 → start 新连接
     */
    @PostMapping("/configs/{configId}/reload")
    public ResponseEntity<Void> reloadChannelConfig(@PathVariable String configId) {
        ChannelConfig config = channelBindingService.getChannelConfig(configId)
                .orElseThrow(() -> new IllegalArgumentException("ChannelConfig not found: " + configId));
        channelManager.reloadChannelConfig(config);
        return ResponseEntity.ok().build();
    }

    // ==================== ChannelBinding（会话 → Workspace 绑定）管理 ====================

    /**
     * 新增绑定关系：将 IM 会话绑定到指定 Workspace
     * POST /api/channels/bindings
     *
     * <p>Body 示例：
     * <pre>
     * {
     *   "channelName": "Feishu",
     *   "chatId": "oc_a1b2c3d4",
     *   "chatType": "group",
     *   "workspaceId": "ws-dev-team",
     *   "description": "研发团队飞书群"
     * }
     * </pre>
     */
    @PostMapping("/bindings")
    public ResponseEntity<ChannelBinding> createBinding(@RequestBody CreateBindingRequest body) {
        ChannelBinding binding = channelBindingService.createBinding(
                body.getChannelName(),
                body.getChatId(),
                body.getChatType(),
                body.getWorkspaceId(),
                body.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(binding);
    }

    /**
     * 查询所有绑定关系
     * GET /api/channels/bindings
     * GET /api/channels/bindings?workspaceId=ws-001
     * GET /api/channels/bindings?channelName=Feishu
     */
    @GetMapping("/bindings")
    public ResponseEntity<List<ChannelBinding>> listBindings(
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String channelName) {
        List<ChannelBinding> bindings;
        if (workspaceId != null) {
            bindings = channelBindingService.listBindingsByWorkspace(workspaceId);
        } else if (channelName != null) {
            bindings = channelBindingService.listBindingsByChannel(channelName);
        } else {
            bindings = channelBindingService.listAllBindings();
        }
        return ResponseEntity.ok(bindings);
    }

    /**
     * 查询指定绑定关系
     * GET /api/channels/bindings/{channelName}/{chatId}
     */
    @GetMapping("/bindings/{channelName}/{chatId}")
    public ResponseEntity<ChannelBinding> getBinding(
            @PathVariable String channelName,
            @PathVariable String chatId) {
        return channelBindingService.getBinding(channelName, chatId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 改绑：将某个会话从原 Workspace 切换到新 Workspace
     * PATCH /api/channels/bindings/{channelName}/{chatId}/workspace
     * Body: { "workspaceId": "ws-new-team" }
     */
    @PatchMapping("/bindings/{channelName}/{chatId}/workspace")
    public ResponseEntity<ChannelBinding> updateBindingWorkspace(
            @PathVariable String channelName,
            @PathVariable String chatId,
            @RequestBody Map<String, String> body) {
        String newWorkspaceId = body.get("workspaceId");
        if (newWorkspaceId == null || newWorkspaceId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ChannelBinding updated = channelBindingService.updateBinding(channelName, chatId, newWorkspaceId);
        return ResponseEntity.ok(updated);
    }

    /**
     * 启用/禁用绑定关系（禁用后该会话的消息走 Fallback 路径）
     * PATCH /api/channels/bindings/{channelName}/{chatId}/status
     * Body: { "enabled": false }
     */
    @PatchMapping("/bindings/{channelName}/{chatId}/status")
    public ResponseEntity<Void> setBindingEnabled(
            @PathVariable String channelName,
            @PathVariable String chatId,
            @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().build();
        }
        channelBindingService.setBindingEnabled(channelName, chatId, enabled);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除绑定关系
     * DELETE /api/channels/bindings/{channelName}/{chatId}
     */
    @DeleteMapping("/bindings/{channelName}/{chatId}")
    public ResponseEntity<Void> deleteBinding(
            @PathVariable String channelName,
            @PathVariable String chatId) {
        channelBindingService.deleteBinding(channelName, chatId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 路由调试 ====================

    /**
     * 调试接口：查询 (channelName, chatId) 会路由到哪个 Workspace
     * GET /api/channels/resolve?channelName=Feishu&chatId=oc_xxx
     *
     * <p>可在配置绑定后验证路由是否生效。
     */
    @GetMapping("/resolve")
    public ResponseEntity<Map<String, String>> resolveWorkspace(
            @RequestParam String channelName,
            @RequestParam String chatId) {
        Optional<String> workspaceId = channelBindingService.resolveWorkspaceId(channelName, chatId);
        if (workspaceId.isPresent()) {
            java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
            result.put("channelName", channelName);
            result.put("chatId", chatId);
            result.put("workspaceId", workspaceId.get());
            result.put("routed", "true");
            return ResponseEntity.ok(result);
        }
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        result.put("channelName", channelName);
        result.put("chatId", chatId);
        result.put("routed", "false");
        result.put("message", "No active binding found, will fallback to single character");
        return ResponseEntity.ok(result);
    }

    // ==================== 请求体 DTO ====================

    @Data
    public static class CreateBindingRequest {
        /** 渠道名称，如 "Feishu" */
        private String channelName;
        /** IM 内的会话 ID，群聊为 chatId，私聊为 senderId */
        private String chatId;
        /** 会话类型："group" 或 "p2p" */
        private String chatType;
        /** 目标 Workspace ID */
        private String workspaceId;
        /** 描述（可选） */
        private String description;
    }
}
package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.channel.ChannelManager;
import org.dragon.channel.entity.ChannelBinding;
import org.dragon.channel.entity.ChannelConfig;
import org.dragon.channel.service.ChannelBindingService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private final ChannelBindingService channelBindingService;
    @Autowired
    private final ChannelManager channelManager;

    // ==================== ChannelConfig（Bot 凭证）管理 ====================
    @Operation(summary = "新增Channel配置")
    @PostMapping("/configs")
    public ResponseEntity<ChannelConfig> createChannelConfig(@RequestBody ChannelConfig config) {
        ChannelConfig created = channelBindingService.createChannelConfig(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "查询所有Channel配置")
    @GetMapping("/configs")
    public ResponseEntity<List<ChannelConfig>> listChannelConfigs(
            @RequestParam(required = false) String channelType) {
        List<ChannelConfig> configs = channelBindingService.listChannelConfigs(channelType);
        return ResponseEntity.ok(configs);
    }

    @Operation(summary = "查询指定Channel配置")
    @GetMapping("/configs/{configId}")
    public ResponseEntity<ChannelConfig> getChannelConfig(@PathVariable String configId) {
        return channelBindingService.getChannelConfig(configId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "更新Channel配置")
    @PutMapping("/configs/{configId}")
    public ResponseEntity<ChannelConfig> updateChannelConfig(
            @PathVariable String configId,
            @RequestBody ChannelConfig config) {
        config.setId(configId);
        ChannelConfig updated = channelBindingService.updateChannelConfig(config);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "删除Channel配置")
    @DeleteMapping("/configs/{configId}")
    public ResponseEntity<Void> deleteChannelConfig(@PathVariable String configId) {
        channelBindingService.deleteChannelConfig(configId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "重新加载Channel配置")
    @PostMapping("/configs/{configId}/reload")
    public ResponseEntity<Void> reloadChannelConfig(@PathVariable String configId) {
        ChannelConfig config = channelBindingService.getChannelConfig(configId)
                .orElseThrow(() -> new IllegalArgumentException("ChannelConfig not found: " + configId));
        channelManager.reloadChannelConfig(config);
        return ResponseEntity.ok().build();
    }

    // ==================== ChannelBinding（会话 → Workspace 绑定）管理 ====================

    @Operation(summary = "新增Channel->Workspace的绑定关系")
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

    @Operation(summary = "查询所有Channel->Workspace绑定关系")
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

    @Operation(summary = "更新绑定关系，根据Channel查询指定Workspace绑定关系")
    @GetMapping("/bindings/{channelName}/{chatId}")
    public ResponseEntity<ChannelBinding> getBinding(
            @PathVariable String channelName,
            @PathVariable String chatId) {
        return channelBindingService.getBinding(channelName, chatId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "更新绑定关系")
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

    @Operation(summary = "启用/禁用绑定关系（禁用后该会话的消息走 Fallback 路径）")
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

    @Operation(summary = "删除绑定关系")
    @DeleteMapping("/bindings/{channelName}/{chatId}")
    public ResponseEntity<Void> deleteBinding(
            @PathVariable String channelName,
            @PathVariable String chatId) {
        channelBindingService.deleteBinding(channelName, chatId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 路由调试 ====================

    @Operation(summary = "调试接口：查询 (channelName, chatId) 会路由到哪个 Workspace")
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
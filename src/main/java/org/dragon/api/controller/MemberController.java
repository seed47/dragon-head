package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.workspace.hiring.HireMode;
import org.dragon.workspace.member.CharacterDuty;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.WorkspaceHiringService;
import org.dragon.workspace.service.WorkspaceMemberManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MemberController 工作空间成员管理 API
 *
 * <p>职责范围：
 * <ul>
 *   <li>成员查询与属性变更（角色/标签/权重/优先级/声誉）</li>
 *   <li>雇佣与解雇（DEFAULT / MANUAL / AUTO 三种模式）</li>
 *   <li>Character 职责描述的设置与查询</li>
 * </ul>
 *
 * <p>URL 设计：所有接口都挂在 /api/workspaces/{workspaceId}/members 下，
 * 与 WorkspaceController 形成父子关系，语义清晰。
 *
 * @author zhz
 * @version 1.0
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/members")
@RequiredArgsConstructor
public class MemberController {

    @Autowired
    private final WorkspaceMemberManagementService memberManagementService;
    @Autowired
    private final WorkspaceHiringService workspaceHiringService;

    // ==================== 成员查询 ====================

    @Operation(summary = "查询工作空间的所有成员")
    @GetMapping
    public ResponseEntity<List<WorkspaceMember>> listMembers(@PathVariable String workspaceId) {
        return ResponseEntity.ok(memberManagementService.listMembers(workspaceId));
    }

    @Operation(summary = "查询工作空间下的指定成员")
    @GetMapping("/{characterId}")
    public ResponseEntity<WorkspaceMember> getMember(
            @PathVariable String workspaceId,
            @PathVariable String characterId) {
        return memberManagementService.getMember(workspaceId, characterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== 成员属性变更 ====================

    @Operation(summary = "更新成员角色")
    @PatchMapping("/{characterId}/role")
    public ResponseEntity<Void> updateRole(
            @PathVariable String workspaceId,
            @PathVariable String characterId,
            @RequestBody RoleRequest body) {
        memberManagementService.updateMemberRole(workspaceId, characterId, body.getRole());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "更新成员标签")
    @PutMapping("/{characterId}/tags")
    public ResponseEntity<Void> updateTags(
            @PathVariable String workspaceId,
            @PathVariable String characterId,
            @RequestBody List<String> tags) {
        memberManagementService.updateMemberTags(workspaceId, characterId, tags);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "更新成员调度权重")
    @PatchMapping("/{characterId}/weight")
    public ResponseEntity<Void> updateWeight(
            @PathVariable String workspaceId,
            @PathVariable String characterId,
            @RequestBody WeightRequest body) {
        memberManagementService.updateMemberWeight(workspaceId, characterId, body.getWeight());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "更新成员调度优先级")
    @PatchMapping("/{characterId}/priority")
    public ResponseEntity<Void> updatePriority(
            @PathVariable String workspaceId,
            @PathVariable String characterId,
            @RequestBody PriorityRequest body) {
        memberManagementService.updateMemberPriority(workspaceId, characterId, body.getPriority());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "调整成员声誉积分（正值加分，负值扣分）")
    @PatchMapping("/{characterId}/reputation")
    public ResponseEntity<Void> updateReputation(
            @PathVariable String workspaceId,
            @PathVariable String characterId,
            @RequestBody ReputationRequest body) {
        memberManagementService.updateMemberReputation(workspaceId, characterId, body.getChange());
        return ResponseEntity.ok().build();
    }

    // ==================== 雇佣与解雇 ====================

    @Operation(summary = "雇佣Character到工作空间")
    @PostMapping("/hire")
    public ResponseEntity<Void> hire(
            @PathVariable String workspaceId,
            @RequestBody HireRequest body) {
        workspaceHiringService.hire(workspaceId, body.getCharacterId(), body.getMode());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "解雇Character")
    @PostMapping("/{characterId}/fire")
    public ResponseEntity<Void> fire(
            @PathVariable String workspaceId,
            @PathVariable String characterId,
            @RequestBody FireRequest body) {
        workspaceHiringService.fire(workspaceId, characterId, body.getMode());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "直接移除成员（不走雇佣审批流程）")
    @DeleteMapping("/{characterId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String workspaceId,
            @PathVariable String characterId) {
        memberManagementService.removeMember(workspaceId, characterId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 职责管理 ====================

    @Operation(summary = "查询Character在工作空间中的职责描述")
    @GetMapping("/{characterId}/duty")
    public ResponseEntity<CharacterDuty> getDuty(
            @PathVariable String workspaceId,
            @PathVariable String characterId) {
        return workspaceHiringService.getCharacterDuty(workspaceId, characterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "设置Character的职责描述（新增或覆盖）")
    @PutMapping("/{characterId}/duty")
    public ResponseEntity<Void> setDuty(
            @PathVariable String workspaceId,
            @PathVariable String characterId,
            @RequestBody DutyRequest body) {
        workspaceHiringService.setCharacterDuty(workspaceId, characterId, body.getDescription());
        return ResponseEntity.ok().build();
    }

    // ==================== 内部请求体 DTO ====================

    @Data
    public static class HireRequest {
        private String characterId;
        private HireMode mode;
    }

    @Data
    public static class FireRequest {
        private HireMode mode;
    }

    @Data
    public static class DutyRequest {
        private String description;
    }

    @Data
    public static class RoleRequest {
        private String role;
    }

    @Data
    public static class WeightRequest {
        private double weight;
    }

    @Data
    public static class PriorityRequest {
        private int priority;
    }

    @Data
    public static class ReputationRequest {
        /** 积分变化量，正值加分，负值扣分 */
        private int change;
    }
}

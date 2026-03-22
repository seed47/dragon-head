package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.actionlog.WorkspaceActionLog;
import org.dragon.workspace.material.Material;
import org.dragon.workspace.service.WorkspaceActionLogService;
import org.dragon.workspace.service.WorkspaceLifecycleService;
import org.dragon.workspace.service.WorkspaceMaterialService;
import org.dragon.workspace.service.WorkspaceTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * WorkspaceController 工作空间管理 API
 *
 * <p>职责范围：
 * <ul>
 *   <li>Workspace 生命周期（创建/更新/删除/状态变更）</li>
 *   <li>Workspace 任务监控（查询/取消）</li>
 *   <li>Workspace 行为日志查询</li>
 *   <li>Workspace 物料管理（上传/下载/删除）</li>
 * </ul>
 *
 * @author zhz
 * @version 1.0
 */
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    @Autowired
    private final WorkspaceLifecycleService workspaceLifecycleService;
    @Autowired
    private final WorkspaceTaskService workspaceTaskService;
    @Autowired
    private final WorkspaceActionLogService workspaceActionLogService;
    @Autowired
    private final WorkspaceMaterialService workspaceMaterialService;

    // ==================== Workspace 生命周期 ====================

    @Operation(summary = "创建工作空间")
    @PostMapping
    public ResponseEntity<Workspace> createWorkspace(@RequestBody Workspace workspace) {
        Workspace created = workspaceLifecycleService.createWorkspace(workspace);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "获取所有工作空间")
    @GetMapping
    public ResponseEntity<List<Workspace>> listWorkspaces(
            @RequestParam(required = false) Workspace.Status status) {
        List<Workspace> list = (status != null)
                ? workspaceLifecycleService.listWorkspacesByStatus(status)
                : workspaceLifecycleService.listWorkspaces();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "获取指定工作空间")
    @GetMapping("/{workspaceId}")
    public ResponseEntity<Workspace> getWorkspace(@PathVariable String workspaceId) {
        return workspaceLifecycleService.getWorkspace(workspaceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "更新工作空间")
    @PutMapping("/{workspaceId}")
    public ResponseEntity<Workspace> updateWorkspace(
            @PathVariable String workspaceId,
            @RequestBody Workspace workspace) {
        workspace.setId(workspaceId);
        Workspace updated = workspaceLifecycleService.updateWorkspace(workspace);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "删除工作空间")
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable String workspaceId) {
        workspaceLifecycleService.deleteWorkspace(workspaceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "激活工作空间")
    @PostMapping("/{workspaceId}/activate")
    public ResponseEntity<Void> activateWorkspace(@PathVariable String workspaceId) {
        workspaceLifecycleService.activateWorkspace(workspaceId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "停用工作空间")
    @PostMapping("/{workspaceId}/deactivate")
    public ResponseEntity<Void> deactivateWorkspace(@PathVariable String workspaceId) {
        workspaceLifecycleService.deactivateWorkspace(workspaceId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "归档工作空间")
    @PostMapping("/{workspaceId}/archive")
    public ResponseEntity<Void> archiveWorkspace(@PathVariable String workspaceId) {
        workspaceLifecycleService.archiveWorkspace(workspaceId);
        return ResponseEntity.ok().build();
    }

    // ==================== 任务监控 ====================

    @Operation(summary = "查询工作空间的所有任务")
    @GetMapping("/{workspaceId}/tasks")
    public ResponseEntity<List<Task>> listTasks(
            @PathVariable String workspaceId,
            @RequestParam(required = false) TaskStatus status) {
        List<Task> tasks = (status != null)
                ? workspaceTaskService.listTasksByStatus(workspaceId, status)
                : workspaceTaskService.listTasks(workspaceId);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "查询指定任务详情")
    @GetMapping("/{workspaceId}/tasks/{taskId}")
    public ResponseEntity<Task> getTask(
            @PathVariable String workspaceId,
            @PathVariable String taskId) {
        return workspaceTaskService.getTask(workspaceId, taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "查询任务执行结果")
    @GetMapping("/{workspaceId}/tasks/{taskId}/result")
    public ResponseEntity<String> getTaskResult(
            @PathVariable String workspaceId,
            @PathVariable String taskId) {
        String result = workspaceTaskService.getTaskResult(workspaceId, taskId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "取消任务")
    @PostMapping("/{workspaceId}/tasks/{taskId}/cancel")
    public ResponseEntity<Task> cancelTask(
            @PathVariable String workspaceId,
            @PathVariable String taskId) {
        Task task = workspaceTaskService.cancelTask(workspaceId, taskId);
        return ResponseEntity.ok(task);
    }

    // ==================== 行为日志查询 ====================

    @Operation(summary = "查询工作空间的所有行为日志")
    @GetMapping("/{workspaceId}/logs")
    public ResponseEntity<List<WorkspaceActionLog>> getActionLogs(
            @PathVariable String workspaceId,
            @RequestParam(required = false) String characterId,
            @RequestParam(required = false) WorkspaceActionLog.ActionType actionType) {
        List<WorkspaceActionLog> logs;
        if (characterId != null) {
            logs = workspaceActionLogService.getActionLogsByCharacter(workspaceId, characterId);
        } else if (actionType != null) {
            logs = workspaceActionLogService.getActionLogsByType(workspaceId, actionType);
        } else {
            logs = workspaceActionLogService.getActionLogs(workspaceId);
        }
        return ResponseEntity.ok(logs);
    }

    // ==================== 物料管理 ====================

    @Operation(summary = "查询工作空间的所有物料")
    @GetMapping("/{workspaceId}/materials")
    public ResponseEntity<List<Material>> listMaterials(@PathVariable String workspaceId) {
        List<Material> materials = workspaceMaterialService.listByWorkspace(workspaceId);
        return ResponseEntity.ok(materials);
    }

    @Operation(summary = "上传物料")
    @PostMapping(value = "/{workspaceId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Material> uploadMaterial(
            @PathVariable String workspaceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploader", defaultValue = "admin") String uploader) throws IOException {
        Material material = workspaceMaterialService.upload(
                workspaceId,
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                uploader);
        return ResponseEntity.status(HttpStatus.CREATED).body(material);
    }

    @Operation(summary = "获取物料元数据")
    @GetMapping("/{workspaceId}/materials/{materialId}")
    public ResponseEntity<Material> getMaterial(
            @PathVariable String workspaceId,
            @PathVariable String materialId) {
        return workspaceMaterialService.get(materialId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "下载物料")
    @GetMapping("/{workspaceId}/materials/{materialId}/download")
    public ResponseEntity<InputStreamResource> downloadMaterial(
            @PathVariable String workspaceId,
            @PathVariable String materialId) {
        Material material = workspaceMaterialService.get(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        InputStream inputStream = workspaceMaterialService.download(materialId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + material.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    @Operation(summary = "删除物料")
    @DeleteMapping("/{workspaceId}/materials/{materialId}")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable String workspaceId,
            @PathVariable String materialId) {
        workspaceMaterialService.delete(materialId);
        return ResponseEntity.noContent().build();
    }
}
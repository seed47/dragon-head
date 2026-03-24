package org.dragon.workspace.service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dragon.task.Task;
import org.dragon.task.TaskStore;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.material.Material;
import org.dragon.workspace.material.MaterialParser;
import org.dragon.workspace.material.MaterialStore;
import org.dragon.workspace.material.MaterialStorage;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceMaterialService 物料管理服务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMaterialService {

    private final MaterialStore materialStore;
    private final MaterialStorage materialStorage;
    private final WorkspaceRegistry workspaceRegistry;
    private final TaskStore taskStore;
    private final MaterialParser materialParser;

    /**
     * 上传物料
     *
     * @param workspaceId 工作空间 ID
     * @param inputStream 输入流
     * @param filename 文件名
     * @param size 文件大小
     * @param contentType 内容类型
     * @param uploader 上传者 ID
     * @return 物料
     */
    public Material upload(String workspaceId, InputStream inputStream, String filename,
                          long size, String contentType, String uploader) {
        // 验证工作空间存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 存储文件
        String storageKey = materialStorage.store(workspaceId, inputStream, filename);

        // 创建物料元数据
        Material material = Material.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .name(filename)
                .size(size)
                .type(contentType)
                .storageKey(storageKey)
                .uploader(uploader)
                .uploadedAt(LocalDateTime.now())
                .build();

        materialStore.save(material);
        log.info("[WorkspaceMaterialService] Uploaded material: {} to workspace: {}", material.getId(), workspaceId);

        return material;
    }

    /**
     * 下载物料
     *
     * @param materialId 物料 ID
     * @return 输入流
     */
    public InputStream download(String materialId) {
        Material material = materialStore.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));

        InputStream inputStream = materialStorage.retrieve(material.getStorageKey());
        if (inputStream == null) {
            throw new IllegalStateException("Material content not found: " + materialId);
        }

        return inputStream;
    }

    /**
     * 获取物料元数据
     *
     * @param materialId 物料 ID
     * @return 物料
     */
    public Optional<Material> get(String materialId) {
        return materialStore.findById(materialId);
    }

    /**
     * 删除物料
     *
     * @param materialId 物料 ID
     */
    public void delete(String materialId) {
        Material material = materialStore.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));

        // 删除存储的内容
        materialStorage.delete(material.getStorageKey());

        // 删除元数据
        materialStore.delete(materialId);
        log.info("[WorkspaceMaterialService] Deleted material: {}", materialId);
    }

    /**
     * 获取工作空间的所有物料
     *
     * @param workspaceId 工作空间 ID
     * @return 物料列表
     */
    public java.util.List<Material> listByWorkspace(String workspaceId) {
        return materialStore.findByWorkspaceId(workspaceId);
    }

    // ==================== 任务关联方法 ====================

    /**
     * 上传并关联到任务
     *
     * @param workspaceId Workspace ID
     * @param inputStream 输入流
     * @param filename 文件名
     * @param size 文件大小
     * @param contentType 内容类型
     * @param uploader 上传者 ID
     * @param taskId 关联的任务 ID（可选）
     * @return 物料
     */
    public Material uploadAndAttachToTask(String workspaceId, InputStream inputStream,
            String filename, long size, String contentType, String uploader, String taskId) {
        // 上传物料
        Material material = upload(workspaceId, inputStream, filename, size, contentType, uploader);

        // 如果有关联的任务，附加解析结果
        if (taskId != null && !taskId.isEmpty()) {
            attachToTask(taskId, material);
        }

        return material;
    }

    /**
     * 附加物料到任务
     *
     * @param taskId 任务 ID
     * @param material 物料
     */
    public void attachToTask(String taskId, Material material) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 添加物料 ID
        if (task.getMaterialIds() == null) {
            task.setMaterialIds(new java.util.ArrayList<>());
        }
        if (!task.getMaterialIds().contains(material.getId())) {
            task.getMaterialIds().add(material.getId());
            taskStore.update(task);
        }

        // 解析物料并存储结果
        try {
            InputStream inputStream = download(material.getId());
            MaterialParser.ParseResult parseResult = materialParser.parse(material, inputStream);

            // 将解析结果存储到 task metadata
            if (task.getMetadata() == null) {
                task.setMetadata(new HashMap<>());
            }

            // 存储解析结果
            @SuppressWarnings("unchecked")
            Map<String, Object> materialResults = (Map<String, Object>) task.getMetadata()
                    .getOrDefault("materialResults", new HashMap<String, Object>());
            materialResults.put(material.getId(), parseResult);
            task.getMetadata().put("materialResults", materialResults);

            // 如果解析成功，追加文本内容到 task input
            if (parseResult.isSuccess() && parseResult.getTextContent() != null) {
                Object currentInput = task.getInput();
                String newContent = "\n\n[Material: " + material.getName() + "]\n" + parseResult.getTextContent();
                task.setInput(currentInput != null ? currentInput.toString() + newContent : newContent);
            }

            taskStore.update(task);
            log.info("[WorkspaceMaterialService] Attached material {} to task {}", material.getId(), taskId);

        } catch (Exception e) {
            log.error("[WorkspaceMaterialService] Failed to parse and attach material {} to task {}: {}",
                    material.getId(), taskId, e.getMessage());
        }
    }

    /**
     * 解析并关联多个物料到任务
     *
     * @param taskId 任务 ID
     * @param materialIds 物料 ID 列表
     */
    public void attachMaterialsToTask(String taskId, List<String> materialIds) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        for (String materialId : materialIds) {
            Optional<Material> materialOpt = get(materialId);
            if (materialOpt.isPresent()) {
                attachToTask(taskId, materialOpt.get());
            }
        }
    }

    /**
     * 获取任务的物料列表
     *
     * @param taskId 任务 ID
     * @return 物料列表
     */
    public List<Material> getTaskMaterials(String taskId) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getMaterialIds() == null || task.getMaterialIds().isEmpty()) {
            return List.of();
        }

        return task.getMaterialIds().stream()
                .map(materialStore::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * 解析任务的物料
     *
     * @param taskId 任务 ID
     * @return 解析结果映射
     */
    public Map<String, MaterialParser.ParseResult> parseTaskMaterials(String taskId) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getMetadata() == null || !task.getMetadata().containsKey("materialResults")) {
            return Map.of();
        }

        @SuppressWarnings("unchecked")
        Map<String, MaterialParser.ParseResult> results =
                (Map<String, MaterialParser.ParseResult>) task.getMetadata().get("materialResults");
        return results;
    }
}

package org.dragon.workspace.service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.material.Material;
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
}

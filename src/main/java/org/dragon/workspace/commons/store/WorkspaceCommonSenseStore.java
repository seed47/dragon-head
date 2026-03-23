package org.dragon.workspace.commons.store;

import java.util.List;
import java.util.Optional;

import org.dragon.workspace.commons.CommonSense;
import org.dragon.workspace.commons.CommonSenseFolder;

/**
 * WorkspaceCommonSenseStore 工作空间常识存储接口
 * 管理 Workspace 下的 CommonSense 和 CommonSenseFolder
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkspaceCommonSenseStore {

    // ==================== 文件夹管理 ====================

    /**
     * 保存文件夹
     */
    CommonSenseFolder saveFolder(CommonSenseFolder folder);

    /**
     * 根据 ID 查询文件夹
     */
    Optional<CommonSenseFolder> findFolderById(String id);

    /**
     * 查询工作空间的所有根文件夹
     */
    List<CommonSenseFolder> findRootFolders(String workspaceId);

    /**
     * 查询工作空间的所有文件夹
     */
    List<CommonSenseFolder> findFoldersByWorkspace(String workspaceId);

    /**
     * 查询子文件夹
     */
    List<CommonSenseFolder> findChildFolders(String parentId);

    /**
     * 删除文件夹
     */
    boolean deleteFolder(String id);

    // ==================== 常识管理 ====================

    /**
     * 保存常识
     */
    CommonSense save(CommonSense commonSense);

    /**
     * 根据 ID 查询常识
     */
    Optional<CommonSense> findById(String id);

    /**
     * 查询工作空间的所有常识
     */
    List<CommonSense> findByWorkspace(String workspaceId);

    /**
     * 查询文件夹下的所有常识
     */
    List<CommonSense> findByFolder(String folderId);

    /**
     * 查询工作空间所有启用的常识
     */
    List<CommonSense> findEnabled(String workspaceId);

    /**
     * 查询工作空间指定类别的常识
     */
    List<CommonSense> findByWorkspaceAndCategory(String workspaceId, CommonSense.Category category);

    /**
     * 查询所有常识
     */
    List<CommonSense> findAll();

    /**
     * 删除常识
     */
    boolean delete(String id);

    /**
     * 查询工作空间常识数量
     */
    int countByWorkspace(String workspaceId);

    /**
     * 清空工作空间的所有常识
     */
    void clearByWorkspace(String workspaceId);
}

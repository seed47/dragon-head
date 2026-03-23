package org.dragon.workspace.commons.store;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.dragon.workspace.commons.CommonSense;
import org.dragon.workspace.commons.CommonSenseFolder;
import org.dragon.workspace.commons.content.CommonSenseContent;
import org.dragon.workspace.commons.content.CommonSenseContentParser;
import org.dragon.workspace.commons.content.ConstraintContent;
import org.dragon.workspace.commons.content.ContentType;
import org.dragon.workspace.commons.content.ForbiddenContent;
import org.springframework.stereotype.Component;

/**
 * MemoryWorkspaceCommonSenseStore 工作空间常识内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryWorkspaceCommonSenseStore implements WorkspaceCommonSenseStore {

    /**
     * 全局工作空间 ID
     */
    public static final String GLOBAL_WORKSPACE_ID = "_global";

    /**
     * 文件夹存储
     * key: folderId
     */
    private final Map<String, CommonSenseFolder> folders = new ConcurrentHashMap<>();

    /**
     * 常识存储
     * key: commonSenseId
     */
    private final Map<String, CommonSense> commonSenses = new ConcurrentHashMap<>();

    /**
     * 默认构造函数，初始化全局常识
     */
    public MemoryWorkspaceCommonSenseStore() {
        initDefaultCommonSense();
    }

    /**
     * 初始化默认全局常识
     */
    private void initDefaultCommonSense() {
        LocalDateTime now = LocalDateTime.now();
        CommonSenseContentParser parser = new CommonSenseContentParser();

        // 隐私保护类 - FORBIDDEN 类型
        save(CommonSense.builder()
                .id("cs-privacy-001")
                .workspaceId(GLOBAL_WORKSPACE_ID)
                .name("用户隐私保护")
                .description("不得泄露用户隐私信息，包括但不限于姓名、联系方式、地址等")
                .category(CommonSense.Category.PRIVACY)
                .content(parser.serialize(CommonSenseContent.builder()
                        .type(ContentType.FORBIDDEN)
                        .data(ForbiddenContent.builder()
                                .items(List.of("泄露用户个人信息", "未经授权访问用户数据"))
                                .reason("隐私保护")
                                .build())
                        .build()))
                .severity(CommonSense.Severity.CRITICAL)
                .enabled(true)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .build());

        // 安全合规类 - CONSTRAINT 类型
        save(CommonSense.builder()
                .id("cs-safety-001")
                .workspaceId(GLOBAL_WORKSPACE_ID)
                .name("法律法规遵守")
                .description("所有任务必须遵循当地法律法规和道德规范")
                .category(CommonSense.Category.SAFETY)
                .content(parser.serialize(CommonSenseContent.builder()
                        .type(ContentType.CONSTRAINT)
                        .data(ConstraintContent.builder()
                                .constraints(List.of(
                                        ConstraintContent.Constraint.builder().key("合法合规").value(true).build(),
                                        ConstraintContent.Constraint.builder().key("道德规范").value(true).build()
                                ))
                                .build())
                        .build()))
                .severity(CommonSense.Severity.CRITICAL)
                .enabled(true)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .build());

        // 性能约束类 - CONSTRAINT 类型
        save(CommonSense.builder()
                .id("cs-performance-001")
                .workspaceId(GLOBAL_WORKSPACE_ID)
                .name("响应时间限制")
                .description("系统响应时间不得超过 30 秒")
                .category(CommonSense.Category.PERFORMANCE)
                .content(parser.serialize(CommonSenseContent.builder()
                        .type(ContentType.CONSTRAINT)
                        .data(ConstraintContent.builder()
                                .constraints(List.of(
                                        ConstraintContent.Constraint.builder()
                                                .key("maxResponseTime")
                                                .value(30000)
                                                .unit("ms")
                                                .build()
                                ))
                                .build())
                        .build()))
                .severity(CommonSense.Severity.HIGH)
                .enabled(true)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .build());

        // 业务规则类 - FORBIDDEN 类型
        save(CommonSense.builder()
                .id("cs-business-001")
                .workspaceId(GLOBAL_WORKSPACE_ID)
                .name("数据持久化要求")
                .description("核心业务数据必须持久化存储")
                .category(CommonSense.Category.BUSINESS)
                .content(parser.serialize(CommonSenseContent.builder()
                        .type(ContentType.FORBIDDEN)
                        .data(ForbiddenContent.builder()
                                .items(List.of("核心业务数据", "用户生成内容"))
                                .reason("数据持久化要求")
                                .build())
                        .build()))
                .severity(CommonSense.Severity.HIGH)
                .enabled(true)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .build());

        // 系统约束类 - SIMPLE 类型
        save(CommonSense.builder()
                .id("cs-system-001")
                .workspaceId(GLOBAL_WORKSPACE_ID)
                .name("资源使用限制")
                .description("单次任务 token 消耗不得超过 100000")
                .category(CommonSense.Category.SYSTEM)
                .content(parser.serialize(CommonSenseContent.builder()
                        .type(ContentType.SIMPLE)
                        .data(Map.of("maxTokens", 100000))
                        .build()))
                .severity(CommonSense.Severity.MEDIUM)
                .enabled(true)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .build());
    }

    // ==================== 文件夹管理 ====================

    @Override
    public CommonSenseFolder saveFolder(CommonSenseFolder folder) {
        if (folder == null || folder.getId() == null) {
            throw new IllegalArgumentException("Folder or Folder id cannot be null");
        }
        folders.put(folder.getId(), folder);
        return folder;
    }

    @Override
    public Optional<CommonSenseFolder> findFolderById(String id) {
        return Optional.ofNullable(folders.get(id));
    }

    @Override
    public List<CommonSenseFolder> findRootFolders(String workspaceId) {
        return folders.values().stream()
                .filter(f -> f.getWorkspaceId().equals(workspaceId))
                .filter(f -> f.getParentId() == null || f.getParentId().isEmpty())
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSenseFolder> findFoldersByWorkspace(String workspaceId) {
        return folders.values().stream()
                .filter(f -> f.getWorkspaceId().equals(workspaceId))
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSenseFolder> findChildFolders(String parentId) {
        return folders.values().stream()
                .filter(f -> parentId.equals(f.getParentId()))
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteFolder(String id) {
        // 删除文件夹及其所有子文件夹
        List<String> folderIdsToDelete = folders.values().stream()
                .filter(f -> f.getId().equals(id) ||
                        (f.getParentId() != null && f.getParentId().equals(id)))
                .map(CommonSenseFolder::getId)
                .collect(Collectors.toList());

        folderIdsToDelete.forEach(folders::remove);

        // 删除文件夹下的所有常识
        commonSenses.entrySet().removeIf(entry ->
                folderIdsToDelete.contains(entry.getValue().getFolderId()));

        return true;
    }

    // ==================== 常识管理 ====================

    @Override
    public CommonSense save(CommonSense commonSense) {
        if (commonSense == null || commonSense.getId() == null) {
            throw new IllegalArgumentException("CommonSense or CommonSense id cannot be null");
        }
        commonSenses.put(commonSense.getId(), commonSense);
        return commonSense;
    }

    @Override
    public Optional<CommonSense> findById(String id) {
        return Optional.ofNullable(commonSenses.get(id));
    }

    @Override
    public List<CommonSense> findByWorkspace(String workspaceId) {
        return commonSenses.values().stream()
                .filter(cs -> cs.getWorkspaceId().equals(workspaceId))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSense> findByFolder(String folderId) {
        return commonSenses.values().stream()
                .filter(cs -> folderId.equals(cs.getFolderId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSense> findEnabled(String workspaceId) {
        return commonSenses.values().stream()
                .filter(cs -> cs.getWorkspaceId().equals(workspaceId))
                .filter(CommonSense::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSense> findByWorkspaceAndCategory(String workspaceId, CommonSense.Category category) {
        return commonSenses.values().stream()
                .filter(cs -> cs.getWorkspaceId().equals(workspaceId))
                .filter(cs -> cs.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        return commonSenses.remove(id) != null;
    }

    @Override
    public List<CommonSense> findAll() {
        return new java.util.ArrayList<>(commonSenses.values());
    }

    @Override
    public int countByWorkspace(String workspaceId) {
        return (int) commonSenses.values().stream()
                .filter(cs -> cs.getWorkspaceId().equals(workspaceId))
                .count();
    }

    @Override
    public void clearByWorkspace(String workspaceId) {
        // 删除工作空间的所有常识
        commonSenses.entrySet().removeIf(entry ->
                entry.getValue().getWorkspaceId().equals(workspaceId));

        // 删除工作空间的所有文件夹
        folders.entrySet().removeIf(entry ->
                entry.getValue().getWorkspaceId().equals(workspaceId));
    }
}

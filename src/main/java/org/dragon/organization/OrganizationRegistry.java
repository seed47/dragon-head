package org.dragon.organization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Organization 注册中心
 * 负责管理所有 Organization 的生命周期
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class OrganizationRegistry {

    /**
     * Organization 注册表
     */
    private final Map<String, Organization> registry = new ConcurrentHashMap<>();

    /**
     * 默认 Organization ID
     */
    private volatile String defaultOrganizationId;

    /**
     * 注册 Organization
     *
     * @param organization Organization 实例
     * @return 注册后的 Organization（含 ID）
     */
    public Organization register(Organization organization) {
        if (organization == null) {
            throw new IllegalArgumentException("Organization cannot be null");
        }

        // 生成 ID 如果没有提供
        if (organization.getId() == null || organization.getId().isEmpty()) {
            organization.setId(UUID.randomUUID().toString());
        }

        // 设置创建/更新时间
        if (organization.getCreatedAt() == null) {
            organization.setCreatedAt(LocalDateTime.now());
        }
        organization.setUpdatedAt(LocalDateTime.now());

        // 如果是第一个 Organization，设为默认
        if (registry.isEmpty()) {
            defaultOrganizationId = organization.getId();
        }

        registry.put(organization.getId(), organization);
        log.info("[OrganizationRegistry] Registered organization: {}, version: {}",
                organization.getId(), organization.getVersion());

        return organization;
    }

    /**
     * 注销 Organization
     *
     * @param organizationId Organization ID
     */
    public void unregister(String organizationId) {
        Organization removed = registry.remove(organizationId);
        if (removed != null) {
            log.info("[OrganizationRegistry] Unregistered organization: {}", organizationId);

            // 如果删除的是默认 Organization，选择下一个
            if (defaultOrganizationId != null && defaultOrganizationId.equals(organizationId)) {
                defaultOrganizationId = registry.isEmpty() ? null : registry.keySet().iterator().next();
            }
        }
    }

    /**
     * 获取 Organization
     *
     * @param organizationId Organization ID
     * @return Optional Organization
     */
    public Optional<Organization> get(String organizationId) {
        return Optional.ofNullable(registry.get(organizationId));
    }

    /**
     * 获取默认 Organization
     *
     * @return Optional Organization
     */
    public Optional<Organization> getDefaultOrganization() {
        if (defaultOrganizationId == null) {
            return Optional.empty();
        }
        return get(defaultOrganizationId);
    }

    /**
     * 获取所有 Organization
     *
     * @return Organization 列表
     */
    public List<Organization> listAll() {
        return new CopyOnWriteArrayList<>(registry.values());
    }

    /**
     * 根据状态获取 Organization 列表
     *
     * @param status 组织状态
     * @return Organization 列表
     */
    public List<Organization> listByStatus(Organization.Status status) {
        return registry.values().stream()
                .filter(org -> org.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 更新 Organization
     *
     * @param organization Organization 实例
     */
    public void update(Organization organization) {
        if (organization == null || organization.getId() == null) {
            throw new IllegalArgumentException("Organization or Organization id cannot be null");
        }

        if (!registry.containsKey(organization.getId())) {
            throw new IllegalArgumentException("Organization not found: " + organization.getId());
        }

        organization.setUpdatedAt(LocalDateTime.now());
        organization.setVersion(organization.getVersion() + 1);
        registry.put(organization.getId(), organization);
        log.info("[OrganizationRegistry] Updated organization: {}", organization.getId());
    }

    /**
     * 设置默认 Organization
     *
     * @param organizationId Organization ID
     */
    public void setDefaultOrganization(String organizationId) {
        if (!registry.containsKey(organizationId)) {
            throw new IllegalArgumentException("Organization not found: " + organizationId);
        }
        defaultOrganizationId = organizationId;
        log.info("[OrganizationRegistry] Set default organization: {}", organizationId);
    }

    /**
     * 激活 Organization
     *
     * @param organizationId Organization ID
     */
    public void activate(String organizationId) {
        get(organizationId).ifPresent(organization -> {
            organization.setStatus(Organization.Status.ACTIVE);
            organization.setUpdatedAt(LocalDateTime.now());
            log.info("[OrganizationRegistry] Activated organization: {}", organizationId);
        });
    }

    /**
     * 停用 Organization
     *
     * @param organizationId Organization ID
     */
    public void deactivate(String organizationId) {
        get(organizationId).ifPresent(organization -> {
            organization.setStatus(Organization.Status.INACTIVE);
            organization.setUpdatedAt(LocalDateTime.now());
            log.info("[OrganizationRegistry] Deactivated organization: {}", organizationId);
        });
    }

    /**
     * 归档 Organization
     *
     * @param organizationId Organization ID
     */
    public void archive(String organizationId) {
        get(organizationId).ifPresent(organization -> {
            organization.setStatus(Organization.Status.ARCHIVED);
            organization.setUpdatedAt(LocalDateTime.now());
            log.info("[OrganizationRegistry] Archived organization: {}", organizationId);
        });
    }

    /**
     * 获取注册表大小
     *
     * @return 注册的 Organization 数量
     */
    public int size() {
        return registry.size();
    }

    /**
     * 检查 Organization 是否存在
     *
     * @param organizationId Organization ID
     * @return 是否存在
     */
    public boolean exists(String organizationId) {
        return registry.containsKey(organizationId);
    }

    /**
     * 根据名称查找 Organization
     *
     * @param name 组织名称
     * @return Optional Organization
     */
    public Optional<Organization> getByName(String name) {
        return registry.values().stream()
                .filter(org -> org.getName().equals(name))
                .findFirst();
    }
}

package org.dragon.sandbox;

import org.dragon.sandbox.SandboxTypes.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SandboxTypes 类型定义测试
 */
class SandboxTypesTest {

    @Test
    void testSandboxDockerConfig_Builder() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .image("test-image")
                .containerPrefix("test-")
                .workdir("/test")
                .readOnlyRoot(false)
                .tmpfs(List.of("/tmp"))
                .network("bridge")
                .user("testuser")
                .capDrop(List.of("NET_ADMIN"))
                .env(Map.of("KEY", "value"))
                .setupCommand("echo hello")
                .pidsLimit(100)
                .memory("512m")
                .memorySwap("1g")
                .cpus(2.0)
                .ulimits(Map.of("nofile", "1024:2048"))
                .seccompProfile("unconfined")
                .apparmorProfile("test-profile")
                .dns(List.of("8.8.8.8"))
                .extraHosts(List.of("host.docker.internal:host-gateway"))
                .binds(List.of("/host/path:/container/path"))
                .build();

        assertEquals("test-image", config.getImage());
        assertEquals("test-", config.getContainerPrefix());
        assertEquals("/test", config.getWorkdir());
        assertFalse(config.isReadOnlyRoot());
        assertEquals(List.of("/tmp"), config.getTmpfs());
        assertEquals("bridge", config.getNetwork());
        assertEquals("testuser", config.getUser());
        assertEquals(List.of("NET_ADMIN"), config.getCapDrop());
        assertEquals(Map.of("KEY", "value"), config.getEnv());
        assertEquals("echo hello", config.getSetupCommand());
        assertEquals(100, config.getPidsLimit());
        assertEquals("512m", config.getMemory());
        assertEquals("1g", config.getMemorySwap());
        assertEquals(2.0, config.getCpus());
        assertEquals(Map.of("nofile", "1024:2048"), config.getUlimits());
        assertEquals("unconfined", config.getSeccompProfile());
        assertEquals("test-profile", config.getApparmorProfile());
        assertEquals(List.of("8.8.8.8"), config.getDns());
        assertEquals(List.of("host.docker.internal:host-gateway"), config.getExtraHosts());
        assertEquals(List.of("/host/path:/container/path"), config.getBinds());
    }

    @Test
    void testSandboxDockerConfig_DefaultValues() {
        SandboxDockerConfig config = new SandboxDockerConfig();

        assertTrue(config.isReadOnlyRoot());
        assertEquals(List.of("/tmp", "/var/tmp", "/run"), config.getTmpfs());
        assertEquals("none", config.getNetwork());
        assertEquals(List.of("ALL"), config.getCapDrop());
    }

    @Test
    void testSandboxToolPolicy_Builder() {
        SandboxToolPolicy policy = SandboxToolPolicy.builder()
                .allow(List.of("read", "write"))
                .deny(List.of("delete"))
                .build();

        assertEquals(List.of("read", "write"), policy.getAllow());
        assertEquals(List.of("delete"), policy.getDeny());
    }

    @Test
    void testSandboxToolPolicySource_Builder() {
        SandboxToolPolicySource source = SandboxToolPolicySource.builder()
                .source(ToolPolicySourceType.AGENT)
                .key("agent-key")
                .build();

        assertEquals(ToolPolicySourceType.AGENT, source.getSource());
        assertEquals("agent-key", source.getKey());
    }

    @Test
    void testSandboxToolPolicyResolved_Builder() {
        SandboxToolPolicySource allowSource = SandboxToolPolicySource.builder()
                .source(ToolPolicySourceType.GLOBAL)
                .key("global-key")
                .build();

        SandboxToolPolicyResolved.PolicySources sources = SandboxToolPolicyResolved.PolicySources.builder()
                .allow(allowSource)
                .build();

        SandboxToolPolicyResolved resolved = SandboxToolPolicyResolved.builder()
                .allow(List.of("read"))
                .deny(List.of("exec"))
                .sources(sources)
                .build();

        assertEquals(List.of("read"), resolved.getAllow());
        assertEquals(List.of("exec"), resolved.getDeny());
        assertNotNull(resolved.getSources());
        assertEquals(ToolPolicySourceType.GLOBAL, resolved.getSources().getAllow().getSource());
    }

    @Test
    void testSandboxBrowserConfig_Builder() {
        SandboxBrowserConfig config = SandboxBrowserConfig.builder()
                .enabled(true)
                .image("browser-image")
                .containerPrefix("browser-")
                .cdpPort(9333)
                .vncPort(5901)
                .noVncPort(6081)
                .headless(true)
                .enableNoVnc(false)
                .allowHostControl(true)
                .autoStart(false)
                .autoStartTimeoutMs(30000)
                .build();

        assertTrue(config.isEnabled());
        assertEquals("browser-image", config.getImage());
        assertEquals("browser-", config.getContainerPrefix());
        assertEquals(9333, config.getCdpPort());
        assertEquals(5901, config.getVncPort());
        assertEquals(6081, config.getNoVncPort());
        assertTrue(config.isHeadless());
        assertFalse(config.isEnableNoVnc());
        assertTrue(config.isAllowHostControl());
        assertFalse(config.isAutoStart());
        assertEquals(30000, config.getAutoStartTimeoutMs());
    }

    @Test
    void testSandboxBrowserConfig_DefaultValues() {
        SandboxBrowserConfig config = new SandboxBrowserConfig();

        assertFalse(config.isEnabled());
        assertEquals(9222, config.getCdpPort());
        assertEquals(5900, config.getVncPort());
        assertEquals(6080, config.getNoVncPort());
        assertFalse(config.isHeadless());
        assertTrue(config.isEnableNoVnc());
        assertFalse(config.isAllowHostControl());
        assertTrue(config.isAutoStart());
        assertEquals(12000, config.getAutoStartTimeoutMs());
    }

    @Test
    void testSandboxPruneConfig_Builder() {
        SandboxPruneConfig config = SandboxPruneConfig.builder()
                .idleHours(48)
                .maxAgeDays(14)
                .build();

        assertEquals(48, config.getIdleHours());
        assertEquals(14, config.getMaxAgeDays());
    }

    @Test
    void testSandboxPruneConfig_DefaultValues() {
        SandboxPruneConfig config = new SandboxPruneConfig();

        assertEquals(24, config.getIdleHours());
        assertEquals(7, config.getMaxAgeDays());
    }

    @Test
    void testSandboxConfig_Builder() {
        SandboxDockerConfig docker = SandboxDockerConfig.builder().image("test").build();
        SandboxBrowserConfig browser = SandboxBrowserConfig.builder().enabled(true).build();
        SandboxToolPolicy tools = SandboxToolPolicy.builder().allow(List.of("exec")).build();
        SandboxPruneConfig prune = SandboxPruneConfig.builder().idleHours(12).build();

        SandboxConfig config = SandboxConfig.builder()
                .mode(SandboxMode.ALL)
                .scope(SandboxScope.SESSION)
                .workspaceAccess(SandboxWorkspaceAccess.RW)
                .workspaceRoot("/root")
                .docker(docker)
                .browser(browser)
                .tools(tools)
                .prune(prune)
                .build();

        assertEquals(SandboxMode.ALL, config.getMode());
        assertEquals(SandboxScope.SESSION, config.getScope());
        assertEquals(SandboxWorkspaceAccess.RW, config.getWorkspaceAccess());
        assertEquals("/root", config.getWorkspaceRoot());
        assertEquals(docker, config.getDocker());
        assertEquals(browser, config.getBrowser());
        assertEquals(tools, config.getTools());
        assertEquals(prune, config.getPrune());
    }

    @Test
    void testSandboxConfig_DefaultValues() {
        SandboxConfig config = new SandboxConfig();

        assertEquals(SandboxMode.OFF, config.getMode());
        assertEquals(SandboxScope.AGENT, config.getScope());
        assertEquals(SandboxWorkspaceAccess.NONE, config.getWorkspaceAccess());
        assertNull(config.getWorkspaceRoot());
    }

    @Test
    void testSandboxBrowserContext_Builder() {
        SandboxBrowserContext context = SandboxBrowserContext.builder()
                .bridgeUrl("ws://localhost:9222")
                .noVncUrl("http://localhost:6080")
                .containerName("test-browser")
                .build();

        assertEquals("ws://localhost:9222", context.getBridgeUrl());
        assertEquals("http://localhost:6080", context.getNoVncUrl());
        assertEquals("test-browser", context.getContainerName());
    }

    @Test
    void testSandboxContext_Builder() {
        SandboxDockerConfig docker = SandboxDockerConfig.builder().image("test").build();
        SandboxBrowserContext browser = SandboxBrowserContext.builder().bridgeUrl("ws://localhost:9222").build();
        SandboxToolPolicy tools = SandboxToolPolicy.builder().allow(List.of("read")).build();

        SandboxContext context = SandboxContext.builder()
                .enabled(true)
                .sessionKey("session-123")
                .workspaceDir("/workspace")
                .agentWorkspaceDir("/agent/workspace")
                .workspaceAccess(SandboxWorkspaceAccess.RW)
                .containerName("container-1")
                .containerWorkdir("/workdir")
                .docker(docker)
                .tools(tools)
                .browserAllowHostControl(true)
                .browser(browser)
                .build();

        assertTrue(context.isEnabled());
        assertEquals("session-123", context.getSessionKey());
        assertEquals("/workspace", context.getWorkspaceDir());
        assertEquals("/agent/workspace", context.getAgentWorkspaceDir());
        assertEquals(SandboxWorkspaceAccess.RW, context.getWorkspaceAccess());
        assertEquals("container-1", context.getContainerName());
        assertEquals("/workdir", context.getContainerWorkdir());
        assertEquals(docker, context.getDocker());
        assertEquals(tools, context.getTools());
        assertTrue(context.isBrowserAllowHostControl());
        assertEquals(browser, context.getBrowser());
    }

    @Test
    void testSandboxWorkspaceInfo_Builder() {
        SandboxWorkspaceInfo info = SandboxWorkspaceInfo.builder()
                .workspaceDir("/workspace")
                .containerWorkdir("/container")
                .build();

        assertEquals("/workspace", info.getWorkspaceDir());
        assertEquals("/container", info.getContainerWorkdir());
    }

    @Test
    void testSandboxEnums() {
        // Test SandboxScope enum values
        assertEquals(3, SandboxScope.values().length);
        assertNotNull(SandboxScope.valueOf("SESSION"));
        assertNotNull(SandboxScope.valueOf("AGENT"));
        assertNotNull(SandboxScope.valueOf("SHARED"));

        // Test SandboxWorkspaceAccess enum values
        assertEquals(3, SandboxWorkspaceAccess.values().length);
        assertNotNull(SandboxWorkspaceAccess.valueOf("NONE"));
        assertNotNull(SandboxWorkspaceAccess.valueOf("RO"));
        assertNotNull(SandboxWorkspaceAccess.valueOf("RW"));

        // Test SandboxMode enum values
        assertEquals(3, SandboxMode.values().length);
        assertNotNull(SandboxMode.valueOf("OFF"));
        assertNotNull(SandboxMode.valueOf("NON_MAIN"));
        assertNotNull(SandboxMode.valueOf("ALL"));

        // Test ToolPolicySourceType enum values
        assertEquals(3, ToolPolicySourceType.values().length);
        assertNotNull(ToolPolicySourceType.valueOf("AGENT"));
        assertNotNull(ToolPolicySourceType.valueOf("GLOBAL"));
        assertNotNull(ToolPolicySourceType.valueOf("DEFAULT"));
    }
}

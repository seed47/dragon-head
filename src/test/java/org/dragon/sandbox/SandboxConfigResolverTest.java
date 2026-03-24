package org.dragon.sandbox;

import org.dragon.sandbox.SandboxTypes.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SandboxConfigResolver 配置解析测试
 */
class SandboxConfigResolverTest {

    @Test
    void testResolveSandboxScope_WithExplicitScope() {
        assertEquals(SandboxScope.SESSION, SandboxConfigResolver.resolveSandboxScope(SandboxScope.SESSION, null));
        assertEquals(SandboxScope.AGENT, SandboxConfigResolver.resolveSandboxScope(SandboxScope.AGENT, null));
        assertEquals(SandboxScope.SHARED, SandboxConfigResolver.resolveSandboxScope(SandboxScope.SHARED, null));
    }

    @Test
    void testResolveSandboxScope_WithPerSession() {
        assertEquals(SandboxScope.SESSION, SandboxConfigResolver.resolveSandboxScope(null, true));
        assertEquals(SandboxScope.SHARED, SandboxConfigResolver.resolveSandboxScope(null, false));
    }

    @Test
    void testResolveSandboxScope_Default() {
        assertEquals(SandboxScope.AGENT, SandboxConfigResolver.resolveSandboxScope(null, null));
    }

    @Test
    void testResolveSandboxScope_Priority() {
        // Explicit scope takes priority over perSession
        assertEquals(SandboxScope.SESSION, SandboxConfigResolver.resolveSandboxScope(SandboxScope.SESSION, false));
        assertEquals(SandboxScope.SHARED, SandboxConfigResolver.resolveSandboxScope(SandboxScope.SHARED, true));
    }

    @Test
    void testResolveSandboxDockerConfig_AgentOverridesGlobal() {
        SandboxDockerConfig global = SandboxDockerConfig.builder()
                .image("global-image")
                .containerPrefix("global-")
                .workdir("/global")
                .readOnlyRoot(true)
                .network("none")
                .build();

        SandboxDockerConfig agent = SandboxDockerConfig.builder()
                .image("agent-image")
                .containerPrefix("agent-")
                .build();

        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.AGENT, global, agent);

        assertEquals("agent-image", result.getImage());
        assertEquals("agent-", result.getContainerPrefix());
        assertEquals("/global", result.getWorkdir()); // Uses global default
    }

    @Test
    void testResolveSandboxDockerConfig_SharedScopeIgnoresAgent() {
        SandboxDockerConfig global = SandboxDockerConfig.builder()
                .image("global-image")
                .containerPrefix("global-")
                .build();

        SandboxDockerConfig agent = SandboxDockerConfig.builder()
                .image("agent-image")
                .build();

        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.SHARED, global, agent);

        assertEquals("global-image", result.getImage());
        assertEquals("global-", result.getContainerPrefix());
    }

    @Test
    void testResolveSandboxDockerConfig_Defaults() {
        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.AGENT, null, null);

        assertEquals(SandboxConstants.DEFAULT_SANDBOX_IMAGE, result.getImage());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_CONTAINER_PREFIX, result.getContainerPrefix());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_WORKDIR, result.getWorkdir());
        assertTrue(result.isReadOnlyRoot());
        assertEquals(List.of("/tmp", "/var/tmp", "/run"), result.getTmpfs());
        assertEquals("none", result.getNetwork());
        assertEquals(List.of("ALL"), result.getCapDrop());
    }

    @Test
    void testResolveSandboxDockerConfig_NullValues() {
        // When agent has null values, global should be used
        SandboxDockerConfig global = SandboxDockerConfig.builder()
                .image("global-image")
                .build();

        SandboxDockerConfig agent = new SandboxDockerConfig(); // All null

        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.AGENT, global, agent);

        assertEquals("global-image", result.getImage());
    }

    @Test
    void testResolveSandboxDockerConfig_EnvMerge() {
        SandboxDockerConfig global = SandboxDockerConfig.builder()
                .env(Map.of("GLOBAL_KEY", "global_value", "SHARED", "global"))
                .build();

        SandboxDockerConfig agent = SandboxDockerConfig.builder()
                .env(Map.of("AGENT_KEY", "agent_value", "SHARED", "agent"))
                .build();

        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.AGENT, global, agent);

        // Agent values should override global values
        assertEquals("agent_value", result.getEnv().get("AGENT_KEY"));
        assertEquals("agent", result.getEnv().get("SHARED"));
        assertEquals("global_value", result.getEnv().get("GLOBAL_KEY"));
        // LANG is only added when globalEnv is null, not when it exists
        assertNull(result.getEnv().get("LANG"));
    }

    @Test
    void testResolveSandboxDockerConfig_EnvOnlyGlobal() {
        SandboxDockerConfig global = SandboxDockerConfig.builder()
                .env(Map.of("KEY", "value"))
                .build();

        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.AGENT, global, null);

        assertEquals("value", result.getEnv().get("KEY"));
        // LANG is not automatically added when globalEnv exists
        assertNull(result.getEnv().get("LANG"));
    }

    @Test
    void testResolveSandboxDockerConfig_EnvOnlyAgent() {
        SandboxDockerConfig agent = SandboxDockerConfig.builder()
                .env(Map.of("KEY", "value"))
                .build();

        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.AGENT, null, agent);

        assertEquals("value", result.getEnv().get("KEY"));
        assertEquals("C.UTF-8", result.getEnv().get("LANG"));
    }

    @Test
    void testResolveSandboxDockerConfig_UlimitsMerge() {
        SandboxDockerConfig global = SandboxDockerConfig.builder()
                .ulimits(Map.of("nofile", "1024"))
                .build();

        SandboxDockerConfig agent = SandboxDockerConfig.builder()
                .ulimits(Map.of("nproc", "100"))
                .build();

        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.AGENT, global, agent);

        assertEquals("1024", result.getUlimits().get("nofile"));
        assertEquals("100", result.getUlimits().get("nproc"));
    }

    @Test
    void testResolveSandboxDockerConfig_BindsMerge() {
        SandboxDockerConfig global = SandboxDockerConfig.builder()
                .binds(List.of("/global/bind1", "/global/bind2"))
                .build();

        SandboxDockerConfig agent = SandboxDockerConfig.builder()
                .binds(List.of("/agent/bind"))
                .build();

        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.AGENT, global, agent);

        assertEquals(3, result.getBinds().size());
        assertTrue(result.getBinds().contains("/global/bind1"));
        assertTrue(result.getBinds().contains("/global/bind2"));
        assertTrue(result.getBinds().contains("/agent/bind"));
    }

    @Test
    void testResolveSandboxBrowserConfig_AgentOverridesGlobal() {
        SandboxBrowserConfig global = SandboxBrowserConfig.builder()
                .enabled(false)
                .image("global-image")
                .cdpPort(9222)
                .headless(false)
                .build();

        SandboxBrowserConfig agent = SandboxBrowserConfig.builder()
                .enabled(true)
                .image("agent-image")
                .headless(true)
                .build();

        SandboxBrowserConfig result = SandboxConfigResolver.resolveSandboxBrowserConfig(
                SandboxScope.AGENT, global, agent);

        assertTrue(result.isEnabled());
        assertEquals("agent-image", result.getImage());
        assertTrue(result.isHeadless());
        assertEquals(9222, result.getCdpPort()); // Uses global default
    }

    @Test
    void testResolveSandboxBrowserConfig_SharedScopeIgnoresAgent() {
        SandboxBrowserConfig global = SandboxBrowserConfig.builder()
                .enabled(true)
                .image("global-image")
                .build();

        SandboxBrowserConfig agent = SandboxBrowserConfig.builder()
                .enabled(false)
                .build();

        SandboxBrowserConfig result = SandboxConfigResolver.resolveSandboxBrowserConfig(
                SandboxScope.SHARED, global, agent);

        assertTrue(result.isEnabled());
        assertEquals("global-image", result.getImage());
    }

    @Test
    void testResolveSandboxBrowserConfig_Defaults() {
        SandboxBrowserConfig result = SandboxConfigResolver.resolveSandboxBrowserConfig(
                SandboxScope.AGENT, null, null);

        assertFalse(result.isEnabled());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_BROWSER_IMAGE, result.getImage());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_BROWSER_PREFIX, result.getContainerPrefix());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_BROWSER_CDP_PORT, result.getCdpPort());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_BROWSER_VNC_PORT, result.getVncPort());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_BROWSER_NOVNC_PORT, result.getNoVncPort());
        assertFalse(result.isHeadless());
        assertTrue(result.isEnableNoVnc());
        assertFalse(result.isAllowHostControl());
        assertTrue(result.isAutoStart());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_BROWSER_AUTOSTART_TIMEOUT_MS, result.getAutoStartTimeoutMs());
    }

    @Test
    void testResolveSandboxBrowserConfig_InvalidPortValues() {
        // Negative and zero values should use defaults, but positive values are accepted
        SandboxBrowserConfig global = SandboxBrowserConfig.builder()
                .cdpPort(0)
                .vncPort(-1)
                .noVncPort(70000) // Valid - greater than 0
                .autoStartTimeoutMs(0)
                .build();

        SandboxBrowserConfig result = SandboxConfigResolver.resolveSandboxBrowserConfig(
                SandboxScope.AGENT, global, null);

        assertEquals(SandboxConstants.DEFAULT_SANDBOX_BROWSER_CDP_PORT, result.getCdpPort());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_BROWSER_VNC_PORT, result.getVncPort());
        // 70000 > 0 so it's considered valid
        assertEquals(70000, result.getNoVncPort());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_BROWSER_AUTOSTART_TIMEOUT_MS, result.getAutoStartTimeoutMs());
    }

    @Test
    void testResolveSandboxPruneConfig_AgentOverridesGlobal() {
        SandboxPruneConfig global = SandboxPruneConfig.builder()
                .idleHours(24)
                .maxAgeDays(7)
                .build();

        SandboxPruneConfig agent = SandboxPruneConfig.builder()
                .idleHours(48)
                .build();

        SandboxPruneConfig result = SandboxConfigResolver.resolveSandboxPruneConfig(
                SandboxScope.AGENT, global, agent);

        assertEquals(48, result.getIdleHours());
        assertEquals(7, result.getMaxAgeDays()); // Uses global
    }

    @Test
    void testResolveSandboxPruneConfig_SharedScopeIgnoresAgent() {
        SandboxPruneConfig global = SandboxPruneConfig.builder()
                .idleHours(24)
                .maxAgeDays(7)
                .build();

        SandboxPruneConfig agent = SandboxPruneConfig.builder()
                .idleHours(48)
                .build();

        SandboxPruneConfig result = SandboxConfigResolver.resolveSandboxPruneConfig(
                SandboxScope.SHARED, global, agent);

        assertEquals(24, result.getIdleHours());
        assertEquals(7, result.getMaxAgeDays());
    }

    @Test
    void testResolveSandboxPruneConfig_Defaults() {
        SandboxPruneConfig result = SandboxConfigResolver.resolveSandboxPruneConfig(
                SandboxScope.AGENT, null, null);

        assertEquals(SandboxConstants.DEFAULT_SANDBOX_IDLE_HOURS, result.getIdleHours());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_MAX_AGE_DAYS, result.getMaxAgeDays());
    }

    @Test
    void testResolveSandboxPruneConfig_InvalidValues() {
        // Negative and zero values should use defaults
        SandboxPruneConfig global = SandboxPruneConfig.builder()
                .idleHours(0)
                .maxAgeDays(-1)
                .build();

        SandboxPruneConfig result = SandboxConfigResolver.resolveSandboxPruneConfig(
                SandboxScope.AGENT, global, null);

        assertEquals(SandboxConstants.DEFAULT_SANDBOX_IDLE_HOURS, result.getIdleHours());
        assertEquals(SandboxConstants.DEFAULT_SANDBOX_MAX_AGE_DAYS, result.getMaxAgeDays());
    }

    @Test
    void testResolveSandboxDockerConfig_EmptyAgentEnv() {
        // When agent env is empty map, global should still be used
        SandboxDockerConfig global = SandboxDockerConfig.builder()
                .env(Map.of("GLOBAL", "value"))
                .build();

        SandboxDockerConfig agent = SandboxDockerConfig.builder()
                .env(Map.of())
                .build();

        SandboxDockerConfig result = SandboxConfigResolver.resolveSandboxDockerConfig(
                SandboxScope.AGENT, global, agent);

        // Empty map is treated as not null, so it takes precedence
        // The merge logic uses agentEnv != null && !agentEnv.isEmpty()
        // So if agentEnv is empty, it falls back to global
        assertEquals("value", result.getEnv().get("GLOBAL"));
    }
}

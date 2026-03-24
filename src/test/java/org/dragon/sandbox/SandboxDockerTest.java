package org.dragon.sandbox;

import org.dragon.sandbox.SandboxDocker.*;
import org.dragon.sandbox.SandboxTypes.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SandboxDocker Docker命令执行测试
 */
class SandboxDockerTest {

    @Test
    void testExecResult_Builder() {
        ExecResult result = ExecResult.builder()
                .stdout("output")
                .stderr("error")
                .code(0)
                .build();

        assertEquals("output", result.getStdout());
        assertEquals("error", result.getStderr());
        assertEquals(0, result.getCode());
    }

    @Test
    void testContainerState_Builder() {
        ContainerState state = ContainerState.builder()
                .exists(true)
                .running(true)
                .build();

        assertTrue(state.isExists());
        assertTrue(state.isRunning());
    }

    @Test
    void testBuildSandboxCreateArgs_Basic() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .readOnlyRoot(true)
                .network("none")
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "test-container",
                config,
                "session-key",
                1234567890L,
                null,
                "abc123");

        assertTrue(args.contains("create"));
        assertTrue(args.contains("--name"));
        assertTrue(args.contains("test-container"));
        assertTrue(args.contains("--label"));
        assertTrue(args.contains("dragon.sandbox=1"));
        assertTrue(args.contains("--label"));
        assertTrue(args.contains("dragon.sessionKey=session-key"));
        assertTrue(args.contains("--label"));
        assertTrue(args.contains("dragon.createdAtMs=1234567890"));
        assertTrue(args.contains("--label"));
        assertTrue(args.contains("dragon.configHash=abc123"));
    }

    @Test
    void testBuildSandboxCreateArgs_ReadOnlyRoot() {
        // Test with readOnlyRoot enabled
        SandboxDockerConfig configReadOnly = SandboxDockerConfig.builder()
                .readOnlyRoot(true)
                .build();

        List<String> argsReadOnly = SandboxDocker.buildSandboxCreateArgs(
                "container", configReadOnly, "key", 1L, null, null);
        assertTrue(argsReadOnly.contains("--read-only"));

        // Test with readOnlyRoot disabled
        SandboxDockerConfig configWritable = SandboxDockerConfig.builder()
                .readOnlyRoot(false)
                .build();

        List<String> argsWritable = SandboxDocker.buildSandboxCreateArgs(
                "container", configWritable, "key", 1L, null, null);
        assertFalse(argsWritable.contains("--read-only"));
    }

    @Test
    void testBuildSandboxCreateArgs_Tmpfs() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .tmpfs(List.of("/tmp", "/var/tmp"))
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--tmpfs"));
        assertTrue(args.contains("/tmp"));
        assertTrue(args.contains("--tmpfs"));
        assertTrue(args.contains("/var/tmp"));
    }

    @Test
    void testBuildSandboxCreateArgs_Network() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .network("bridge")
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--network"));
        int networkIndex = args.indexOf("--network");
        assertEquals("bridge", args.get(networkIndex + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_Network_None() {
        // Test with network = "none" should still add --network none
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .network("none")
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--network"));
        int networkIndex = args.indexOf("--network");
        assertEquals("none", args.get(networkIndex + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_User() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .user("1000:1000")
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--user"));
        int userIndex = args.indexOf("--user");
        assertEquals("1000:1000", args.get(userIndex + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_CapDrop() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .capDrop(List.of("ALL", "NET_ADMIN"))
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--cap-drop"));
        assertTrue(args.contains("ALL"));
        assertTrue(args.contains("--cap-drop"));
        assertTrue(args.contains("NET_ADMIN"));
    }

    @Test
    void testBuildSandboxCreateArgs_SecurityOpt() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .seccompProfile("unconfined")
                .apparmorProfile("test-profile")
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        // Should always include no-new-privileges
        assertTrue(args.contains("--security-opt"));
        assertTrue(args.contains("no-new-privileges"));

        // Should include seccomp
        assertTrue(args.contains("seccomp=unconfined"));

        // Should include apparmor
        assertTrue(args.contains("apparmor=test-profile"));
    }

    @Test
    void testBuildSandboxCreateArgs_Dns() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .dns(List.of("8.8.8.8", "8.8.4.4"))
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--dns"));
        assertTrue(args.contains("8.8.8.8"));
        assertTrue(args.contains("8.8.4.4"));
    }

    @Test
    void testBuildSandboxCreateArgs_Dns_FilterBlank() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .dns(List.of("8.8.8.8", "", "   "))
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        // Should only contain valid DNS entries
        assertTrue(args.contains("--dns"));
        int dnsIndex = args.indexOf("--dns");
        assertEquals("8.8.8.8", args.get(dnsIndex + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_ExtraHosts() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .extraHosts(List.of("host.docker.internal:host-gateway"))
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--add-host"));
        int index = args.indexOf("--add-host");
        assertEquals("host.docker.internal:host-gateway", args.get(index + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_ExtraHosts_FilterBlank() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .extraHosts(List.of("", "valid:127.0.0.1", "   "))
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--add-host"));
        int index = args.indexOf("--add-host");
        assertEquals("valid:127.0.0.1", args.get(index + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_PidsLimit() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .pidsLimit(100)
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--pids-limit"));
        int index = args.indexOf("--pids-limit");
        assertEquals("100", args.get(index + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_PidsLimit_Zero() {
        // Zero or negative should not be added
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .pidsLimit(0)
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertFalse(args.contains("--pids-limit"));
    }

    @Test
    void testBuildSandboxCreateArgs_Memory() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .memory("512m")
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--memory"));
        int index = args.indexOf("--memory");
        assertEquals("512m", args.get(index + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_MemorySwap() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .memorySwap("1g")
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--memory-swap"));
        int index = args.indexOf("--memory-swap");
        assertEquals("1g", args.get(index + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_Cpus() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .cpus(2.0)
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("--cpus"));
        int index = args.indexOf("--cpus");
        assertEquals("2.0", args.get(index + 1));
    }

    @Test
    void testBuildSandboxCreateArgs_Cpus_Zero() {
        // Zero or negative should not be added
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .cpus(0.0)
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertFalse(args.contains("--cpus"));
    }

    @Test
    void testBuildSandboxCreateArgs_Binds() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .binds(List.of("/host/path:/container/path:ro", "/data:/data:rw"))
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        assertTrue(args.contains("-v"));
        assertTrue(args.contains("/host/path:/container/path:ro"));
        assertTrue(args.contains("/data:/data:rw"));
    }

    @Test
    void testBuildSandboxCreateArgs_CustomLabels() {
        SandboxDockerConfig config = new SandboxDockerConfig();

        Map<String, String> labels = Map.of(
                "custom.label1", "value1",
                "custom.label2", "value2");

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, labels, null);

        assertTrue(args.contains("--label"));
        // Verify custom labels are present
        assertTrue(args.contains("custom.label1=value1"));
        assertTrue(args.contains("custom.label2=value2"));
    }

    @Test
    void testBuildSandboxCreateArgs_FilterBlankLabels() {
        SandboxDockerConfig config = new SandboxDockerConfig();

        // Labels with blank keys or values should be filtered
        Map<String, String> labels = Map.of(
                "valid", "value",
                "", "empty-key",
                "blank-value", "",
                "   ", "blank-key");

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, labels, null);

        assertTrue(args.contains("--label"));
        assertTrue(args.contains("valid=value"));
        // Blank keys/values should be filtered out
        assertFalse(args.contains("=empty-key"));
        assertFalse(args.contains("blank-value="));
    }

    @Test
    void testBuildSandboxCreateArgs_NullConfigHash() {
        SandboxDockerConfig config = new SandboxDockerConfig();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "container", config, "key", 1L, null, null);

        // Should not contain configHash label when null
        assertFalse(args.contains("dragon.configHash"));
    }

    @Test
    void testBuildSandboxCreateArgs_FullConfig() {
        SandboxDockerConfig config = SandboxDockerConfig.builder()
                .readOnlyRoot(true)
                .tmpfs(List.of("/tmp"))
                .network("bridge")
                .user("1000:1000")
                .capDrop(List.of("ALL"))
                .seccompProfile("unconfined")
                .dns(List.of("8.8.8.8"))
                .extraHosts(List.of("host:ip"))
                .pidsLimit(100)
                .memory("512m")
                .memorySwap("1g")
                .cpus(2.0)
                .binds(List.of("/host:/container"))
                .build();

        List<String> args = SandboxDocker.buildSandboxCreateArgs(
                "full-container", config, "session-123", 1234567890L,
                Map.of("app", "test"), "hash123");

        // Verify all expected elements are present
        assertTrue(args.contains("--name"));
        assertTrue(args.contains("--read-only"));
        assertTrue(args.contains("--tmpfs"));
        assertTrue(args.contains("--network"));
        assertTrue(args.contains("--user"));
        assertTrue(args.contains("--cap-drop"));
        assertTrue(args.contains("--security-opt"));
        assertTrue(args.contains("--dns"));
        assertTrue(args.contains("--add-host"));
        assertTrue(args.contains("--pids-limit"));
        assertTrue(args.contains("--memory"));
        assertTrue(args.contains("--memory-swap"));
        assertTrue(args.contains("--cpus"));
        assertTrue(args.contains("-v"));
        assertTrue(args.contains("--label"));
    }
}

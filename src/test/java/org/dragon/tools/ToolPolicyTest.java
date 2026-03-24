package org.dragon.tools;

import org.dragon.sandbox.SandboxTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolPolicy 工具策略测试。
 * 测试工具允许/拒绝策略、模式匹配等功能。
 */
class ToolPolicyTest {

    @BeforeEach
    void setUp() {
        // Reset static state if needed
    }

    // ==================== 基本功能测试 ====================

    /**
     * 测试 ALLOW_ALL 策略
     */
    @Test
    void testAllowAllPolicy() {
        ToolPolicy policy = ToolPolicy.ALLOW_ALL;

        assertTrue(policy.isAllowed("any-tool"));
        assertTrue(policy.isAllowed("exec"));
        assertTrue(policy.isAllowed("browser"));
        assertTrue(policy.isAllowed(""));
    }

    /**
     * 测试空策略默认允许
     */
    @Test
    void testEmptyPolicyDefaultAllow() {
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), Collections.emptySet(), true);

        assertTrue(policy.isAllowed("any-tool"));
        assertTrue(policy.isAllowed("test"));
    }

    /**
     * 测试空策略默认拒绝
     */
    @Test
    void testEmptyPolicyDefaultDeny() {
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), Collections.emptySet(), false);

        assertFalse(policy.isAllowed("any-tool"));
        assertFalse(policy.isAllowed("test"));
    }

    /**
     * 测试 null 工具名称
     */
    @Test
    void testNullToolName() {
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), Collections.emptySet(), true);

        assertFalse(policy.isAllowed(null));
    }

    /**
     * 测试空工具名称
     */
    @Test
    void testEmptyToolName() {
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), Collections.emptySet(), true);

        assertFalse(policy.isAllowed(""));
        assertFalse(policy.isAllowed("   "));
    }

    // ==================== 白名单模式测试 ====================

    /**
     * 测试白名单 - 明确允许的工具
     */
    @Test
    void testAllowListExactMatch() {
        Set<String> allowPatterns = new HashSet<>(Arrays.asList("exec", "file-read"));
        ToolPolicy policy = new ToolPolicy(allowPatterns, Collections.emptySet(), false);

        assertTrue(policy.isAllowed("exec"));
        assertTrue(policy.isAllowed("file-read"));
    }

    /**
     * 测试白名单 - 不在允许列表中的工具
     */
    @Test
    void testAllowListNotInList() {
        Set<String> allowPatterns = new HashSet<>(Arrays.asList("exec", "file-read"));
        ToolPolicy policy = new ToolPolicy(allowPatterns, Collections.emptySet(), false);

        assertFalse(policy.isAllowed("browser"));
        assertFalse(policy.isAllowed("network"));
    }

    /**
     * 测试白名单 - 通配符匹配
     */
    @Test
    void testAllowListWildcard() {
        Set<String> allowPatterns = new HashSet<>(Arrays.asList("session*"));
        ToolPolicy policy = new ToolPolicy(allowPatterns, Collections.emptySet(), false);

        assertTrue(policy.isAllowed("session-list"));
        assertTrue(policy.isAllowed("sessions"));
        assertFalse(policy.isAllowed("session"));
    }

    // ==================== 黑名单模式测试 ====================

    /**
     * 测试黑名单 - 明确拒绝的工具
     */
    @Test
    void testDenyListExactMatch() {
        Set<String> denyPatterns = new HashSet<>(Arrays.asList("dangerous", "exec"));
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), denyPatterns, true);

        assertFalse(policy.isAllowed("dangerous"));
        assertFalse(policy.isAllowed("exec"));
    }

    /**
     * 测试黑名单 - 未被拒绝的工具
     */
    @Test
    void testDenyListNotDenied() {
        Set<String> denyPatterns = new HashSet<>(Arrays.asList("dangerous"));
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), denyPatterns, true);

        assertTrue(policy.isAllowed("safe-tool"));
        assertTrue(policy.isAllowed("read"));
    }

    /**
     * 测试黑名单 - 通配符匹配
     */
    @Test
    void testDenyListWildcard() {
        Set<String> denyPatterns = new HashSet<>(Arrays.asList("bash*"));
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), denyPatterns, true);

        assertFalse(policy.isAllowed("bash"));
        assertFalse(policy.isAllowed("bash-exec"));
        assertTrue(policy.isAllowed("sh"));
    }

    // ==================== 优先级测试 ====================

    /**
     * 测试拒绝优先级高于允许
     */
    @Test
    void testDenyTakesPrecedenceOverAllow() {
        Set<String> allowPatterns = new HashSet<>(Arrays.asList("exec", "network"));
        Set<String> denyPatterns = new HashSet<>(Arrays.asList("network"));
        ToolPolicy policy = new ToolPolicy(allowPatterns, denyPatterns, false);

        // Even though "network" is in allow list, it's also in deny list
        assertFalse(policy.isAllowed("network"));
        // "exec" is allowed (in allow list, not in deny list)
        assertTrue(policy.isAllowed("exec"));
    }

    /**
     * 测试后缀通配符匹配
     */
    @Test
    void testSuffixWildcardMatch() {
        Set<String> patterns = new HashSet<>(Arrays.asList("*Tool"));
        ToolPolicy policy = new ToolPolicy(patterns, Collections.emptySet(), false);

        assertTrue(policy.isAllowed("FileTool"));
        assertTrue(policy.isAllowed("ImageTool"));
        assertFalse(policy.isAllowed("Tool"));
        assertFalse(policy.isAllowed("MyToolExtra"));
    }

    // ==================== 过滤功能测试 ====================

    /**
     * 测试过滤允许的工具名称
     */
    @Test
    void testFilterAllowed() {
        Set<String> allowPatterns = new HashSet<>(Arrays.asList("read", "list"));
        ToolPolicy policy = new ToolPolicy(allowPatterns, Collections.emptySet(), false);

        Collection<String> toolNames = Arrays.asList("read", "write", "list", "delete");
        List<String> filtered = policy.filterAllowed(toolNames);

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains("read"));
        assertTrue(filtered.contains("list"));
    }

    /**
     * 测试过滤空集合
     */
    @Test
    void testFilterAllowedEmptyCollection() {
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), Collections.emptySet(), true);

        List<String> filtered = policy.filterAllowed(Collections.emptyList());
        assertTrue(filtered.isEmpty());
    }

    // ==================== fromConfig 测试 ====================

    /**
     * 测试从 null 配置创建策略
     */
    @Test
    void testFromConfigNull() {
        ToolPolicy policy = ToolPolicy.fromConfig(null);

        assertSame(ToolPolicy.ALLOW_ALL, policy);
    }

    /**
     * 测试从空配置创建策略
     */
    @Test
    void testFromConfigEmpty() {
        SandboxTypes.SandboxToolPolicy config = SandboxTypes.SandboxToolPolicy.builder()
                .allow(Collections.emptyList())
                .deny(Collections.emptyList())
                .build();

        ToolPolicy policy = ToolPolicy.fromConfig(config);

        // Empty allow list means default allow
        assertTrue(policy.isAllowed("any-tool"));
    }

    /**
     * 测试从白名单配置创建策略
     */
    @Test
    void testFromConfigAllowList() {
        SandboxTypes.SandboxToolPolicy config = SandboxTypes.SandboxToolPolicy.builder()
                .allow(Arrays.asList("exec", "read"))
                .build();

        ToolPolicy policy = ToolPolicy.fromConfig(config);

        assertTrue(policy.isAllowed("exec"));
        assertTrue(policy.isAllowed("read"));
        assertFalse(policy.isAllowed("write"));
    }

    /**
     * 测试从黑名单配置创建策略
     */
    @Test
    void testFromConfigDenyList() {
        SandboxTypes.SandboxToolPolicy config = SandboxTypes.SandboxToolPolicy.builder()
                .deny(Arrays.asList("dangerous"))
                .build();

        ToolPolicy policy = ToolPolicy.fromConfig(config);

        assertFalse(policy.isAllowed("dangerous"));
        assertTrue(policy.isAllowed("safe"));
    }

    /**
     * 测试从同时设置 allow 和 deny 的配置创建策略
     */
    @Test
    void testFromConfigAllowAndDeny() {
        SandboxTypes.SandboxToolPolicy config = SandboxTypes.SandboxToolPolicy.builder()
                .allow(Arrays.asList("exec", "read", "network"))
                .deny(Arrays.asList("network"))
                .build();

        ToolPolicy policy = ToolPolicy.fromConfig(config);

        // deny takes precedence
        assertFalse(policy.isAllowed("network"));
        assertTrue(policy.isAllowed("exec"));
        assertTrue(policy.isAllowed("read"));
    }

    // ==================== toString 测试 ====================

    /**
     * 测试 ALLOW_ALL 的 toString
     */
    @Test
    void testToStringAllowAll() {
        String result = ToolPolicy.ALLOW_ALL.toString();
        assertEquals("ToolPolicy[ALLOW_ALL]", result);
    }

    /**
     * 测试普通策略的 toString
     */
    @Test
    void testToStringNormal() {
        Set<String> allow = new HashSet<>(Arrays.asList("exec"));
        Set<String> deny = new HashSet<>(Arrays.asList("dangerous"));
        ToolPolicy policy = new ToolPolicy(allow, deny, true);

        String result = policy.toString();
        assertTrue(result.contains("allow"));
        assertTrue(result.contains("deny"));
    }

    // ==================== 边界情况测试 ====================

    /**
     * 测试 null allowPatterns
     */
    @Test
    void testNullAllowPatterns() {
        ToolPolicy policy = new ToolPolicy(null, Collections.emptySet(), true);

        assertTrue(policy.isAllowed("any-tool"));
    }

    /**
     * 测试 null denyPatterns
     */
    @Test
    void testNullDenyPatterns() {
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), null, true);

        assertTrue(policy.isAllowed("any-tool"));
    }

    /**
     * 测试精确通配符 *
     */
    @Test
    void testExactWildcardStar() {
        Set<String> patterns = new HashSet<>(Arrays.asList("*"));
        ToolPolicy policy = new ToolPolicy(patterns, Collections.emptySet(), false);

        assertTrue(policy.isAllowed("any-tool"));
        assertTrue(policy.isAllowed("anything"));
    }
}

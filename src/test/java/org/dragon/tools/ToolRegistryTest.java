package org.dragon.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolRegistry 工具注册中心测试。
 * 测试工具的注册、获取、列表、过滤等功能。
 */
class ToolRegistryTest {

    private ToolRegistry registry;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试工具注册基本功能
     */
    @Test
    void testRegisterTool() {
        AgentTool tool = createTestTool("test-tool", "A test tool");
        registry.register(tool);

        Optional<AgentTool> retrieved = registry.get("test-tool");
        assertTrue(retrieved.isPresent());
        assertEquals("test-tool", retrieved.get().getName());
        assertEquals("A test tool", retrieved.get().getDescription());
    }

    /**
     * 测试注册同名工具会覆盖
     */
    @Test
    void testRegisterOverwritesExistingTool() {
        AgentTool tool1 = createTestTool("duplicate-tool", "First description");
        AgentTool tool2 = createTestTool("duplicate-tool", "Second description");

        registry.register(tool1);
        registry.register(tool2);

        Optional<AgentTool> retrieved = registry.get("duplicate-tool");
        assertTrue(retrieved.isPresent());
        assertEquals("Second description", retrieved.get().getDescription());
    }

    /**
     * 测试批量注册工具
     */
    @Test
    void testRegisterAll() {
        AgentTool tool1 = createTestTool("tool-1", "First tool");
        AgentTool tool2 = createTestTool("tool-2", "Second tool");
        AgentTool tool3 = createTestTool("tool-3", "Third tool");

        List<AgentTool> tools = Arrays.asList(tool1, tool2, tool3);
        registry.registerAll(tools);

        assertEquals(3, registry.size());
        assertTrue(registry.get("tool-1").isPresent());
        assertTrue(registry.get("tool-2").isPresent());
        assertTrue(registry.get("tool-3").isPresent());
    }

    /**
     * 测试批量注册时已存在的工具不会被覆盖
     */
    @Test
    void testRegisterAllSkipsExistingTools() {
        AgentTool existingTool = createTestTool("existing", "Existing tool");
        registry.register(existingTool);

        AgentTool tool1 = createTestTool("existing", "Should not overwrite");
        AgentTool tool2 = createTestTool("new-tool", "New tool");
        registry.registerAll(Arrays.asList(tool1, tool2));

        assertEquals(2, registry.size());
        assertEquals("Existing tool", registry.get("existing").get().getDescription());
    }

    /**
     * 测试获取不存在的工具
     */
    @Test
    void testGetNonExistentTool() {
        Optional<AgentTool> retrieved = registry.get("non-existent");
        assertFalse(retrieved.isPresent());
    }

    /**
     * 测试获取所有工具名称
     */
    @Test
    void testGetToolNames() {
        registry.register(createTestTool("alpha", "Alpha tool"));
        registry.register(createTestTool("beta", "Beta tool"));
        registry.register(createTestTool("gamma", "Gamma tool"));

        Set<String> names = registry.getToolNames();
        assertEquals(3, names.size());
        assertTrue(names.contains("alpha"));
        assertTrue(names.contains("beta"));
        assertTrue(names.contains("gamma"));
    }

    /**
     * 测试获取工具名称集合是不可变的
     */
    @Test
    void testGetToolNamesReturnsUnmodifiableSet() {
        registry.register(createTestTool("test", "Test tool"));
        Set<String> names = registry.getToolNames();

        assertThrows(UnsupportedOperationException.class, () -> names.add("new-tool"));
    }

    /**
     * 测试列出所有工具
     */
    @Test
    void testListAll() {
        registry.register(createTestTool("tool-a", "Tool A"));
        registry.register(createTestTool("tool-b", "Tool B"));

        List<AgentTool> allTools = registry.listAll();
        assertEquals(2, allTools.size());
    }

    /**
     * 测试转换为工具定义
     */
    @Test
    void testToDefinitions() {
        AgentTool tool = createTestToolWithSchema("def-tool", "Definition tool",
                "{\"type\": \"object\", \"properties\": {\"command\": {\"type\": \"string\"}}}");
        registry.register(tool);

        List<Map<String, Object>> definitions = registry.toDefinitions();
        assertEquals(1, definitions.size());

        Map<String, Object> def = definitions.get(0);
        assertEquals("def-tool", def.get("name"));
        assertEquals("Definition tool", def.get("description"));
        assertNotNull(def.get("input_schema"));
    }

    /**
     * 测试策略过滤 - 允许所有
     */
    @Test
    void testFilterByPolicyAllowAll() {
        registry.register(createTestTool("tool-1", "Tool 1"));
        registry.register(createTestTool("tool-2", "Tool 2"));

        List<AgentTool> filtered = registry.filterByPolicy(ToolPolicy.ALLOW_ALL);
        assertEquals(2, filtered.size());
    }

    /**
     * 测试策略过滤 - null 策略等同于允许所有
     */
    @Test
    void testFilterByPolicyNull() {
        registry.register(createTestTool("tool-a", "Tool A"));
        registry.register(createTestTool("tool-b", "Tool B"));

        List<AgentTool> filtered = registry.filterByPolicy(null);
        assertEquals(2, filtered.size());
    }

    /**
     * 测试策略过滤 - 白名单模式
     */
    @Test
    void testFilterByPolicyAllowList() {
        registry.register(createTestTool("allowed-tool", "Allowed"));
        registry.register(createTestTool("denied-tool", "Denied"));

        Set<String> allowPatterns = new HashSet<>(Arrays.asList("allowed-tool"));
        ToolPolicy policy = new ToolPolicy(allowPatterns, Collections.emptySet(), false);

        List<AgentTool> filtered = registry.filterByPolicy(policy);
        assertEquals(1, filtered.size());
        assertEquals("allowed-tool", filtered.get(0).getName());
    }

    /**
     * 测试策略过滤 - 黑名单模式
     */
    @Test
    void testFilterByPolicyDenyList() {
        registry.register(createTestTool("safe-tool", "Safe"));
        registry.register(createTestTool("dangerous-tool", "Dangerous"));

        Set<String> denyPatterns = new HashSet<>(Arrays.asList("dangerous-tool"));
        ToolPolicy policy = new ToolPolicy(Collections.emptySet(), denyPatterns, true);

        List<AgentTool> filtered = registry.filterByPolicy(policy);
        assertEquals(1, filtered.size());
        assertEquals("safe-tool", filtered.get(0).getName());
    }

    /**
     * 测试转换为特定 provider 格式
     */
    @Test
    void testToProviderDefinitions() {
        AgentTool tool = createTestTool("provider-tool", "Provider tool");
        registry.register(tool);

        List<Map<String, Object>> definitions = registry.toProviderDefinitions("anthropic");
        assertEquals(1, definitions.size());
    }

    // 辅助方法：创建简单的测试工具
    private AgentTool createTestTool(String name, String description) {
        return createTestToolWithSchema(name, description, "{}");
    }

    private AgentTool createTestToolWithSchema(String name, String description, String schemaJson) {
        JsonNode schema = objectMapper.createObjectNode();
        try {
            schema = objectMapper.readTree(schemaJson);
        } catch (Exception e) {
            // Use default empty object node on parse error
        }

        final JsonNode finalSchema = schema;
        return new AgentTool() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public JsonNode getParameterSchema() {
                return finalSchema;
            }

            @Override
            public CompletableFuture<ToolResult> execute(ToolContext context) {
                return CompletableFuture.completedFuture(ToolResult.ok("Executed"));
            }
        };
    }
}

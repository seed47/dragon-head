package org.dragon.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentTool 接口及其内部类测试。
 * 测试 ToolResult 和 ToolContext 的基本功能。
 */
class AgentToolTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ==================== ToolResult 测试 ====================

    /**
     * 测试创建成功的 ToolResult（仅输出）
     */
    @Test
    void testToolResultOkWithOutput() {
        AgentTool.ToolResult result = AgentTool.ToolResult.ok("Success output");

        assertTrue(result.isSuccess());
        assertEquals("Success output", result.getOutput());
        assertNull(result.getError());
        assertNull(result.getData());
    }

    /**
     * 测试创建成功的 ToolResult（带额外数据）
     */
    @Test
    void testToolResultOkWithData() {
        Object data = new Object();
        AgentTool.ToolResult result = AgentTool.ToolResult.ok("Success output", data);

        assertTrue(result.isSuccess());
        assertEquals("Success output", result.getOutput());
        assertSame(data, result.getData());
    }

    /**
     * 测试创建失败的 ToolResult
     */
    @Test
    void testToolResultFail() {
        AgentTool.ToolResult result = AgentTool.ToolResult.fail("Error message");

        assertFalse(result.isSuccess());
        assertEquals("Error message", result.getError());
        assertNull(result.getOutput());
        assertNull(result.getData());
    }

    /**
     * 测试 ToolResult 无参构造函数
     */
    @Test
    void testToolResultNoArgsConstructor() {
        AgentTool.ToolResult result = new AgentTool.ToolResult();

        // Default values should be null/false
        assertFalse(result.isSuccess());
        assertNull(result.getOutput());
        assertNull(result.getData());
        assertNull(result.getError());
    }

    /**
     * 测试 ToolResult 全参构造函数
     */
    @Test
    void testToolResultAllArgsConstructor() {
        Object data = new Object();
        AgentTool.ToolResult result = new AgentTool.ToolResult(true, "output", data, "error");

        assertTrue(result.isSuccess());
        assertEquals("output", result.getOutput());
        assertSame(data, result.getData());
        assertEquals("error", result.getError());
    }

    // ==================== ToolContext 测试 ====================

    /**
     * 测试 ToolContext 构造和获取属性
     */
    @Test
    void testToolContextBuilder() {
        JsonNode params = objectMapper.createObjectNode();
        org.dragon.config.config.ConfigProperties config = new org.dragon.config.config.ConfigProperties();

        AgentTool.ToolContext context = AgentTool.ToolContext.builder()
                .parameters(params)
                .sessionKey("session-123")
                .cwd("/home/user")
                .config(config)
                .build();

        assertSame(params, context.getParameters());
        assertEquals("session-123", context.getSessionKey());
        assertEquals("/home/user", context.getCwd());
        assertSame(config, context.getConfig());
    }

    /**
     * 测试 ToolContext 无参构造函数
     */
    @Test
    void testToolContextNoArgsConstructor() {
        AgentTool.ToolContext context = new AgentTool.ToolContext();

        assertNull(context.getParameters());
        assertNull(context.getSessionKey());
        assertNull(context.getCwd());
        assertNull(context.getConfig());
    }

    /**
     * 测试 ToolContext 全参构造函数
     */
    @Test
    void testToolContextAllArgsConstructor() throws Exception {
        JsonNode params = objectMapper.readTree("{\"key\": \"value\"}");
        org.dragon.config.config.ConfigProperties config = new org.dragon.config.config.ConfigProperties();

        AgentTool.ToolContext context = new AgentTool.ToolContext(
                params, "session-key", "/workspace", config);

        assertEquals("session-key", context.getSessionKey());
        assertEquals("/workspace", context.getCwd());
        assertNotNull(context.getParameters());
        assertEquals("value", context.getParameters().get("key").asText());
    }

    // ==================== AgentTool 接口实现测试 ====================

    /**
     * 测试完整的 AgentTool 实现
     */
    @Test
    void testCompleteAgentToolImplementation() throws Exception {
        JsonNode schema = objectMapper.readTree("{\"type\": \"object\", \"properties\": {\"cmd\": {\"type\": \"string\"}}}");

        AgentTool tool = new AgentTool() {
            @Override
            public String getName() {
                return "exec";
            }

            @Override
            public String getDescription() {
                return "Execute a command";
            }

            @Override
            public JsonNode getParameterSchema() {
                return schema;
            }

            @Override
            public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
                String cmd = context.getParameters().has("cmd")
                        ? context.getParameters().get("cmd").asText()
                        : "default";
                return CompletableFuture.completedFuture(
                        AgentTool.ToolResult.ok("Executed: " + cmd));
            }
        };

        // Verify tool metadata
        assertEquals("exec", tool.getName());
        assertEquals("Execute a command", tool.getDescription());
        assertNotNull(tool.getParameterSchema());
        assertTrue(tool.getParameterSchema().has("properties"));

        // Test execution
        JsonNode params = objectMapper.readTree("{\"cmd\": \"ls -la\"}");
        AgentTool.ToolContext context = AgentTool.ToolContext.builder()
                .parameters(params)
                .sessionKey("test-session")
                .cwd("/home")
                .build();

        CompletableFuture<AgentTool.ToolResult> future = tool.execute(context);
        AgentTool.ToolResult result = future.get();

        assertTrue(result.isSuccess());
        assertEquals("Executed: ls -la", result.getOutput());
    }

    /**
     * 测试工具执行返回失败结果
     */
    @Test
    void testToolExecuteReturnsFailure() throws Exception {
        AgentTool tool = new AgentTool() {
            @Override
            public String getName() {
                return "fail-tool";
            }

            @Override
            public String getDescription() {
                return "A tool that always fails";
            }

            @Override
            public JsonNode getParameterSchema() {
                return objectMapper.createObjectNode();
            }

            @Override
            public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
                return CompletableFuture.completedFuture(
                        AgentTool.ToolResult.fail("Intentional failure"));
            }
        };

        AgentTool.ToolContext context = AgentTool.ToolContext.builder()
                .parameters(objectMapper.createObjectNode())
                .build();

        AgentTool.ToolResult result = tool.execute(context).get();

        assertFalse(result.isSuccess());
        assertEquals("Intentional failure", result.getError());
    }

    /**
     * 测试工具返回多模态内容数据
     */
    @Test
    void testToolResultWithComplexData() {
        Object complexData = java.util.Map.of(
                "images", java.util.List.of("img1.png", "img2.png"),
                "text", "Result text"
        );

        AgentTool.ToolResult result = AgentTool.ToolResult.ok("Operation completed", complexData);

        assertTrue(result.isSuccess());
        assertEquals("Operation completed", result.getOutput());
        assertNotNull(result.getData());
    }

    /**
     * 测试工具的异步执行特性
     */
    @Test
    void testToolAsyncExecution() throws Exception {
        AgentTool tool = new AgentTool() {
            @Override
            public String getName() {
                return "async-tool";
            }

            @Override
            public String getDescription() {
                return "Async tool";
            }

            @Override
            public JsonNode getParameterSchema() {
                return objectMapper.createObjectNode();
            }

            @Override
            public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
                // Simulate async operation
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return AgentTool.ToolResult.ok("Async result");
                });
            }
        };

        long startTime = System.currentTimeMillis();
        AgentTool.ToolResult result = tool.execute(AgentTool.ToolContext.builder().build()).get();
        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(result.isSuccess());
        assertTrue(elapsed >= 40); // Allow some timing variance
    }
}

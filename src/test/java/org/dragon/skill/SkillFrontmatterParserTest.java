package org.dragon.skill;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * SkillFrontmatterParser 解析功能测试。
 * 测试 frontmatter 解析、元数据解析和调用策略解析功能。
 */
class SkillFrontmatterParserTest {

    @Test
    void testParseFrontmatter() {
        String content = "---\n" +
                "name: test-skill\n" +
                "description: A test skill\n" +
                "user-invocable: true\n" +
                "---\n" +
                "# Body\n" +
                "Hello world";

        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);

        assertEquals(3, frontmatter.size());
        assertEquals("test-skill", frontmatter.get("name"));
        assertEquals("A test skill", frontmatter.get("description"));
        assertEquals("true", frontmatter.get("user-invocable"));
    }

    @Test
    void testParseFrontmatterEmptyContent() {
        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter("");
        assertTrue(frontmatter.isEmpty());
    }

    @Test
    void testParseFrontmatterNullContent() {
        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(null);
        assertTrue(frontmatter.isEmpty());
    }

    @Test
    void testParseFrontmatterNoFrontmatter() {
        String content = "# Just a header\nNo frontmatter here";
        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);
        assertTrue(frontmatter.isEmpty());
    }

    @Test
    void testParseFrontmatterWithMultilineValue() {
        String content = "---\n" +
                "description: A very long\ndescription with\nmultiple lines\n" +
                "---\n";

        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);

        assertNotNull(frontmatter.get("description"));
        assertTrue(frontmatter.get("description").contains("multiple lines"));
    }

    @Test
    void testExtractBody() {
        String content = "---\n" +
                "name: test-skill\n" +
                "---\n" +
                "# Body\n" +
                "Hello world";

        String body = SkillFrontmatterParser.extractBody(content);

        assertEquals("# Body\nHello world", body);
    }

    @Test
    void testExtractBodyNoFrontmatter() {
        String content = "# Just content\nNo frontmatter";
        String body = SkillFrontmatterParser.extractBody(content);
        assertEquals("# Just content\nNo frontmatter", body);
    }

    @Test
    void testExtractBodyEmptyContent() {
        String body = SkillFrontmatterParser.extractBody("");
        assertEquals("", body);
    }

    @Test
    void testResolveMetadata() {
        String content = "---\n" +
                "metadata: { \"dragonhead\": { \"always\": true, \"emoji\": \"🧪\", \"os\": [\"darwin\", \"linux\"] } }\n" +
                "---\n";

        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);
        SkillTypes.SkillMetadata metadata = SkillFrontmatterParser.resolveMetadata(frontmatter);

        assertNotNull(metadata);
        assertTrue(metadata.getAlways());
        assertEquals("🧪", metadata.getEmoji());
        assertNotNull(metadata.getOs());
        assertEquals(2, metadata.getOs().size());
        assertTrue(metadata.getOs().contains("darwin"));
    }

    @Test
    void testResolveMetadataNoMetadataKey() {
        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter("---\nname: test\n---\n");
        SkillTypes.SkillMetadata metadata = SkillFrontmatterParser.resolveMetadata(frontmatter);
        assertNull(metadata);
    }

    @Test
    void testResolveMetadataWithSkillKey() {
        String content = "---\n" +
                "metadata: { \"dragonhead\": { \"skillKey\": \"custom-key\", \"primaryEnv\": \"MY_ENV\" } }\n" +
                "---\n";

        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);
        SkillTypes.SkillMetadata metadata = SkillFrontmatterParser.resolveMetadata(frontmatter);

        assertNotNull(metadata);
        assertEquals("custom-key", metadata.getSkillKey());
        assertEquals("MY_ENV", metadata.getPrimaryEnv());
    }

    @Test
    void testResolveInvocationPolicy() {
        String content = "---\n" +
                "user-invocable: false\n" +
                "disable-model-invocation: true\n" +
                "---\n";

        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);
        SkillTypes.SkillInvocationPolicy policy = SkillFrontmatterParser.resolveInvocationPolicy(frontmatter);

        assertNotNull(policy);
        assertFalse(policy.isUserInvocable());
        assertTrue(policy.isDisableModelInvocation());
    }

    @Test
    void testResolveInvocationPolicyDefaults() {
        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter("---\n---\n");
        SkillTypes.SkillInvocationPolicy policy = SkillFrontmatterParser.resolveInvocationPolicy(frontmatter);

        assertNotNull(policy);
        assertTrue(policy.isUserInvocable()); // default
        assertFalse(policy.isDisableModelInvocation()); // default
    }

    @Test
    void testResolveInvocationPolicyVariousTrueValues() {
        // Test various true values: "true", "yes", "1", "on"
        Map<String, String> frontmatter1 = java.util.Collections.singletonMap("user-invocable", "yes");
        SkillTypes.SkillInvocationPolicy policy1 = SkillFrontmatterParser.resolveInvocationPolicy(frontmatter1);
        assertTrue(policy1.isUserInvocable());

        Map<String, String> frontmatter2 = java.util.Collections.singletonMap("user-invocable", "1");
        SkillTypes.SkillInvocationPolicy policy2 = SkillFrontmatterParser.resolveInvocationPolicy(frontmatter2);
        assertTrue(policy2.isUserInvocable());

        Map<String, String> frontmatter3 = java.util.Collections.singletonMap("user-invocable", "on");
        SkillTypes.SkillInvocationPolicy policy3 = SkillFrontmatterParser.resolveInvocationPolicy(frontmatter3);
        assertTrue(policy3.isUserInvocable());
    }

    @Test
    void testResolveSkillKey() {
        SkillTypes.Skill skill = new SkillTypes.Skill("default-name", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        SkillTypes.SkillMetadata metadata = new SkillTypes.SkillMetadata(null, "custom-key", null, null, null, null, null, null);
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, metadata, null);

        String skillKey = SkillFrontmatterParser.resolveSkillKey(skill, entry);
        assertEquals("custom-key", skillKey);
    }

    @Test
    void testResolveSkillKeyDefaultToName() {
        SkillTypes.Skill skill = new SkillTypes.Skill("default-name", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, null, null);

        String skillKey = SkillFrontmatterParser.resolveSkillKey(skill, entry);
        assertEquals("default-name", skillKey);
    }
}

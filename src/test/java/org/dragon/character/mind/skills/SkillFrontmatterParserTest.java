package org.dragon.character.mind.skills;

import org.dragon.character.mind.skills.SkillTypes.SkillInvocationPolicy;
import org.dragon.character.mind.skills.SkillTypes.SkillMetadata;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void testResolveMetadata() {
        String content = "---\n" +
                "metadata: { \"dragonhead\": { \"always\": true, \"emoji\": \"🧪\", \"os\": [\"darwin\", \"linux\"] } }\n" +
                "---\n";

        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);
        SkillMetadata metadata = SkillFrontmatterParser.resolveMetadata(frontmatter);

        assertNotNull(metadata);
        assertTrue(metadata.getAlways());
        assertEquals("🧪", metadata.getEmoji());
        assertNotNull(metadata.getOs());
        assertEquals(2, metadata.getOs().size());
        assertTrue(metadata.getOs().contains("darwin"));
    }

    @Test
    void testResolveInvocationPolicy() {
        String content = "---\n" +
                "user-invocable: false\n" +
                "disable-model-invocation: true\n" +
                "---\n";

        Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);
        SkillInvocationPolicy policy = SkillFrontmatterParser.resolveInvocationPolicy(frontmatter);

        assertNotNull(policy);
        assertFalse(policy.isUserInvocable());
        assertTrue(policy.isDisableModelInvocation());
    }
}
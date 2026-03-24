package org.dragon.skill;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Skill Entity 数据模型测试。
 * 测试 org.dragon.skill.Skill 实体类的基本功能。
 */
class SkillTest {

    @Test
    void testSkillCreation() {
        Skill skill = Skill.builder()
                .id("skill-001")
                .name("test-skill")
                .description("A test skill")
                .category("testing")
                .tags(Arrays.asList("test", "demo"))
                .build();

        assertNotNull(skill);
        assertEquals("skill-001", skill.getId());
        assertEquals("test-skill", skill.getName());
        assertEquals("A test skill", skill.getDescription());
        assertEquals("testing", skill.getCategory());
        assertEquals(2, skill.getTags().size());
        assertTrue(skill.getTags().contains("test"));
    }

    @Test
    void testSkillWithMetadata() {
        Skill.SkillMetadata metadata = Skill.SkillMetadata.builder()
                .inputParams(Collections.singletonList(
                        Skill.Parameter.builder()
                                .name("input")
                                .type("string")
                                .description("Input parameter")
                                .required(true)
                                .build()))
                .outputParams(Collections.singletonList(
                        Skill.Parameter.builder()
                                .name("result")
                                .type("string")
                                .description("Output result")
                                .required(false)
                                .build()))
                .config(Collections.singletonMap("timeout", 3000))
                .build();

        Skill skill = Skill.builder()
                .id("skill-002")
                .name("skill-with-metadata")
                .description("Skill with metadata")
                .metadata(metadata)
                .build();

        assertNotNull(skill.getMetadata());
        assertEquals(1, skill.getMetadata().getInputParams().size());
        assertEquals(1, skill.getMetadata().getOutputParams().size());
        assertNotNull(skill.getMetadata().getConfig());
        assertEquals(3000, skill.getMetadata().getConfig().get("timeout"));
    }

    @Test
    void testSkillMetadataParameter() {
        Skill.Parameter param = Skill.Parameter.builder()
                .name("command")
                .type("string")
                .description("The command to execute")
                .required(true)
                .defaultValue("ls -la")
                .build();

        assertEquals("command", param.getName());
        assertEquals("string", param.getType());
        assertEquals("The command to execute", param.getDescription());
        assertTrue(param.isRequired());
        assertEquals("ls -la", param.getDefaultValue());
    }

    @Test
    void testSkillWithEmptyTags() {
        Skill skill = Skill.builder()
                .id("skill-003")
                .name("no-tags-skill")
                .description("Skill with no tags")
                .tags(Collections.emptyList())
                .build();

        assertNotNull(skill);
        assertTrue(skill.getTags().isEmpty());
    }

    @Test
    void testSkillSetters() {
        Skill skill = new Skill();
        skill.setId("skill-004");
        skill.setName("setter-test");
        skill.setDescription("Testing setters");
        skill.setCategory("unit-test");
        skill.setTags(Arrays.asList("setter", "test"));

        assertEquals("skill-004", skill.getId());
        assertEquals("setter-test", skill.getName());
        assertEquals("Testing setters", skill.getDescription());
        assertEquals("unit-test", skill.getCategory());
        assertEquals(2, skill.getTags().size());
    }

    @Test
    void testSkillMetadataWithNullConfig() {
        Skill.SkillMetadata metadata = Skill.SkillMetadata.builder()
                .inputParams(null)
                .outputParams(null)
                .config(null)
                .build();

        assertNotNull(metadata);
        assertTrue(metadata.getInputParams().isEmpty());
        assertTrue(metadata.getOutputParams().isEmpty());
        assertTrue(metadata.getConfig().isEmpty());
    }
}

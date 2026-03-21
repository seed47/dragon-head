package org.dragon.skill;

import org.dragon.skill.SkillTypes.Skill;
import org.dragon.skill.SkillTypes.SkillEntry;
import org.dragon.skill.SkillTypes.SkillMetadata;
import org.dragon.skill.SkillTypes.SkillRequires;
import org.dragon.skill.SkillTypes.SkillSource;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillConfigResolverTest {

    @Test
    void testShouldIncludeSkill_AlwaysTrue() {
        Skill skill = new Skill("test", "desc", SkillSource.WORKSPACE, "path", "dir", "content");
        SkillMetadata metadata = new SkillMetadata(true, null, null, null, null, null, null, null);
        SkillEntry entry = new SkillEntry(skill, null, metadata, null);

        assertTrue(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }

    @Test
    void testShouldIncludeSkill_OsMismatch() {
        Skill skill = new Skill("test", "desc", SkillSource.WORKSPACE, "path", "dir", "content");
        // Assuming tests run on something other than TempleOS
        SkillMetadata metadata = new SkillMetadata(null, null, null, null, null, Collections.singletonList("templeos"), null, null);
        SkillEntry entry = new SkillEntry(skill, null, metadata, null);

        assertFalse(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }

    @Test
    void testShouldIncludeSkill_MissingBinary() {
        Skill skill = new Skill("test", "desc", SkillSource.WORKSPACE, "path", "dir", "content");
        SkillRequires requires = new SkillRequires(Collections.singletonList("non_existent_binary_12345"), null, null, null);
        SkillMetadata metadata = new SkillMetadata(null, null, null, null, null, null, requires, null);
        SkillEntry entry = new SkillEntry(skill, null, metadata, null);

        assertFalse(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }

    @Test
    void testShouldIncludeSkill_MissingEnv() {
        Skill skill = new Skill("test", "desc", SkillSource.WORKSPACE, "path", "dir", "content");
        SkillRequires requires = new SkillRequires(null, null, Collections.singletonList("NON_EXISTENT_ENV_VAR_12345"), null);
        SkillMetadata metadata = new SkillMetadata(null, null, null, null, null, null, requires, null);
        SkillEntry entry = new SkillEntry(skill, null, metadata, null);

        assertFalse(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }
}

package org.dragon.skill;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * SkillLoader 加载功能测试。
 * 测试 SkillLoader 的各种加载、过滤和提示词构建功能。
 */
class SkillLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testLoadSkillsFromDir() {
        Path testDir = Paths.get("src/test/resources/skills");
        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(testDir, SkillTypes.SkillSource.WORKSPACE);

        assertNotNull(skills);
        assertEquals(2, skills.size());

        SkillTypes.Skill skill1 = skills.stream().filter(s -> s.getName().equals("test-skill-1")).findFirst().orElse(null);
        assertNotNull(skill1);
        assertEquals("A basic test skill", skill1.getDescription());
        assertEquals(SkillTypes.SkillSource.WORKSPACE, skill1.getSource());
        assertTrue(skill1.getContent().contains("This is the body of test skill 1."));

        SkillTypes.Skill skill2 = skills.stream().filter(s -> s.getName().equals("test-skill-2")).findFirst().orElse(null);
        assertNotNull(skill2);
        assertEquals("A skill with requirements", skill2.getDescription());
    }

    @Test
    void testLoadSkillsFromDirNullPath() {
        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(null, SkillTypes.SkillSource.WORKSPACE);
        assertNotNull(skills);
        assertTrue(skills.isEmpty());
    }

    @Test
    void testLoadSkillsFromDirEmptyPath() {
        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(Paths.get("non-existent-path"), SkillTypes.SkillSource.WORKSPACE);
        assertNotNull(skills);
        assertTrue(skills.isEmpty());
    }

    @Test
    void testLoadSkillsFromDirFiltersNonDirectories() throws Exception {
        // Create temp file (not directory)
        Path tempFile = tempDir.resolve("not-a-skill.txt");
        Files.writeString(tempFile, "content");

        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(tempDir, SkillTypes.SkillSource.WORKSPACE);
        assertNotNull(skills);
        assertTrue(skills.isEmpty());
    }

    @Test
    void testBuildSkillsPrompt() {
        Path testDir = Paths.get("src/test/resources/skills");
        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(testDir, SkillTypes.SkillSource.WORKSPACE);

        // Mock SkillEntry creation for testing prompt builder
        List<SkillTypes.SkillEntry> entries = skills.stream()
                .map(s -> new SkillTypes.SkillEntry(s, null, null, null))
                .toList();

        String prompt = SkillLoader.buildSkillsPrompt(entries);

        assertNotNull(prompt);
        assertTrue(prompt.contains("<skill name=\"test-skill-1\">"));
        assertTrue(prompt.contains("This is the body of test skill 1."));
        assertTrue(prompt.contains("</skill>"));
        assertTrue(prompt.contains("<skill name=\"test-skill-2\">"));
    }

    @Test
    void testBuildSkillsPromptEmptyList() {
        String prompt = SkillLoader.buildSkillsPrompt(Collections.emptyList());
        assertNotNull(prompt);
        assertEquals("", prompt);
    }

    @Test
    void testBuildSkillsPromptNullList() {
        String prompt = SkillLoader.buildSkillsPrompt(null);
        assertNotNull(prompt);
        assertEquals("", prompt);
    }

    @Test
    void testBuildSkillSnapshot() {
        Path testDir = Paths.get("src/test/resources/skills");
        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(testDir, SkillTypes.SkillSource.WORKSPACE);

        List<SkillTypes.SkillEntry> entries = skills.stream()
                .map(s -> new SkillTypes.SkillEntry(s, null, null, null))
                .toList();

        SkillTypes.SkillSnapshot snapshot = SkillLoader.buildSkillSnapshot(entries);

        assertNotNull(snapshot);
        assertNotNull(snapshot.getPrompt());
        assertNotNull(snapshot.getSkills());
        assertEquals(2, snapshot.getSkills().size());
        assertNotNull(snapshot.getResolvedSkills());
    }

    @Test
    void testFilterSkillEntriesByName() {
        Path testDir = Paths.get("src/test/resources/skills");
        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(testDir, SkillTypes.SkillSource.WORKSPACE);

        List<SkillTypes.SkillEntry> entries = skills.stream()
                .map(s -> new SkillTypes.SkillEntry(s, null, null, null))
                .toList();

        List<SkillTypes.SkillEntry> filtered = SkillLoader.filterSkillEntries(entries, null, Collections.singletonList("test-skill-1"));

        assertEquals(1, filtered.size());
        assertEquals("test-skill-1", filtered.get(0).getSkill().getName());
    }

    @Test
    void testFilterSkillEntriesNoMatch() {
        Path testDir = Paths.get("src/test/resources/skills");
        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(testDir, SkillTypes.SkillSource.WORKSPACE);

        List<SkillTypes.SkillEntry> entries = skills.stream()
                .map(s -> new SkillTypes.SkillEntry(s, null, null, null))
                .toList();

        List<SkillTypes.SkillEntry> filtered = SkillLoader.filterSkillEntries(entries, null, Collections.singletonList("non-existent-skill"));

        assertTrue(filtered.isEmpty());
    }
}

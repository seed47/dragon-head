package org.dragon.api.controller;

import lombok.RequiredArgsConstructor;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CharacterController Character（智能体）管理 API
 *
 * <p>职责范围：
 * <ul>
 *   <li>查询所有/单个 Character 及其状态</li>
 *   <li>注册新 Character、更新 Character 配置</li>
 *   <li>Character 状态生命周期管理（启动/暂停/销毁）</li>
 *   <li>设置/查看默认 Character（供 Gateway Fallback 路径使用）</li>
 *   <li>注销 Character</li>
 * </ul>
 *
 * @author zhz
 * @version 1.0
 */
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterRegistry characterRegistry;

    // ==================== 查询 ====================

    /**
     * 查询所有 Character
     * GET /api/characters
     */
    @GetMapping
    public ResponseEntity<List<Character>> listAll() {
        return ResponseEntity.ok(characterRegistry.listAll());
    }

    /**
     * 查询指定 Character
     * GET /api/characters/{characterId}
     */
    @GetMapping("/{characterId}")
    public ResponseEntity<Character> getCharacter(@PathVariable String characterId) {
        return characterRegistry.get(characterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 查询当前默认 Character
     * GET /api/characters/default
     *
     * <p>默认 Character 是 Gateway 在无 Workspace 绑定时的 Fallback 目标。
     */
    @GetMapping("/default")
    public ResponseEntity<Character> getDefaultCharacter() {
        return characterRegistry.getDefaultCharacter()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== 注册与更新 ====================

    /**
     * 注册新 Character
     * POST /api/characters
     *
     * <p>Body 示例：
     * <pre>
     * {
     *   "id": "c-coder-001",
     *   "name": "代码助手",
     *   "description": "擅长代码审查与技术方案",
     *   "status": "UNLOADED",
     *   "agentEngineConfig": {
     *     "defaultModelId": "gpt-4o"
     *   }
     * }
     * </pre>
     */
    @PostMapping
    public ResponseEntity<Character> registerCharacter(@RequestBody Character character) {
        characterRegistry.register(character);
        return ResponseEntity.status(HttpStatus.CREATED).body(character);
    }

    /**
     * 更新 Character 配置
     * PUT /api/characters/{characterId}
     */
    @PutMapping("/{characterId}")
    public ResponseEntity<Character> updateCharacter(
            @PathVariable String characterId,
            @RequestBody Character character) {
        character.setId(characterId);
        characterRegistry.update(character);
        return ResponseEntity.ok(character);
    }

    /**
     * 注销 Character
     * DELETE /api/characters/{characterId}
     */
    @DeleteMapping("/{characterId}")
    public ResponseEntity<Void> unregisterCharacter(@PathVariable String characterId) {
        characterRegistry.unregister(characterId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 状态生命周期 ====================

    /**
     * 加载 Character（UNLOADED → LOADED）
     * POST /api/characters/{characterId}/load
     */
    @PostMapping("/{characterId}/load")
    public ResponseEntity<Void> loadCharacter(@PathVariable String characterId) {
        characterRegistry.load(characterId);
        return ResponseEntity.ok().build();
    }

    /**
     * 启动 Character（→ RUNNING）
     * POST /api/characters/{characterId}/start
     */
    @PostMapping("/{characterId}/start")
    public ResponseEntity<Void> startCharacter(@PathVariable String characterId) {
        characterRegistry.start(characterId);
        return ResponseEntity.ok().build();
    }

    /**
     * 暂停 Character（RUNNING → PAUSED）
     * POST /api/characters/{characterId}/pause
     */
    @PostMapping("/{characterId}/pause")
    public ResponseEntity<Void> pauseCharacter(@PathVariable String characterId) {
        characterRegistry.pause(characterId);
        return ResponseEntity.ok().build();
    }

    /**
     * 销毁 Character（→ DESTROYED）
     * POST /api/characters/{characterId}/destroy
     */
    @PostMapping("/{characterId}/destroy")
    public ResponseEntity<Void> destroyCharacter(@PathVariable String characterId) {
        characterRegistry.destroy(characterId);
        return ResponseEntity.ok().build();
    }

    // ==================== 默认 Character 设置 ====================

    /**
     * 设置默认 Character
     * POST /api/characters/{characterId}/set-default
     *
     * <p>影响 Gateway 无 Workspace 绑定时的 Fallback 行为。
     */
    @PostMapping("/{characterId}/set-default")
    public ResponseEntity<Void> setDefaultCharacter(@PathVariable String characterId) {
        characterRegistry.setDefaultCharacter(characterId);
        return ResponseEntity.ok().build();
    }
}

package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private final CharacterRegistry characterRegistry;

    // ==================== 查询 ====================

    @Operation(summary = "查询所有Character")
    @GetMapping
    public ResponseEntity<List<Character>> listAll() {
        return ResponseEntity.ok(characterRegistry.listAll());
    }

    @Operation(summary = "查询指定Character")
    @GetMapping("/{characterId}")
    public ResponseEntity<Character> getCharacter(@PathVariable String characterId) {
        return characterRegistry.get(characterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "查询当前默认Character")
    @GetMapping("/default")
    public ResponseEntity<Character> getDefaultCharacter() {
        return characterRegistry.getDefaultCharacter()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== 注册与更新 ====================

    @Operation(summary = "注册新Character")
    @PostMapping
    public ResponseEntity<Character> registerCharacter(@RequestBody Character character) {
        characterRegistry.register(character);
        return ResponseEntity.status(HttpStatus.CREATED).body(character);
    }

    @Operation(summary = "更新Character配置")
    @PutMapping("/{characterId}")
    public ResponseEntity<Character> updateCharacter(
            @PathVariable String characterId,
            @RequestBody Character character) {
        character.setId(characterId);
        characterRegistry.update(character);
        return ResponseEntity.ok(character);
    }

    @Operation(summary = "注销Character")
    @DeleteMapping("/{characterId}")
    public ResponseEntity<Void> unregisterCharacter(@PathVariable String characterId) {
        characterRegistry.unregister(characterId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 状态生命周期 ====================

    @Operation(summary = "加载Character")
    @PostMapping("/{characterId}/load")
    public ResponseEntity<Void> loadCharacter(@PathVariable String characterId) {
        characterRegistry.load(characterId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "启动 Character → RUNNING")
    @PostMapping("/{characterId}/start")
    public ResponseEntity<Void> startCharacter(@PathVariable String characterId) {
        characterRegistry.start(characterId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "暂停 Character（RUNNING → PAUSED）")
    @PostMapping("/{characterId}/pause")
    public ResponseEntity<Void> pauseCharacter(@PathVariable String characterId) {
        characterRegistry.pause(characterId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "销毁 Character → DESTROYED）")
    @PostMapping("/{characterId}/destroy")
    public ResponseEntity<Void> destroyCharacter(@PathVariable String characterId) {
        characterRegistry.destroy(characterId);
        return ResponseEntity.ok().build();
    }

    // ==================== 默认 Character 设置 ====================

    @Operation(summary = "设置默认Character")
    @PostMapping("/{characterId}/set-default")
    public ResponseEntity<Void> setDefaultCharacter(@PathVariable String characterId) {
        characterRegistry.setDefaultCharacter(characterId);
        return ResponseEntity.ok().build();
    }
}

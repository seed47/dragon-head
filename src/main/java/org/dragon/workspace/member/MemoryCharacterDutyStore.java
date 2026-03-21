package org.dragon.workspace.member;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Character Duty 内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryCharacterDutyStore implements CharacterDutyStore {

    private final Map<String, CharacterDuty> duties = new ConcurrentHashMap<>();

    @Override
    public void save(CharacterDuty duty) {
        duties.put(duty.getId(), duty);
    }

    @Override
    public void update(CharacterDuty duty) {
        duties.put(duty.getId(), duty);
    }

    @Override
    public void delete(String id) {
        duties.remove(id);
    }

    @Override
    public Optional<CharacterDuty> findById(String id) {
        return Optional.ofNullable(duties.get(id));
    }

    @Override
    public Optional<CharacterDuty> findByWorkspaceIdAndCharacterId(String workspaceId, String characterId) {
        String id = CharacterDuty.createId(workspaceId, characterId);
        return findById(id);
    }

    @Override
    public List<CharacterDuty> findByWorkspaceId(String workspaceId) {
        return duties.values().stream()
                .filter(duty -> workspaceId.equals(duty.getWorkspaceId()))
                .collect(Collectors.toList());
    }
}

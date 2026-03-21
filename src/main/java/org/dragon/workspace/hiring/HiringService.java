package org.dragon.workspace.hiring;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HiringService 雇佣流程服务
 * 处理外部通过 Workspace 提交雇佣请求的统一入口
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HiringService {

    private final WorkspaceRegistry workspaceRegistry;
    private final HiringRequestStore hiringRequestStore;
    private final HiringRecordStore hiringRecordStore;
    private final LLMHiringEngine llmHiringEngine;
    private final CharacterRegistry characterRegistry;

    /**
     * 提交雇佣请求
     * 外部通过 Workspace 提交雇佣请求的统一入口
     *
     * @param workspaceId 工作空间 ID
     * @param request 雇佣请求
     * @return 雇佣请求
     */
    public HiringRequest submitHiringRequest(String workspaceId, HiringRequest request) {
        // 验证工作空间存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 设置基本信息
        request.setId(UUID.randomUUID().toString());
        request.setWorkspaceId(workspaceId);
        request.setStatus(HiringRequestStatus.OPEN);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        // 保存雇佣请求
        hiringRequestStore.save(request);
        log.info("[HiringService] Submitted hiring request: {} to workspace: {}", request.getId(), workspaceId);

        return request;
    }

    /**
     * 处理雇佣请求
     * 只负责将候选人加入 Workspace，不负责任务分发
     *
     * @param request 雇佣请求
     */
    public void processHiringRequest(HiringRequest request) {
        // 更新状态为处理中
        request.setStatus(HiringRequestStatus.PROCESSING);
        request.setUpdatedAt(LocalDateTime.now());
        hiringRequestStore.update(request);

        try {
            // 根据 targetType 处理
            List<Candidate> acceptedCandidates = new ArrayList<>();

            switch (request.getTargetType()) {
                case CHARACTER:
                    acceptedCandidates = handleCharacterTarget(request);
                    break;
                case ROLE:
                case AUTO:
                    acceptedCandidates = handleAutoMatch(request);
                    break;
            }

            // 将候选人加入 Workspace（创建雇佣记录）
            if (!acceptedCandidates.isEmpty()) {
                // 雇佣记录已在各个 handler 中创建
                request.setStatus(HiringRequestStatus.FILLED);
                log.info("[HiringService] Added {} candidates to workspace: {}",
                        acceptedCandidates.size(), request.getWorkspaceId());
            } else {
                request.setStatus(HiringRequestStatus.OPEN);
            }

        } catch (Exception e) {
            log.error("[HiringService] Error processing hiring request: {}", request.getId(), e);
            request.setStatus(HiringRequestStatus.OPEN);
        }

        request.setUpdatedAt(LocalDateTime.now());
        hiringRequestStore.update(request);
    }

    /**
     * 处理指定 Character 的雇佣
     */
    private List<Candidate> handleCharacterTarget(HiringRequest request) {
        String targetId = request.getTargetId();
        if (targetId == null || targetId.isEmpty()) {
            throw new IllegalArgumentException("Target ID is required for CHARACTER target type");
        }

        // 验证 Character 存在
        Character character = characterRegistry.get(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + targetId));

        // 创建候选人
        Candidate candidate = Candidate.builder()
                .id(character.getId())
                .type(Candidate.Type.CHARACTER)
                .name(character.getName())
                .description(character.getDescription())
                .matchScore(100)
                .build();

        // 创建雇佣记录
        createHiringRecord(request, candidate, HiringRecord.Decision.ACCEPTED, "Direct hire - specified character");

        List<Candidate> result = new ArrayList<>();
        result.add(candidate);
        return result;
    }

    /**
     * 处理自动匹配（使用 LLM）
     */
    private List<Candidate> handleAutoMatch(HiringRequest request) {
        // 获取所有可用的 Character
        List<Candidate> availableCandidates = new ArrayList<>();

        // 添加所有 Character
        for (Character character : characterRegistry.listAll()) {
            if (character.getStatus() == Character.Status.RUNNING) {
                availableCandidates.add(Candidate.builder()
                        .id(character.getId())
                        .type(Candidate.Type.CHARACTER)
                        .name(character.getName())
                        .description(character.getDescription())
                        .build());
            }
        }

        // 使用 LLM 匹配候选人
        List<Candidate> matchedCandidates = llmHiringEngine.matchCandidates(request, availableCandidates);

        // 使用 LLM 做出决策
        LLMHiringEngine.HiringDecision decision = llmHiringEngine.makeDecision(request, matchedCandidates);

        // 创建雇佣记录
        for (Candidate candidate : decision.getAcceptedCandidates()) {
            createHiringRecord(request, candidate, HiringRecord.Decision.ACCEPTED, decision.getDecisionReason());
        }

        return decision.getAcceptedCandidates();
    }

    /**
     * 创建雇佣记录
     */
    private void createHiringRecord(HiringRequest request, Candidate candidate, HiringRecord.Decision decision, String reason) {
        HiringRecord record = HiringRecord.builder()
                .id(UUID.randomUUID().toString())
                .hiringRequestId(request.getId())
                .candidateId(candidate.getId())
                .candidateType(HiringRecord.CandidateType.CHARACTER)
                .decision(decision)
                .reason(reason)
                .matchScore(candidate.getMatchScore())
                .hiredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        hiringRecordStore.save(record);
        log.info("[HiringService] Created hiring record for candidate: {}", candidate.getId());
    }

    /**
     * 获取雇佣请求
     *
     * @param workspaceId 工作空间 ID
     * @param hireId 雇佣请求 ID
     * @return 雇佣请求
     */
    public Optional<HiringRequest> getHiringRequest(String workspaceId, String hireId) {
        return hiringRequestStore.findById(hireId)
                .filter(req -> workspaceId.equals(req.getWorkspaceId()));
    }

    /**
     * 获取工作空间的雇佣请求列表
     *
     * @param workspaceId 工作空间 ID
     * @return 雇佣请求列表
     */
    public List<HiringRequest> listHiringRequests(String workspaceId) {
        return hiringRequestStore.findByWorkspaceId(workspaceId);
    }

    /**
     * 获取雇佣记录列表
     *
     * @param hiringRequestId 雇佣请求 ID
     * @return 雇佣记录列表
     */
    public List<HiringRecord> getHiringRecords(String hiringRequestId) {
        return hiringRecordStore.findByHiringRequestId(hiringRequestId);
    }
}

package org.dragon.organization.recruitment;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.dragon.organization.OrganizationRegistry;
import org.dragon.organization.member.MemberManagementService;
import org.dragon.organization.member.OrganizationMember;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RecruitmentService 招聘服务
 * 管理招聘需求发布、候选人评估、录用流程
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final OrganizationRegistry organizationRegistry;
    private final MemberManagementService memberService;

    // 招聘需求存储
    private final Map<String, RecruitmentRequest> requests = new ConcurrentHashMap<>();
    // 候选人评估存储
    private final Map<String, CandidateEvaluation> evaluations = new ConcurrentHashMap<>();

    /**
     * 发布招聘需求
     *
     * @param request 招聘需求
     * @return 含 ID 的招聘需求
     */
    public RecruitmentRequest publishRequest(RecruitmentRequest request) {
        // 验证组织存在
        organizationRegistry.get(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization not found: " + request.getOrganizationId()));

        // 生成 ID
        if (request.getId() == null || request.getId().isEmpty()) {
            request.setId(UUID.randomUUID().toString());
        }
        if (request.getCreatedAt() == null) {
            request.setCreatedAt(LocalDateTime.now());
        }
        if (request.getStatus() == null) {
            request.setStatus(RecruitmentRequest.Status.OPEN);
        }

        requests.put(request.getId(), request);
        log.info("[RecruitmentService] Published recruitment request {} in organization {}",
                request.getId(), request.getOrganizationId());

        return request;
    }

    /**
     * 关闭招聘需求
     *
     * @param requestId 需求 ID
     */
    public void closeRequest(String requestId) {
        RecruitmentRequest request = requests.get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Recruitment request not found: " + requestId);
        }
        request.setStatus(RecruitmentRequest.Status.CLOSED);
        requests.put(requestId, request);
    }

    /**
     * 获取组织招聘需求
     *
     * @param organizationId 组织 ID
     * @return 招聘需求列表
     */
    public List<RecruitmentRequest> getRequests(String organizationId) {
        return requests.values().stream()
                .filter(r -> r.getOrganizationId().equals(organizationId))
                .collect(Collectors.toList());
    }

    /**
     * 获取开放招聘需求
     *
     * @return 开放的需求列表
     */
    public List<RecruitmentRequest> getOpenRequests() {
        return requests.values().stream()
                .filter(r -> r.getStatus() == RecruitmentRequest.Status.OPEN)
                .collect(Collectors.toList());
    }

    /**
     * 评估候选人
     *
     * @param evaluation 评估
     * @return 含 ID 的评估
     */
    public CandidateEvaluation evaluateCandidate(CandidateEvaluation evaluation) {
        // 生成 ID
        if (evaluation.getId() == null || evaluation.getId().isEmpty()) {
            evaluation.setId(UUID.randomUUID().toString());
        }
        if (evaluation.getCreatedAt() == null) {
            evaluation.setCreatedAt(LocalDateTime.now());
        }
        if (evaluation.getStatus() == null) {
            evaluation.setStatus(CandidateEvaluation.Status.EVALUATING);
        }

        // TODO: 实现实际评估逻辑（能力测试、性格匹配等）
        // 这里模拟评估
        evaluation.setCapabilityScore(0.75);
        evaluation.setPersonalityScore(0.8);
        evaluation.setHistoryScore(0.7);
        evaluation.setOverallScore((evaluation.getCapabilityScore() +
                evaluation.getPersonalityScore() +
                evaluation.getHistoryScore()) / 3);
        evaluation.setRecommendation("建议录用");
        evaluation.setStatus(CandidateEvaluation.Status.COMPLETED);
        evaluation.setEvaluatedAt(LocalDateTime.now());

        evaluations.put(evaluation.getId(), evaluation);
        log.info("[RecruitmentService] Evaluated candidate {} for request {}",
                evaluation.getCandidateCharacterId(), evaluation.getRecruitmentRequestId());

        return evaluation;
    }

    /**
     * 录用候选人
     *
     * @param evaluationId 评估 ID
     * @param role 角色
     * @param layer 层级
     * @return 添加的成员
     */
    public OrganizationMember acceptCandidate(String evaluationId, String role,
            OrganizationMember.Layer layer) {
        CandidateEvaluation evaluation = evaluations.get(evaluationId);
        if (evaluation == null) {
            throw new IllegalArgumentException("Evaluation not found: " + evaluationId);
        }

        if (evaluation.getOverallScore() < 0.5) {
            evaluation.setStatus(CandidateEvaluation.Status.REJECTED);
            evaluations.put(evaluationId, evaluation);
            throw new IllegalArgumentException("Candidate score too low, rejected");
        }

        // 添加为组织成员
        OrganizationMember member = memberService.addMember(
                evaluation.getOrganizationId(),
                evaluation.getCandidateCharacterId(),
                role,
                layer
        );

        // 关闭招聘需求
        RecruitmentRequest request = requests.get(evaluation.getRecruitmentRequestId());
        if (request != null) {
            request.setStatus(RecruitmentRequest.Status.FILLED);
            requests.put(request.getId(), request);
        }

        log.info("[RecruitmentService] Accepted candidate {} to organization {} as {}",
                evaluation.getCandidateCharacterId(), evaluation.getOrganizationId(), role);

        return member;
    }

    /**
     * 获取候选人评估列表
     *
     * @param recruitmentRequestId 招聘需求 ID
     * @return 评估列表
     */
    public List<CandidateEvaluation> getEvaluations(String recruitmentRequestId) {
        return evaluations.values().stream()
                .filter(e -> e.getRecruitmentRequestId().equals(recruitmentRequestId))
                .sorted((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()))
                .collect(Collectors.toList());
    }

    /**
     * 模拟协作评估
     * 让候选 Character 参与一个微任务，观察其表现
     *
     * @param organizationId 组织 ID
     * @param candidateCharacterId 候选 Character ID
     * @param microTaskDescription 微任务描述
     * @return 评估结果
     */
    public CandidateEvaluation simulateCollaboration(String organizationId,
            String candidateCharacterId, String microTaskDescription) {
        // TODO: 实现实际的微任务执行和评估
        // 这里返回模拟结果

        CandidateEvaluation evaluation = CandidateEvaluation.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(organizationId)
                .candidateCharacterId(candidateCharacterId)
                .status(CandidateEvaluation.Status.COMPLETED)
                .capabilityScore(0.75)
                .personalityScore(0.8)
                .historyScore(0.7)
                .overallScore(0.75)
                .recommendation("表现良好，建议录用")
                .evaluatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        evaluations.put(evaluation.getId(), evaluation);
        return evaluation;
    }
}

package com.fairpilot.service;

import com.fairpilot.*;
import com.fairpilot.ai.CitationAssembler;
import com.fairpilot.ai.HallucinationGuard;
import com.fairpilot.ai.LlmClient;
import com.fairpilot.ai.PromptBuilder;
import com.fairpilot.dto.AssistantDto.*;
import com.fairpilot.repository.BoothRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import com.fairpilot.ai.HallucinationGuard.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AssistantService — 파이프라인 오케스트레이터
 * ────────────────────────────────────────────────────────────────
 *
 *  ① buildCandidatePool   후보 풀 구성 (요청 ID ∩ DB 실존)
 *  ② PromptBuilder.build  Spring AI Prompt 조립 (그라운딩)
 *  ③ LlmClient.complete   Ollama 호출 → JSON 파싱
 *  ④ HallucinationGuard   3중 검증 및 마스킹
 *  ⑤ CitationAssembler    검증 ID 에만 DB 원본 정보 조립
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final BoothRepository boothRepository;
    private final HallucinationGuard guard;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final CitationAssembler assembler;
    private final OllamaProperties ollamaProperties;

    public AskResponse ask(AskRequest request) {

        // ① 후보 풀 구성
        Set<String> candidatePool = guard.buildCandidatePool(request.getCandidateBoothIds());
        List<Booth> candidateBooths = boothRepository.findAllById(candidatePool);
        log.debug("[AssistantService] 후보풀={}, 질문='{}'",
                candidatePool.size(), request.getQuestion());

        // ② Spring AI Prompt 조립
        Prompt prompt = promptBuilder.build(request.getQuestion(), candidateBooths);

        // ③ Ollama 호출
        LlmRawOutput llmOutput = llmClient.complete(prompt);

        // ④ 할루시네이션 가드
        GuardResult guardResult = guard.verify(llmOutput, candidatePool);
        log.debug("[AssistantService] 검증완료={}, 제거={}+{}",
                guardResult.verifiedIds(),
                guardResult.hallucinatedIds(),
                guardResult.textLeakedIds());

        // ⑤ 인용 조립
        Map<String, Booth> boothMap = candidateBooths.stream()
                .collect(Collectors.toMap(Booth::getId, b -> b));

        List<CitedBooth> citedBooths = assembler.assemble(
                guardResult.verifiedIds(), boothMap, llmOutput.getCitationNotes());

        List<String> allHallucinatedIds = new ArrayList<>();
        allHallucinatedIds.addAll(guardResult.hallucinatedIds());
        allHallucinatedIds.addAll(guardResult.textLeakedIds());

        return AskResponse.builder()
                .answer(guardResult.sanitizedAnswer())
                .citedBooths(citedBooths)
                .removedHallucinatedIds(allHallucinatedIds)
                .meta(Meta.builder()
                        .candidatePoolSize(candidatePool.size())
                        .modelUsed(ollamaProperties.getModel())
                        .llmRawBoothIds(llmOutput.getReferencedBoothIds())
                        .verifiedBoothIds(guardResult.verifiedIds())
                        .build())
                .build();
    }
}

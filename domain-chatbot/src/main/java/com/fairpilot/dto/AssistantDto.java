package com.fairpilot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

public class AssistantDto {

    // ── 요청 ──────────────────────────────────────────────────────
    @Getter
    public static class AskRequest {
        @NotBlank(message = "question 은 필수입니다.")
        @Size(max = 500, message = "question 은 500자 이하여야 합니다.")
        private String question;

        /** 선택: 사전에 좁혀둔 후보 부스 ID 목록. 없으면 전체 풀 사용. */
        private List<String> candidateBoothIds;
    }

    // ── 응답 ──────────────────────────────────────────────────────
    @Getter
    @Builder
    public static class AskResponse {
        private String answer;
        private List<CitedBooth> citedBooths;
        private List<String> removedHallucinatedIds;
        private Meta meta;
    }

    @Getter
    @Builder
    public static class CitedBooth {
        private String id;
        private String name;
        private String company;
        private String location;
        private String relevanceNote;
    }

    @Getter
    @Builder
    public static class Meta {
        private int candidatePoolSize;
        private String modelUsed;
        private List<String> llmRawBoothIds;
        private List<String> verifiedBoothIds;
    }

    // ── LLM 원시 출력 (내부용) ─────────────────────────────────────
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LlmRawOutput {
        private String answer;
        private List<String> referencedBoothIds;
        private Map<String, String> citationNotes;
    }
}

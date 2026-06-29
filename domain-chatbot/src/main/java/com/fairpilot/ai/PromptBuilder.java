package com.fairpilot.ai;

import com.fairpilot.Booth;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PromptBuilder
 * ────────────────────────────────────────────────────────────────
 * Spring AI 의 Prompt / SystemMessage / UserMessage 를 사용해
 * 후보 부스 정보를 컨텍스트로 주입하는 그라운딩 프롬프트 생성
 */
@Component
public class PromptBuilder {

    public Prompt build(String question, List<Booth> candidateBooths) {
        return new Prompt(List.of(
                new SystemMessage(systemText()),
                new UserMessage(userText(question, candidateBooths))
        ));
    }

    private String systemText() {
        return """
                당신은 전시회 안내 도우미 AI입니다.

                ## 핵심 규칙 (반드시 준수)
                1. 오직 아래 [부스 데이터베이스]에 있는 부스 정보만 사용하세요.
                2. 데이터베이스에 없는 부스 ID, 회사명, 제품을 절대 창작하지 마세요.
                3. 모르는 정보는 "해당 정보는 제공된 부스 데이터에 없습니다"라고 답하세요.
                4. 응답은 반드시 아래 JSON 형식으로만 반환하세요. 마크다운 코드블록도 제외하세요.

                ## 응답 JSON 형식
                {
                  "answer": "참관객을 위한 친절한 한국어 답변",
                  "referencedBoothIds": ["실제로 답변에 인용한 부스 ID — 부스 데이터베이스에 있는 것만"],
                  "citationNotes": {
                    "BOOTH-XXX": "이 부스를 인용한 구체적 이유"
                  }
                }
                """;
    }

    private String userText(String question, List<Booth> candidateBooths) {
        String boothContext = candidateBooths.stream()
                .map(b -> """
                        [%s] %s (%s)
                          - 카테고리: %s
                          - 태그: %s
                          - 설명: %s
                          - 위치: %s
                        """.formatted(
                        b.getId(), b.getName(), b.getCompany(),
                        b.getCategory(),
                        String.join(", ", b.getTags()),
                        b.getDescription(),
                        b.getLocation()
                ))
                .collect(Collectors.joining("\n"));

        return """
                ## 부스 데이터베이스 (총 %d개 — 이 목록 외 부스는 존재하지 않음)

                %s

                ---

                ## 참관객 질문
                %s

                위 부스 데이터베이스만 참조하여 JSON 형식으로 답변하세요.
                """.formatted(candidateBooths.size(), boothContext, question);
    }
}

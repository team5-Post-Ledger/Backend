package com.fairpilot.repository;

import com.fairpilot.Booth;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class BoothRepository {

    private final Map<String, Booth> store;

    public BoothRepository() {
        List<Booth> samples = List.of(
                Booth.builder().id("BOOTH-001").name("AI 비전 솔루션관")
                        .company("테크노비전(주)").category("인공지능")
                        .tags(List.of("컴퓨터비전", "객체인식", "제조AI"))
                        .description("제조 공정 불량 검출을 위한 엣지 AI 비전 솔루션 전시.")
                        .location("A홀 1번 부스").contact("vision@technovision.kr").build(),

                Booth.builder().id("BOOTH-002").name("스마트 물류 자동화관")
                        .company("로지텍AI").category("물류·SCM")
                        .tags(List.of("AMR", "WMS", "자동화"))
                        .description("자율주행 로봇(AMR)과 WMS 통합 물류 자동화 플랫폼.")
                        .location("A홀 2번 부스").contact("info@logitech-ai.co.kr").build(),

                Booth.builder().id("BOOTH-003").name("헬스케어 데이터 플랫폼관")
                        .company("메디클라우드").category("헬스케어")
                        .tags(List.of("EHR", "의료AI", "FHIR"))
                        .description("FHIR 기반 전자의무기록 통합 및 임상 의사결정 지원 AI.")
                        .location("B홀 1번 부스").contact("contact@medicloud.kr").build(),

                Booth.builder().id("BOOTH-004").name("사이버보안 제로트러스트관")
                        .company("시큐어넷").category("보안")
                        .tags(List.of("제로트러스트", "SIEM", "EDR"))
                        .description("제로트러스트 아키텍처 기반 엔터프라이즈 보안 플랫폼.")
                        .location("B홀 2번 부스").contact("sales@securenet.kr").build(),

                Booth.builder().id("BOOTH-005").name("그린에너지 모니터링관")
                        .company("에코테크").category("에너지·환경")
                        .tags(List.of("ESG", "태양광", "에너지관리"))
                        .description("태양광·풍력 발전소 실시간 모니터링 및 ESG 리포팅 솔루션.")
                        .location("C홀 1번 부스").contact("eco@ecotech.kr").build(),

                Booth.builder().id("BOOTH-006").name("핀테크 결제 혁신관")
                        .company("페이플러스").category("핀테크")
                        .tags(List.of("간편결제", "오픈뱅킹", "PG"))
                        .description("오픈뱅킹 연동 통합 결제 게이트웨이 및 정산 자동화.")
                        .location("C홀 2번 부스").contact("biz@payplus.kr").build(),

                Booth.builder().id("BOOTH-007").name("메타버스 교육 플랫폼관")
                        .company("에듀버스").category("교육·XR")
                        .tags(List.of("메타버스", "VR교육", "LMS"))
                        .description("VR 기반 직무 교육 시뮬레이터와 LMS 통합 플랫폼.")
                        .location("D홀 1번 부스").contact("edu@eduverse.kr").build(),

                Booth.builder().id("BOOTH-008").name("스마트팜 IoT관")
                        .company("애그리테크").category("농업·IoT")
                        .tags(List.of("스마트팜", "수직농장", "IoT센서"))
                        .description("IoT 센서 기반 수직농장 자동화 및 작물 생장 예측 AI.")
                        .location("D홀 2번 부스").contact("farm@agritech.kr").build()
        );

        this.store = samples.stream()
                .collect(Collectors.toUnmodifiableMap(Booth::getId, b -> b));
    }

    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    public Optional<Booth> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Booth> findAllById(Collection<String> ids) {
        return ids.stream()
                .map(store::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public Set<String> findAllIds() {
        return store.keySet();
    }
}

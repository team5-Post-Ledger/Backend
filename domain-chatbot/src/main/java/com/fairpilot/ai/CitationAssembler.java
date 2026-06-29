package com.fairpilot.ai;

import com.fairpilot.Booth;
import com.fairpilot.dto.AssistantDto.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CitationAssembler {

    public List<CitedBooth> assemble(
            List<String> verifiedIds,
            Map<String, Booth> boothMap,
            Map<String, String> citationNotes
    ) {
        return verifiedIds.stream()
                .filter(boothMap::containsKey)
                .map(id -> {
                    Booth booth = boothMap.get(id);
                    return CitedBooth.builder()
                            .id(booth.getId())
                            .name(booth.getName())           // DB 원본만 사용
                            .company(booth.getCompany())     // DB 원본만 사용
                            .location(booth.getLocation())
                            .relevanceNote(citationNotes.getOrDefault(id, "관련 부스로 선정됨"))
                            .build();
                })
                .toList();
    }
}

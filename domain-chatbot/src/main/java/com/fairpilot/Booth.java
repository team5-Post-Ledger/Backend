package com.fairpilot;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class Booth {
    private String id;          // "BOOTH-001"
    private String name;
    private String company;
    private String category;
    private List<String> tags;
    private String description;
    private String location;
    private String contact;
}

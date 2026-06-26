package com.fairpilot.dto;

public record BoothInfo(
        Long id,
        String name,
        String description,
        String category,
        int posX,
        int posY,
        String congestionLevel  // "여유" | "보통" | "혼잡"
) {}

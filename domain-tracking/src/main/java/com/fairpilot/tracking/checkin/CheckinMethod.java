package com.fairpilot.tracking.checkin;

public enum CheckinMethod {
    QR_SELF,        // QR 스캔
    MANUAL_SEARCH,  // 수기 조회
    ONSITE_MANUAL,  // 현장 수기
    WALK_IN,        // 워크인
    REISSUE         // 재발급
}
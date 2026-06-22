package com.fairpilot.tracking.checkin;

import com.fairpilot.core.auth.CurrentUser;
import com.fairpilot.core.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exhibitions/{exhibitionId}/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final NametagBindingService nametagBindingService;

    /**
     * 입구 2-스캔 바인딩 체크인
     * EXPO_ADMIN / STAFF 전용
     */
    @PostMapping("/nametag")
    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'STAFF', 'PLATFORM_ADMIN')")
    public ApiResponse<Void> checkinByNametag(
            @PathVariable Long exhibitionId,
            @RequestParam String nameTagToken,
            @RequestParam Long attendeeId,
            @RequestParam Long reservationId,
            @CurrentUser Long staffUserId) {

        nametagBindingService.bind(
                nameTagToken, attendeeId, reservationId,
                exhibitionId, staffUserId);

        return ApiResponse.ok(null);
    }

    /**
     * 수기 체크인 (인터넷 오류 대응)
     * EXPO_ADMIN / STAFF 전용
     */
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'STAFF', 'PLATFORM_ADMIN')")
    public ApiResponse<Void> checkinManual(
            @PathVariable Long exhibitionId,
            @RequestParam Long attendeeId,
            @RequestParam Long reservationId,
            @RequestParam(required = false) String memo,
            @CurrentUser Long staffUserId) {

        nametagBindingService.bindManual(
                attendeeId, reservationId,
                exhibitionId, staffUserId, memo);

        return ApiResponse.ok(null);
    }
}
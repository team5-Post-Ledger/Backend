package com.fairpilot.payment;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    /**
     * 포트원 V2 webhook 수신
     */
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(
            @RequestBody PortOneWebhookPayload payload) {
        log.info("webhook 수신: type={}, paymentId={}",
                payload.type(), payload.data().paymentId());
        paymentService.handleWebhook(payload);
        return ApiResponse.ok(null);
    }

    /**
     * 토스페이먼츠 webhook 수신
     * Authorization: Basic {Base64(secret:)} 헤더 검증 필수
     * 검증 실패 시 401 반환 (위변조 방어)
     */
    @PostMapping("/webhook/toss")
    public ApiResponse<Void> tossWebhook(
            @RequestHeader("Authorization") String authorization,
            @RequestBody TossWebhookPayload payload) {

        verifyTossAuthorization(authorization);

        log.info("토스 webhook 수신: eventType={}, paymentKey={}",
                payload.eventType(), payload.data().paymentKey());
        paymentService.handleTossWebhook(payload);
        return ApiResponse.ok(null);
    }

    /**
     * ONSITE 현장결제 등록
     * EXPO_ADMIN / STAFF 전용
     */
    @PostMapping("/onsite")
    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'STAFF')")
    public ApiResponse<Void> onsite(
            @RequestBody @Valid OnsitePaymentRequest req) {
        paymentService.handleOnsite(req);
        return ApiResponse.ok(null);
    }

    /**
     * 토스 Authorization 헤더 검증
     * 토스는 Basic {Base64(secret:)} 형식으로 헤더를 보냄
     * secret 뒤에 콜론(:)을 붙인 뒤 Base64 인코딩
     */
    private void verifyTossAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토스 Authorization 헤더가 없습니다.");
        }
        String expected = "Basic " + Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        if (!expected.equals(authorization)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토스 Authorization 헤더가 유효하지 않습니다.");
        }
    }
}
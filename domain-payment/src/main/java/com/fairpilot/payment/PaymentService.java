package com.fairpilot.payment;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 포트원 webhook 멱등 처리
     * PAID/FAILED: 중복 수신 시 무시
     * CANCELLED: 기존 레코드 상태 업데이트 (멱등성 체크 제외)
     */
    @Transactional
    public void handleWebhook(PortOneWebhookPayload payload) {
        String pgTxId = payload.data().paymentId();
        String status = payload.data().status();

        switch (status) {
            case "PAID" -> {
                if (paymentRepository.findByPgTxId(pgTxId).isPresent()) {
                    log.info("중복 PAID webhook 무시: pgTxId={}", pgTxId);
                    return;
                }
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(pgTxId))
                        .pgProvider("PORTONE")
                        .pgTxId(pgTxId)
                        .amount(BigDecimal.ZERO)
                        .feeAmount(BigDecimal.ZERO)
                        .build();
                payment.markPaid();
                paymentRepository.save(payment);
                log.info("결제 완료 처리: pgTxId={}", pgTxId);
            }
            case "FAILED" -> {
                if (paymentRepository.findByPgTxId(pgTxId).isPresent()) {
                    log.info("중복 FAILED webhook 무시: pgTxId={}", pgTxId);
                    return;
                }
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(pgTxId))
                        .pgProvider("PORTONE")
                        .pgTxId(pgTxId)
                        .amount(BigDecimal.ZERO)
                        .feeAmount(BigDecimal.ZERO)
                        .build();
                payment.markFailed();
                paymentRepository.save(payment);
                log.info("결제 실패 처리: pgTxId={}", pgTxId);
            }
            case "CANCELLED" -> {
                paymentRepository.findByPgTxId(pgTxId).ifPresentOrElse(p -> {
                    p.markCancelled();
                    paymentRepository.save(p);
                    log.info("결제 취소 처리: pgTxId={}", pgTxId);
                }, () -> log.warn("취소 대상 결제 없음: pgTxId={}", pgTxId));
            }
            default -> log.warn("알 수 없는 결제 상태: {}", status);
        }
    }

    /**
     * 토스페이먼츠 webhook 멱등 처리
     * DONE/ABORTED: 중복 수신 시 무시
     * CANCELED: 기존 레코드 상태 업데이트 (멱등성 체크 제외)
     */
    @Transactional
    public void handleTossWebhook(TossWebhookPayload payload) {
        String pgTxId = payload.data().paymentKey();
        String orderId = payload.data().orderId();
        String status = payload.data().status();
        BigDecimal amount = payload.data().totalAmount();

        switch (status) {
            case "DONE" -> {
                if (paymentRepository.findByPgTxId(pgTxId).isPresent()) {
                    log.info("중복 DONE webhook 무시: pgTxId={}", pgTxId);
                    return;
                }
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(orderId))
                        .pgProvider("TOSS")
                        .pgTxId(pgTxId)
                        .amount(amount != null ? amount : BigDecimal.ZERO)
                        .feeAmount(BigDecimal.ZERO)
                        .build();
                payment.markPaid();
                paymentRepository.save(payment);
                log.info("토스 결제 완료 처리: pgTxId={}", pgTxId);
            }
            case "ABORTED" -> {
                if (paymentRepository.findByPgTxId(pgTxId).isPresent()) {
                    log.info("중복 ABORTED webhook 무시: pgTxId={}", pgTxId);
                    return;
                }
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(orderId))
                        .pgProvider("TOSS")
                        .pgTxId(pgTxId)
                        .amount(BigDecimal.ZERO)
                        .feeAmount(BigDecimal.ZERO)
                        .build();
                payment.markFailed();
                paymentRepository.save(payment);
                log.info("토스 결제 실패 처리: pgTxId={}", pgTxId);
            }
            case "CANCELED" -> {
                paymentRepository.findByPgTxId(pgTxId).ifPresentOrElse(p -> {
                    p.markCancelled();
                    paymentRepository.save(p);
                    log.info("토스 결제 취소 처리: pgTxId={}", pgTxId);
                }, () -> log.warn("취소 대상 토스 결제 없음: pgTxId={}", pgTxId));
            }
            default -> log.warn("알 수 없는 토스 결제 상태: {}", status);
        }
    }

    /** ONSITE 현장결제 처리 (/onsite 엔드포인트 전용) */
    @Transactional
    public void handleOnsite(OnsitePaymentRequest req) {
        if (paymentRepository.findByPgTxId(req.pgTxId()).isPresent()) {
            log.info("중복 ONSITE 결제 무시: pgTxId={}", req.pgTxId());
            return;
        }
        Payment payment = Payment.builder()
                .reservationId(req.reservationId())
                .exhibitionId(req.exhibitionId())
                .pgProvider("ONSITE")
                .pgTxId(req.pgTxId())
                .amount(req.amount())
                .feeAmount(BigDecimal.ZERO)
                .build();
        payment.markPaid();
        paymentRepository.save(payment);
        log.info("현장결제 처리: pgTxId={}, exhibitionId={}", req.pgTxId(), req.exhibitionId());
    }

    /**
     * ID 문자열에서 reservationId 추출
     * 포트원: pgTxId 기반, 토스: orderId 기반 — 동일한 {reservationId}_{uuid} 형식
     */
    private Long extractReservationId(String id) {
        try {
            return Long.parseLong(id.split("_")[0]);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "reservationId 추출 실패: " + id);
        }
    }
}
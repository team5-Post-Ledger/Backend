package com.fairpilot.payment;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 결제 요청 발급
     * orderId = {reservationId}_{UUID} 형식으로 생성
     * READY Payment 레코드 저장 후 orderId + amount 반환
     *
     * 중복 방지: 동일 reservationId에 READY/PAID 결제가 이미 있으면 예외
     * (사용자가 결제창을 두 번 여는 경우 방어)
     */
    @Transactional
    public PaymentInitiateResponse initiate(PaymentInitiateRequest req) {
        // pgProvider 유효성 검증
        if (!List.of("TOSS", "PORTONE").contains(req.pgProvider())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "지원하지 않는 PG사입니다: " + req.pgProvider());
        }

        // 동일 예약의 READY/PAID 결제 중복 방지
        paymentRepository.findByReservationIdAndStatusIn(
                req.reservationId(),
                List.of(PaymentStatus.READY, PaymentStatus.PAID)
        ).ifPresent(p -> {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "이미 진행 중이거나 완료된 결제가 있습니다. reservationId=" + req.reservationId());
        });

        // orderId 생성: {reservationId}_{UUID 하이픈 제거}
        // extractReservationId()에서 split("_")[0]으로 파싱하므로 이 형식 고정
        String orderId = req.reservationId() + "_" + UUID.randomUUID().toString().replace("-", "");

        Payment payment = Payment.builder()
                .reservationId(req.reservationId())
                .exhibitionId(req.exhibitionId())
                .pgProvider(req.pgProvider())
                .pgTxId(orderId)   // 포트원은 pgTxId = orderId, 토스는 webhook 시 paymentKey로 갱신
                .amount(req.amount())
                .feeAmount(BigDecimal.ZERO)
                .build();

        paymentRepository.save(payment);
        log.info("결제 요청 발급: orderId={}, pgProvider={}, amount={}",
                orderId, req.pgProvider(), req.amount());

        return new PaymentInitiateResponse(orderId, req.amount());
    }

    /**
     * 포트원 webhook 멱등 처리
     * PAID/FAILED: 중복 수신 시 무시
     * CANCELLED: 기존 레코드 상태 업데이트
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
                // initiate()로 생성된 READY 레코드가 있으면 업데이트, 없으면 신규 생성
                Payment payment = paymentRepository
                        .findByReservationIdAndStatusIn(
                                extractReservationId(orderId),
                                List.of(PaymentStatus.READY))
                        .map(p -> {
                            // pgTxId를 paymentKey로 갱신 (initiate 시엔 orderId로 저장됨)
                            p.updatePgTxId(pgTxId);
                            p.updateAmount(amount != null ? amount : BigDecimal.ZERO);
                            return p;
                        })
                        .orElseGet(() -> Payment.builder()
                                .reservationId(extractReservationId(orderId))
                                .pgProvider("TOSS")
                                .pgTxId(pgTxId)
                                .amount(amount != null ? amount : BigDecimal.ZERO)
                                .feeAmount(BigDecimal.ZERO)
                                .build());
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

    /** ONSITE 현장결제 처리 */
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

    /** ID 문자열에서 reservationId 추출 — {reservationId}_{uuid} 형식 */
    private Long extractReservationId(String id) {
        try {
            return Long.parseLong(id.split("_")[0]);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "reservationId 추출 실패: " + id);
        }
    }
}
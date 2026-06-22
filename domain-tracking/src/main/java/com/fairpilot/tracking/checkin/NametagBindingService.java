package com.fairpilot.tracking.checkin;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.fairpilot.tracking.domain.NameTagStatus;
import com.fairpilot.tracking.repository.NameTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NametagBindingService {

    private final NameTagRepository nameTagRepository;
    private final CheckinLogRepository checkinLogRepository;

    /** QR 스캔 바인딩 (AVAILABLE → ISSUED) */
    @Transactional
    public void bind(String nameTagToken, Long attendeeId, Long reservationId,
                     Long exhibitionId, Long staffUserId) {

        var nameTag = nameTagRepository.findByToken(nameTagToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "네임태그를 찾을 수 없습니다."));

        if (nameTag.getStatus() != NameTagStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "이미 사용 중이거나 무효화된 네임태그입니다.");
        }

        nameTagRepository.bindToAttendee(nameTag.getId(), attendeeId, NameTagStatus.ISSUED);

        checkinLogRepository.save(CheckinLog.builder()
                .exhibitionId(exhibitionId)
                .reservationId(reservationId)
                .attendeeId(attendeeId)
                .nameTagId(nameTag.getId())
                .checkinMethod(CheckinMethod.QR_SELF)
                .checkedInByUserId(staffUserId)
                .build());
    }

    /** 수기 체크인 (인터넷 오류 대응) */
    @Transactional
    public void bindManual(Long attendeeId, Long reservationId,
                           Long exhibitionId, Long staffUserId, String memo) {

        checkinLogRepository.save(CheckinLog.builder()
                .exhibitionId(exhibitionId)
                .reservationId(reservationId)
                .attendeeId(attendeeId)
                .nameTagId(null)
                .checkinMethod(CheckinMethod.ONSITE_MANUAL)
                .checkedInByUserId(staffUserId)
                .memo(memo)
                .build());
    }
}
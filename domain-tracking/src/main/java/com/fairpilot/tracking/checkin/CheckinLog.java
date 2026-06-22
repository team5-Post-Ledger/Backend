package com.fairpilot.tracking.checkin;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkin_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckinLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long exhibitionId;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private Long attendeeId;

    @Column
    private Long nameTagId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckinMethod checkinMethod;

    @Column(nullable = false)
    private Long checkedInByUserId;

    @Column(nullable = false)
    private LocalDateTime checkedInAt;

    @Column(length = 500)
    private String memo;

    @Builder
    public CheckinLog(Long exhibitionId, Long reservationId, Long attendeeId,
                      Long nameTagId, CheckinMethod checkinMethod,
                      Long checkedInByUserId, String memo) {
        this.exhibitionId = exhibitionId;
        this.reservationId = reservationId;
        this.attendeeId = attendeeId;
        this.nameTagId = nameTagId;
        this.checkinMethod = checkinMethod;
        this.checkedInByUserId = checkedInByUserId;
        this.checkedInAt = LocalDateTime.now();
        this.memo = memo;
    }
}
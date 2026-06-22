package com.fairpilot.tracking.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckinLogRepository extends JpaRepository<CheckinLog, Long> {

    List<CheckinLog> findAllByExhibitionIdAndAttendeeId(Long exhibitionId, Long attendeeId);
}
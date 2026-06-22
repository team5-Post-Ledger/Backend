package com.fairpilot.tracking.repository;

import com.fairpilot.tracking.domain.NameTag;
import com.fairpilot.tracking.domain.NameTagStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NameTagRepository extends JpaRepository<NameTag, Long> {

    Optional<NameTag> findByToken(String token);

    @Modifying
    @Query("UPDATE NameTag n SET n.attendeeId = :attendeeId, n.status = :status WHERE n.id = :id")
    void bindToAttendee(@Param("id") Long id,
                        @Param("attendeeId") Long attendeeId,
                        @Param("status") NameTagStatus status);
}
package com.gymrat.repository;

import com.gymrat.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByMemberIdOrderByCheckInTimeDesc(Long memberId);
    List<Attendance> findByCheckInTimeBetweenOrderByCheckInTimeDesc(LocalDateTime start, LocalDateTime end);
    long countByMemberId(Long memberId);
}

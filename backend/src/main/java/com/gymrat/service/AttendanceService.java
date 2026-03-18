package com.gymrat.service;

import com.gymrat.dto.AttendanceDto;
import com.gymrat.entity.Attendance;
import com.gymrat.entity.Member;
import com.gymrat.repository.AttendanceRepository;
import com.gymrat.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;

    public AttendanceDto checkIn(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found: " + memberId));
        Attendance attendance = Attendance.builder()
                .member(member)
                .checkInTime(LocalDateTime.now())
                .build();
        return toDto(attendanceRepository.save(attendance));
    }

    public List<AttendanceDto> getByMember(Long memberId) {
        return attendanceRepository.findByMemberIdOrderByCheckInTimeDesc(memberId)
                .stream().map(this::toDto).toList();
    }

    public List<AttendanceDto> getAll(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return attendanceRepository.findByCheckInTimeBetweenOrderByCheckInTimeDesc(start, end)
                    .stream().map(this::toDto).toList();
        }
        return attendanceRepository.findAll().stream()
                .sorted((a, b) -> b.getCheckInTime().compareTo(a.getCheckInTime()))
                .map(this::toDto).toList();
    }

    public void delete(Long id) {
        if (!attendanceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found: " + id);
        }
        attendanceRepository.deleteById(id);
    }

    private AttendanceDto toDto(Attendance a) {
        return new AttendanceDto(a.getId(), a.getMember().getId(), a.getMember().getName(), a.getCheckInTime());
    }
}

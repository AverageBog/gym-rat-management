package com.gymrat.repository;

import com.gymrat.entity.MemberNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberNoteRepository extends JpaRepository<MemberNote, Long> {
    List<MemberNote> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}

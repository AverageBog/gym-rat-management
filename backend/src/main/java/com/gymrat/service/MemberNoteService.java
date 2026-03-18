package com.gymrat.service;

import com.gymrat.dto.MemberNoteDto;
import com.gymrat.entity.Member;
import com.gymrat.entity.MemberNote;
import com.gymrat.repository.MemberNoteRepository;
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
public class MemberNoteService {

    private final MemberNoteRepository noteRepository;
    private final MemberRepository memberRepository;

    public List<MemberNoteDto> getByMember(Long memberId) {
        return noteRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(this::toDto).toList();
    }

    public MemberNoteDto addNote(Long memberId, String content) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found: " + memberId));
        MemberNote note = MemberNote.builder()
                .member(member)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        return toDto(noteRepository.save(note));
    }

    public MemberNoteDto updateNote(Long id, String content) {
        MemberNote note = noteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found: " + id));
        note.setContent(content);
        return toDto(noteRepository.save(note));
    }

    public void deleteNote(Long id) {
        if (!noteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found: " + id);
        }
        noteRepository.deleteById(id);
    }

    private MemberNoteDto toDto(MemberNote note) {
        return new MemberNoteDto(note.getId(), note.getMember().getId(), note.getContent(), note.getCreatedAt());
    }
}

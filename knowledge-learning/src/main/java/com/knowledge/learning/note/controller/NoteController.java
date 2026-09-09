package com.knowledge.learning.note.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.note.converter.NoteConverter;
import com.knowledge.learning.note.dto.CreateNoteDTO;
import com.knowledge.learning.note.dto.RenameNoteDTO;
import com.knowledge.learning.note.dto.UpdateNoteDTO;
import com.knowledge.learning.note.service.NoteService;
import com.knowledge.learning.note.vo.NoteVO;
import com.knowledge.security.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/learning/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public Result<NoteVO> create(@Valid @RequestBody CreateNoteDTO request,
                                 HttpServletRequest servletRequest) {
        return Result.ok(NoteConverter.toVO(noteService.create(UserContext.getUserId(), request)),
                requestId(servletRequest));
    }

    @GetMapping
    public Result<List<NoteVO>> list(@RequestParam(required = false) String courseId,
                                     @RequestParam(required = false) String chapterId,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
                                     HttpServletRequest servletRequest) {
        return Result.ok(noteService.list(UserContext.getUserId(), courseId, chapterId, keyword, limit)
                .stream().map(NoteConverter::toVO).toList(), requestId(servletRequest));
    }

    @GetMapping("/{noteId}")
    public Result<NoteVO> get(@PathVariable String noteId,
                              HttpServletRequest servletRequest) {
        return Result.ok(NoteConverter.toVO(noteService.get(UserContext.getUserId(), noteId)),
                requestId(servletRequest));
    }

    @PutMapping("/{noteId}")
    public Result<NoteVO> update(@PathVariable String noteId,
                                 @Valid @RequestBody UpdateNoteDTO request, HttpServletRequest servletRequest) {
        return Result.ok(NoteConverter.toVO(noteService.update(UserContext.getUserId(), noteId, request)),
                requestId(servletRequest));
    }

    @PatchMapping("/{noteId}/title")
    public Result<NoteVO> rename(@PathVariable String noteId,
                                 @Valid @RequestBody RenameNoteDTO request, HttpServletRequest servletRequest) {
        return Result.ok(NoteConverter.toVO(noteService.rename(UserContext.getUserId(), noteId, request)),
                requestId(servletRequest));
    }

    @DeleteMapping("/{noteId}")
    public Result<Void> delete(@PathVariable String noteId,
                               @RequestParam @PositiveOrZero int version, HttpServletRequest servletRequest) {
        noteService.delete(UserContext.getUserId(), noteId, version);
        return Result.ok(null, requestId(servletRequest));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}

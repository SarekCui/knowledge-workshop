package com.knowledge.learning.note.controller;

import com.knowledge.api.common.Result;
import com.knowledge.api.common.PageVO;
import com.knowledge.api.learning.dto.CreateNoteCommentDTO;
import com.knowledge.common.converter.PageConverter;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.note.converter.NoteConverter;
import com.knowledge.learning.note.converter.NoteEngagementConverter;
import com.knowledge.learning.note.dto.ChangeNoteStatusDTO;
import com.knowledge.learning.note.dto.CreateNoteDTO;
import com.knowledge.learning.note.dto.RenameNoteDTO;
import com.knowledge.learning.note.dto.UpdateNoteDTO;
import com.knowledge.learning.note.enums.NoteSort;
import com.knowledge.learning.note.service.NoteCommentService;
import com.knowledge.learning.note.service.NoteCommentLikeService;
import com.knowledge.learning.note.service.NoteEngagementService;
import com.knowledge.learning.note.service.NoteQueryService;
import com.knowledge.learning.note.service.NoteService;
import com.knowledge.learning.note.vo.NoteCommentVO;
import com.knowledge.learning.note.vo.NoteEngagementVO;
import com.knowledge.learning.note.vo.NoteVO;
import com.knowledge.security.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import jakarta.annotation.Resource;
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

    @Resource
    private NoteService noteService;
    @Resource
    private NoteQueryService queryService;
    @Resource
    private NoteEngagementService engagementService;
    @Resource
    private NoteCommentService commentService;
    @Resource
    private NoteCommentLikeService commentLikeService;

    @GetMapping("/public")
    public Result<PageVO<NoteVO>> publicPage(@RequestParam(required = false) @Size(max = 64) String courseId,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 20) String tag,
            @RequestParam(defaultValue = "LATEST") NoteSort sort,
            @RequestParam(defaultValue = "1") @Min(1) @Max(1000) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize, HttpServletRequest request) {
        return Result.ok(PageConverter.toVO(queryService.publicPageForUser(UserContext.getUserIdOrNull(), courseId,
                        keyword, tag, sort, pageNo, pageSize),
                NoteConverter::toVO), requestId(request));
    }

    @GetMapping("/public/{noteId}")
    public Result<NoteVO> publicDetail(@PathVariable String noteId, HttpServletRequest request) {
        return Result.ok(NoteConverter.toVO(queryService.getPublicForUser(UserContext.getUserIdOrNull(), noteId)),
                requestId(request));
    }

    @GetMapping("/mine")
    public Result<PageVO<NoteVO>> mine(@RequestParam(required = false) @Size(max = 64) String courseId,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) @Max(1000) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize, HttpServletRequest request) {
        return Result.ok(PageConverter.toVO(queryService.mine(UserContext.getUserId(), courseId, keyword, pageNo, pageSize), NoteConverter::toVO), requestId(request));
    }

    @GetMapping("/liked")
    public Result<PageVO<NoteVO>> liked(@RequestParam(defaultValue = "1") @Min(1) @Max(1000) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize, HttpServletRequest request) {
        return Result.ok(PageConverter.toVO(queryService.liked(UserContext.getUserId(), pageNo, pageSize),
                NoteConverter::toVO), requestId(request));
    }

    @GetMapping("/favorites")
    public Result<PageVO<NoteVO>> favorites(@RequestParam(defaultValue = "1") @Min(1) @Max(1000) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize, HttpServletRequest request) {
        return Result.ok(PageConverter.toVO(queryService.favorited(UserContext.getUserId(), pageNo, pageSize),
                NoteConverter::toVO), requestId(request));
    }

    @GetMapping("/public/{noteId}/engagement")
    public Result<NoteEngagementVO> engagement(@PathVariable String noteId, HttpServletRequest request) {
        return Result.ok(NoteEngagementConverter.toVO(engagementService.get(UserContext.getUserIdOrNull(), noteId)),
                requestId(request));
    }

    @PutMapping("/public/{noteId}/likes")
    public Result<NoteEngagementVO> like(@PathVariable String noteId, HttpServletRequest request) {
        return Result.ok(NoteEngagementConverter.toVO(engagementService.like(UserContext.getUserId(), noteId)),
                requestId(request));
    }

    @DeleteMapping("/public/{noteId}/likes")
    public Result<NoteEngagementVO> unlike(@PathVariable String noteId, HttpServletRequest request) {
        return Result.ok(NoteEngagementConverter.toVO(engagementService.unlike(UserContext.getUserId(), noteId)),
                requestId(request));
    }

    @PutMapping("/public/{noteId}/favorites")
    public Result<NoteEngagementVO> favorite(@PathVariable String noteId, HttpServletRequest request) {
        return Result.ok(NoteEngagementConverter.toVO(engagementService.favorite(UserContext.getUserId(), noteId)),
                requestId(request));
    }

    @DeleteMapping("/public/{noteId}/favorites")
    public Result<NoteEngagementVO> unfavorite(@PathVariable String noteId, HttpServletRequest request) {
        return Result.ok(NoteEngagementConverter.toVO(engagementService.unfavorite(UserContext.getUserId(), noteId)),
                requestId(request));
    }

    @GetMapping("/public/{noteId}/comments")
    public Result<PageVO<NoteCommentVO>> comments(@PathVariable String noteId,
            @RequestParam(defaultValue = "1") @Min(1) @Max(1000) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize, HttpServletRequest request) {
        return Result.ok(PageConverter.toVO(commentService.page(UserContext.getUserIdOrNull(), noteId, pageNo, pageSize),
                NoteEngagementConverter::toVO), requestId(request));
    }

    @PostMapping("/public/{noteId}/comments")
    public Result<NoteCommentVO> comment(@PathVariable String noteId,
            @Valid @RequestBody CreateNoteCommentDTO body, HttpServletRequest request) {
        return Result.ok(NoteEngagementConverter.toVO(commentService.create(UserContext.getUserId(), noteId, body)),
                requestId(request));
    }

    @PutMapping("/comments/{commentId}/likes")
    public Result<NoteCommentVO> likeComment(@PathVariable String commentId, HttpServletRequest request) {
        return Result.ok(NoteEngagementConverter.toVO(commentLikeService.like(UserContext.getUserId(), commentId)),
                requestId(request));
    }

    @DeleteMapping("/comments/{commentId}/likes")
    public Result<NoteCommentVO> unlikeComment(@PathVariable String commentId, HttpServletRequest request) {
        return Result.ok(NoteEngagementConverter.toVO(commentLikeService.unlike(UserContext.getUserId(), commentId)),
                requestId(request));
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable String commentId,
            @RequestParam @PositiveOrZero int version, HttpServletRequest request) {
        commentService.delete(UserContext.getUserId(), commentId, version);
        return Result.ok(null, requestId(request));
    }

    @PatchMapping("/{noteId}/status")
    public Result<NoteVO> status(@PathVariable String noteId, @Valid @RequestBody ChangeNoteStatusDTO body,
            HttpServletRequest request) {
        return Result.ok(NoteConverter.toVO(noteService.changeStatus(UserContext.getUserId(), noteId, body)), requestId(request));
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
                                     @RequestParam(required = false) @Size(max = 100) String keyword,
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

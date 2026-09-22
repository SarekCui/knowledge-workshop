package com.knowledge.learning.note.controller;

import com.knowledge.api.common.Result;
import com.knowledge.api.learning.dto.AgentCommentContextDTO;
import com.knowledge.api.learning.dto.CreateNoteCommentDTO;
import com.knowledge.learning.note.service.NoteCommentService;
import jakarta.validation.Valid;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notes")
public class AgentInternalNoteController {

    @Resource
    private NoteCommentService commentService;

    @PostMapping("/{noteId}/comments")
    @PreAuthorize("hasAuthority('SCOPE_learning.comment.write')")
    public Result<Void> createComment(@PathVariable String noteId,
            @Valid @RequestBody CreateNoteCommentDTO request) {
        commentService.createAgentReply(noteId, request);
        return Result.ok(null, null);
    }

    /** Returns note title/content and the source comment so the agent can compose a grounded reply. */
    @GetMapping("/{noteId}/comments/{commentId}/agent-context")
    @PreAuthorize("hasAuthority('SCOPE_learning.comment.read')")
    public Result<AgentCommentContextDTO> context(@PathVariable String noteId, @PathVariable String commentId) {
        return Result.ok(commentService.loadAgentContext(noteId, commentId), null);
    }
}

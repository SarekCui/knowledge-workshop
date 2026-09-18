package com.knowledge.learning.note.controller;

import com.knowledge.api.common.Result;
import com.knowledge.api.learning.dto.PublishAgentCommentReplyDTO;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.note.converter.NoteEngagementConverter;
import com.knowledge.learning.note.dao.mapper.NoteCommentMapper;
import com.knowledge.learning.note.dao.mapper.NoteMapper;
import com.knowledge.learning.note.dao.model.NoteCommentDO;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.service.NoteCommentService;
import com.knowledge.learning.note.vo.NoteCommentVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/agent/comments")
@PreAuthorize("hasRole('AGENT')")
public class AgentInternalNoteController {

    @Autowired
    private NoteCommentService commentService;
    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private NoteCommentMapper commentMapper;

    @PostMapping("/ai-replies")
    public Result<NoteCommentVO> publishReply(@RequestBody PublishAgentCommentReplyDTO request,
            HttpServletRequest servletRequest) {
        return Result.ok(NoteEngagementConverter.toVO(commentService.publishAgentReply(request)),
                String.valueOf(servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
    }

    /** Returns note title/content and the source comment so the agent can compose a grounded reply. */
    @GetMapping("/context")
    public Result<Map<String, Object>> context(@RequestParam String noteId, @RequestParam String commentId) {
        NoteDO note = noteMapper.selectById(noteId);
        NoteCommentDO comment = commentMapper.selectById(commentId);
        if (note == null || comment == null) {
            throw com.knowledge.common.exception.BusinessException.notFound("笔记或评论不存在");
        }
        return Result.ok(Map.of(
                "noteTitle", note.getTitle() == null ? "" : note.getTitle(),
                "noteContent", note.getContent() == null ? "" : note.getContent(),
                "commentContent", comment.getContent() == null ? "" : comment.getContent()), null);
    }
}

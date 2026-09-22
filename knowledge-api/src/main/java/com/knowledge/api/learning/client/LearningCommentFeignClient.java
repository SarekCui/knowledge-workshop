package com.knowledge.api.learning.client;

import com.knowledge.api.common.Result;
import com.knowledge.api.learning.dto.AgentCommentContextDTO;
import com.knowledge.api.learning.dto.CreateNoteCommentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Remote contract for Learning's internal Note-comment capability. */
@FeignClient(name = "knowledge-learning", contextId = "learningCommentFeignClient", path = "/internal/notes")
public interface LearningCommentFeignClient {

    @GetMapping("/{noteId}/comments/{commentId}/agent-context")
    Result<AgentCommentContextDTO> fetchContext(
            @PathVariable("noteId") String noteId,
            @PathVariable("commentId") String commentId);

    @PostMapping("/{noteId}/comments")
    Result<Void> createComment(
            @PathVariable("noteId") String noteId,
            @RequestBody CreateNoteCommentDTO request);
}

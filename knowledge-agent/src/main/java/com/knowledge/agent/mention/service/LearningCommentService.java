package com.knowledge.agent.mention.service;

import com.knowledge.api.learning.client.LearningCommentFeignClient;
import com.knowledge.api.common.Result;
import com.knowledge.api.learning.dto.AgentCommentContextDTO;
import com.knowledge.api.learning.dto.CreateNoteCommentDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/** Application-facing wrapper around Learning's remote comment contract. */
@Service
public class LearningCommentService {

    @Resource
    private LearningCommentFeignClient learningCommentFeignClient;

    public AgentCommentContextDTO fetchContext(String noteId, String sourceCommentId) {
        Result<AgentCommentContextDTO> result = learningCommentFeignClient.fetchContext(noteId, sourceCommentId);
        if (result == null || result.data() == null) {
            throw new IllegalStateException("empty comment context response");
        }
        return result.data();
    }

    public void publishReply(String noteId, String sourceCommentId, String content) {
        CreateNoteCommentDTO request = new CreateNoteCommentDTO(
                "agent:comment-reply:" + sourceCommentId, sourceCommentId, content);
        Result<Void> result = learningCommentFeignClient.createComment(noteId, request);
        if (result == null) {
            throw new IllegalStateException("empty comment publish response");
        }
    }
}

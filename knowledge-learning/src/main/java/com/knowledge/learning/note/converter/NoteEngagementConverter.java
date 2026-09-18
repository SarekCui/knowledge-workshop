package com.knowledge.learning.note.converter;

import com.knowledge.learning.note.bo.NoteCommentBO;
import com.knowledge.learning.note.bo.NoteEngagementBO;
import com.knowledge.learning.note.dao.model.NoteCommentDO;
import com.knowledge.learning.note.vo.NoteCommentVO;
import com.knowledge.learning.note.vo.NoteEngagementVO;

public final class NoteEngagementConverter {

    private NoteEngagementConverter() {
    }

    public static NoteCommentBO toBO(NoteCommentDO source, String viewerUserId) {
        return toBO(source, viewerUserId, false);
    }

    public static NoteCommentBO toBO(NoteCommentDO source, String viewerUserId, boolean liked) {
        return new NoteCommentBO(source.getId(), source.getNoteId(), source.getUserId(), source.getAuthorType(),
                source.getParentCommentId(), source.getSourceCommentId(), source.getContent(), count(source.getLikeCount()), liked,
                source.getUserId().equals(viewerUserId), source.getVersion(),
                source.getCreatedAt(), source.getUpdatedAt());
    }

    public static NoteCommentVO toVO(NoteCommentBO source) {
        return new NoteCommentVO(source.id(), source.noteId(), source.authorId(), source.authorType().name(), source.parentCommentId(), source.sourceCommentId(),
                source.content(), source.likeCount(), source.liked(), source.owned(), source.version(), source.createdAt(), source.updatedAt());
    }

    public static NoteEngagementVO toVO(NoteEngagementBO source) {
        return new NoteEngagementVO(source.likeCount(), source.favoriteCount(), source.commentCount(),
                source.liked(), source.favorited());
    }

    private static long count(Long value) {
        return value == null ? 0 : value;
    }
}

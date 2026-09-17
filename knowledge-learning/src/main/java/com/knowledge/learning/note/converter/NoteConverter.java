package com.knowledge.learning.note.converter;

import com.knowledge.learning.note.bo.NoteBO;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.vo.NoteVO;
import java.util.List;

public final class NoteConverter {

    private NoteConverter() {
    }

    public static NoteBO toBO(NoteDO source) {
        return toBO(source, List.of());
    }

    public static NoteBO toBO(NoteDO source, List<String> tags) {
        return new NoteBO(source.getId(), source.getCourseId(), source.getChapterId(), source.getTitle(),
                source.getContent(), source.getVideoPositionMs(), source.getVersion(), source.getCreatedAt(),
                source.getUpdatedAt(), source.getUserId(), source.getStatus(), source.getPublishedAt(),
                count(source.getLikeCount()), count(source.getFavoriteCount()), count(source.getCommentCount()),
                List.copyOf(tags), false, false);
    }

    public static NoteVO toVO(NoteBO source) {
        return new NoteVO(source.id(), source.courseId(), source.chapterId(), source.title(), source.content(),
                source.videoPositionMs(), source.version(), source.createdAt(), source.updatedAt(),
                source.authorId(), source.status(), source.publishedAt(), source.likeCount(),
                source.favoriteCount(), source.commentCount(), source.tags(), source.liked(), source.favorited());
    }

    private static long count(Long value) {
        return value == null ? 0 : value;
    }
}

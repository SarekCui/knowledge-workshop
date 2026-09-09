package com.knowledge.learning.note.converter;

import com.knowledge.learning.note.bo.NoteBO;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.vo.NoteVO;

public final class NoteConverter {

    private NoteConverter() {
    }

    public static NoteBO toBO(NoteDO source) {
        return new NoteBO(source.getId(), source.getCourseId(), source.getChapterId(), source.getTitle(),
                source.getContent(), source.getVideoPositionMs(), source.getVersion(), source.getCreatedAt(),
                source.getUpdatedAt());
    }

    public static NoteVO toVO(NoteBO source) {
        return new NoteVO(source.id(), source.courseId(), source.chapterId(), source.title(), source.content(),
                source.videoPositionMs(), source.version(), source.createdAt(), source.updatedAt());
    }
}

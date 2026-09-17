package com.knowledge.learning.note.converter;

import com.knowledge.learning.note.bo.NoteImageAccessBO;
import com.knowledge.learning.note.bo.NoteImageBO;
import com.knowledge.learning.note.vo.NoteImageAccessVO;
import com.knowledge.learning.note.vo.NoteImageVO;

public final class NoteImageConverter {
    private NoteImageConverter() {
    }

    public static NoteImageVO toVO(NoteImageBO source) {
        return new NoteImageVO(source.id(), source.markdownUrl(), source.width(), source.height());
    }

    public static NoteImageAccessVO toVO(NoteImageAccessBO source) {
        return new NoteImageAccessVO(source.url());
    }
}

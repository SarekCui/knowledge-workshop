package com.knowledge.learning.note.storage;

import com.knowledge.learning.note.bo.NormalizedNoteImageBO;

public interface NoteImageObjectStorage {
    String put(NormalizedNoteImageBO image);

    String temporaryUrl(String objectKey);

    boolean delete(String objectKey);
}

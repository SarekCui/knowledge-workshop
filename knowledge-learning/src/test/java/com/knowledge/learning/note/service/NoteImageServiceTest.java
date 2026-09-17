package com.knowledge.learning.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.note.dao.mapper.NoteImageMapper;
import com.knowledge.learning.note.dao.mapper.NoteMapper;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.dao.model.NoteImageDO;
import com.knowledge.learning.note.enums.NoteImageStatus;
import com.knowledge.learning.note.enums.NoteStatus;
import com.knowledge.learning.note.storage.NoteImageObjectStorage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NoteImageServiceTest {
    private static final String IMAGE_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void extractsUniqueInternalImagesButIgnoresCodeSamples() {
        var service = service(mock(NoteImageMapper.class), mock(NoteMapper.class), mock(NoteImageObjectStorage.class));
        String markdown = """
                ![架构图](/api/learning/note-images/11111111-1111-1111-1111-111111111111)
                `![inline](/api/learning/note-images/22222222-2222-2222-2222-222222222222)`
                ```md
                ![sample](/api/learning/note-images/33333333-3333-3333-3333-333333333333)
                ```
                ![重复](/api/learning/note-images/11111111-1111-1111-1111-111111111111)
                """;

        assertThat(service.extractReferences(markdown)).isEqualTo(Set.of(IMAGE_ID));
    }

    @Test
    void bindsOwnedImagesAndReleasesRemovedReferences() {
        NoteImageMapper mapper = mock(NoteImageMapper.class);
        NoteImageDO image = image("user-1", null, NoteImageStatus.TEMP);
        when(mapper.selectByIds(Set.of(IMAGE_ID))).thenReturn(List.of(image));
        when(mapper.bind(eq(IMAGE_ID), eq("user-1"), eq("note-1"), any())).thenReturn(1);
        var service = service(mapper, mock(NoteMapper.class), mock(NoteImageObjectStorage.class));

        service.syncReferences("user-1", "note-1",
                "![架构图](/api/learning/note-images/" + IMAGE_ID + ")");

        verify(mapper).bind(eq(IMAGE_ID), eq("user-1"), eq("note-1"), any());
        verify(mapper).releaseMissing(eq("note-1"), eq(List.of(IMAGE_ID)), any(), any());
    }

    @Test
    void refusesImageOwnedByAnotherUserWithoutMutatingBindings() {
        NoteImageMapper mapper = mock(NoteImageMapper.class);
        when(mapper.selectByIds(Set.of(IMAGE_ID)))
                .thenReturn(List.of(image("other-user", null, NoteImageStatus.TEMP)));
        var service = service(mapper, mock(NoteMapper.class), mock(NoteImageObjectStorage.class));

        assertThatThrownBy(() -> service.syncReferences("user-1", "note-1",
                "![图](/api/learning/note-images/" + IMAGE_ID + ")"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权使用");
        verify(mapper, never()).bind(any(), any(), any(), any());
        verify(mapper, never()).releaseMissing(any(), anyList(), any(), any());
    }

    @Test
    void onlyOwnerOrPublicNoteReaderCanResolveTemporaryUrl() {
        NoteImageMapper imageMapper = mock(NoteImageMapper.class);
        NoteMapper noteMapper = mock(NoteMapper.class);
        NoteImageObjectStorage storage = mock(NoteImageObjectStorage.class);
        NoteImageDO image = image("owner", "note-1", NoteImageStatus.BOUND);
        when(imageMapper.selectById(IMAGE_ID)).thenReturn(image);
        when(storage.temporaryUrl("note-images/object.png")).thenReturn("https://storage/signed");
        var service = service(imageMapper, noteMapper, storage);

        assertThat(service.access("owner", IMAGE_ID).url()).isEqualTo("https://storage/signed");

        NoteDO note = new NoteDO();
        note.setDeleted(0);
        note.setStatus(NoteStatus.PRIVATE);
        when(noteMapper.selectById("note-1")).thenReturn(note);
        assertThatThrownBy(() -> service.access("reader", IMAGE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");

        note.setStatus(NoteStatus.PUBLIC);
        assertThat(service.access("reader", IMAGE_ID).url()).isEqualTo("https://storage/signed");
    }

    private NoteImageDO image(String userId, String noteId, NoteImageStatus status) {
        NoteImageDO image = new NoteImageDO();
        image.setId(IMAGE_ID);
        image.setUserId(userId);
        image.setNoteId(noteId);
        image.setObjectKey("note-images/object.png");
        image.setStatus(status);
        return image;
    }

    private NoteImageService service(NoteImageMapper imageMapper, NoteMapper noteMapper,
            NoteImageObjectStorage storage) {
        var service = new NoteImageService();
        ReflectionTestUtils.setField(service, "noteImageMapper", imageMapper);
        ReflectionTestUtils.setField(service, "noteMapper", noteMapper);
        ReflectionTestUtils.setField(service, "fileService", new NoteImageFileService());
        ReflectionTestUtils.setField(service, "objectStorage", storage);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        return service;
    }
}

package com.knowledge.learning.note.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.note.bo.NoteImageAccessBO;
import com.knowledge.learning.note.bo.NoteImageBO;
import com.knowledge.learning.note.bo.NormalizedNoteImageBO;
import com.knowledge.learning.note.dao.mapper.NoteImageMapper;
import com.knowledge.learning.note.dao.mapper.NoteMapper;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.dao.model.NoteImageDO;
import com.knowledge.learning.note.enums.NoteImageStatus;
import com.knowledge.learning.note.enums.NoteStatus;
import com.knowledge.learning.note.storage.NoteImageObjectStorage;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NoteImageService {
    private static final int MAX_IMAGES_PER_NOTE = 20;
    private static final int MAX_TEMPORARY_IMAGES = 20;
    private static final Duration TEMPORARY_TTL = Duration.ofHours(24);
    private static final Duration CLEANUP_RETRY = Duration.ofHours(1);
    private static final Pattern INTERNAL_IMAGE = Pattern.compile(
            "!\\[[^\\]]*]\\(/api/learning/note-images/([0-9a-fA-F-]{36})(?:\\s+\"[^\"]*\")?\\)");

    @Autowired
    private NoteImageMapper noteImageMapper;
    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private NoteImageFileService fileService;
    @Autowired
    private NoteImageObjectStorage objectStorage;
    @Autowired
    private Clock clock;

    public NoteImageBO upload(String userId, MultipartFile file) {
        if (noteImageMapper.countTemporary(userId) >= MAX_TEMPORARY_IMAGES) {
            throw BusinessException.conflict("待使用的 Note 图片已达20张，请先保存或移除已有图片");
        }
        NormalizedNoteImageBO image = fileService.normalize(file);
        String objectKey = objectStorage.put(image);
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(clock);
        NoteImageDO record = new NoteImageDO();
        record.setId(id);
        record.setUserId(userId);
        record.setObjectKey(objectKey);
        record.setContentType(image.contentType());
        record.setFileSize((long) image.content().length);
        record.setWidth(image.width());
        record.setHeight(image.height());
        record.setStatus(NoteImageStatus.TEMP);
        record.setExpiresAt(now.plus(TEMPORARY_TTL));
        record.setVersion(0);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        try {
            noteImageMapper.insert(record);
        } catch (RuntimeException exception) {
            objectStorage.delete(objectKey);
            throw exception;
        }
        return new NoteImageBO(id, markdownUrl(id), image.width(), image.height());
    }

    public NoteImageAccessBO access(String userId, String imageId) {
        NoteImageDO image = noteImageMapper.selectById(imageId);
        if (image == null || image.getStatus() == NoteImageStatus.DELETING) {
            throw BusinessException.notFound("Note 图片不存在");
        }
        if (!Objects.equals(userId, image.getUserId())) {
            NoteDO note = image.getNoteId() == null ? null : noteMapper.selectById(image.getNoteId());
            if (note == null || note.getDeleted() != 0 || note.getStatus() != NoteStatus.PUBLIC) {
                throw BusinessException.notFound("Note 图片不存在");
            }
        }
        return new NoteImageAccessBO(objectStorage.temporaryUrl(image.getObjectKey()));
    }

    public void syncReferences(String userId, String noteId, String content) {
        Set<String> ids = extractReferences(content);
        if (ids.size() > MAX_IMAGES_PER_NOTE) {
            throw BusinessException.badRequest("每篇 Note 最多引用20张站内图片");
        }
        if (!ids.isEmpty()) {
            List<NoteImageDO> images = noteImageMapper.selectByIds(ids);
            if (images.size() != ids.size()) throw BusinessException.badRequest("Note 正文包含无效图片");
            for (NoteImageDO image : images) {
                if (!userId.equals(image.getUserId()) || image.getStatus() == NoteImageStatus.DELETING
                        || image.getNoteId() != null && !noteId.equals(image.getNoteId())) {
                    throw BusinessException.forbidden("无权使用该 Note 图片");
                }
            }
            LocalDateTime now = LocalDateTime.now(clock);
            for (String id : ids) {
                if (noteImageMapper.bind(id, userId, noteId, now) != 1) {
                    throw BusinessException.conflict("Note 图片状态已变化，请刷新后重试");
                }
            }
        }
        LocalDateTime now = LocalDateTime.now(clock);
        noteImageMapper.releaseMissing(noteId, List.copyOf(ids), now.plus(TEMPORARY_TTL), now);
    }

    public void releaseAll(String noteId) {
        LocalDateTime now = LocalDateTime.now(clock);
        noteImageMapper.releaseMissing(noteId, List.of(), now.plus(TEMPORARY_TTL), now);
    }

    public int cleanupExpired() {
        LocalDateTime now = LocalDateTime.now(clock);
        noteImageMapper.recoverStaleClaims(now.minusMinutes(10), now);
        String token = UUID.randomUUID().toString();
        noteImageMapper.claimExpired(token, now, 100);
        int deleted = 0;
        for (NoteImageDO image : noteImageMapper.selectClaimed(token)) {
            if (objectStorage.delete(image.getObjectKey())) {
                deleted += noteImageMapper.deleteClaimed(image.getId(), token);
            } else {
                noteImageMapper.retryClaimed(image.getId(), token, now.plus(CLEANUP_RETRY), now);
            }
        }
        return deleted;
    }

    Set<String> extractReferences(String content) {
        StringBuilder visibleMarkdown = new StringBuilder();
        boolean fenced = false;
        for (String line : content.split("\\R", -1)) {
            String trimmed = line.stripLeading();
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                fenced = !fenced;
                continue;
            }
            if (!fenced) visibleMarkdown.append(line.replaceAll("`[^`]*`", "")).append('\n');
        }
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = INTERNAL_IMAGE.matcher(visibleMarkdown);
        while (matcher.find()) ids.add(matcher.group(1));
        return ids;
    }

    private String markdownUrl(String id) {
        return "/api/learning/note-images/" + id;
    }
}

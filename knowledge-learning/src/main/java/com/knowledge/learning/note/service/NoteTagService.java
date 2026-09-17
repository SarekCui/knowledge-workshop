package com.knowledge.learning.note.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.note.dao.mapper.NoteTagMapper;
import com.knowledge.learning.note.dao.model.NoteTagDO;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoteTagService {
    private static final int MAX_TAGS = 5;
    private static final int MAX_TAG_LENGTH = 20;

    @Autowired
    private NoteTagMapper noteTagMapper;
    @Autowired
    private Clock clock;

    public List<String> normalize(List<String> tags) {
        if (tags == null || tags.isEmpty()) return List.of();
        if (tags.size() > MAX_TAGS) throw BusinessException.badRequest("每篇 Note 最多设置5个技术标签");
        Map<String, String> unique = new LinkedHashMap<>();
        for (String value : tags) {
            if (value == null) throw BusinessException.badRequest("技术标签不能为空");
            String display = Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ");
            if (display.isEmpty() || display.length() > MAX_TAG_LENGTH || display.chars().anyMatch(Character::isISOControl)) {
                throw BusinessException.badRequest("技术标签长度须为1—20个字符且不能包含控制字符");
            }
            unique.putIfAbsent(normalizedName(display), display);
        }
        if (unique.size() > MAX_TAGS) throw BusinessException.badRequest("每篇 Note 最多设置5个技术标签");
        return List.copyOf(unique.values());
    }

    public String normalizeFilter(String tag) {
        if (tag == null || tag.isBlank()) return null;
        return normalizedName(normalize(List.of(tag)).get(0));
    }

    public String normalizeSearchTerm(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return normalizedName(Normalizer.normalize(keyword, Normalizer.Form.NFKC).trim());
    }

    public void replace(String noteId, List<String> tags) {
        noteTagMapper.deleteByNoteId(noteId);
        LocalDateTime now = LocalDateTime.now(clock);
        for (int index = 0; index < tags.size(); index++) {
            String display = tags.get(index);
            noteTagMapper.insert(noteId, normalizedName(display), display, index, now);
        }
    }

    public List<String> find(String noteId) {
        return findByNoteIds(List.of(noteId)).getOrDefault(noteId, List.of());
    }

    public Map<String, List<String>> findByNoteIds(List<String> noteIds) {
        if (noteIds.isEmpty()) return Map.of();
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (NoteTagDO tag : noteTagMapper.selectByNoteIds(noteIds)) {
            result.computeIfAbsent(tag.getNoteId(), ignored -> new ArrayList<>()).add(tag.getDisplayName());
        }
        return result;
    }

    private String normalizedName(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}

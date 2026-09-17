package com.knowledge.learning.note.job;

import com.knowledge.learning.note.service.NoteImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoteImageCleanupJob {
    private static final Logger LOG = LoggerFactory.getLogger(NoteImageCleanupJob.class);

    @Autowired
    private NoteImageService noteImageService;

    @Scheduled(fixedDelayString = "${knowledge.learning.note-image.cleanup-delay:1h}")
    public void cleanup() {
        int deleted = noteImageService.cleanupExpired();
        if (deleted > 0) LOG.info("event=note_image_cleanup deleted={}", deleted);
    }
}

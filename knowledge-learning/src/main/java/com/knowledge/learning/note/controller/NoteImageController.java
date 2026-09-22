package com.knowledge.learning.note.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.note.converter.NoteImageConverter;
import com.knowledge.learning.note.service.NoteImageService;
import com.knowledge.learning.note.vo.NoteImageAccessVO;
import com.knowledge.learning.note.vo.NoteImageVO;
import com.knowledge.security.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/learning/note-images")
public class NoteImageController {
    @Resource
    private NoteImageService noteImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<NoteImageVO> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        return Result.ok(NoteImageConverter.toVO(noteImageService.upload(UserContext.getUserId(), file)),
                requestId(request));
    }

    @GetMapping("/{imageId}/access")
    public Result<NoteImageAccessVO> access(@PathVariable @Size(max = 64) String imageId,
            HttpServletRequest request) {
        return Result.ok(NoteImageConverter.toVO(noteImageService.access(UserContext.getUserIdOrNull(), imageId)),
                requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}

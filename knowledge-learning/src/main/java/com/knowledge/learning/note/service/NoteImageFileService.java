package com.knowledge.learning.note.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.note.bo.NormalizedNoteImageBO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NoteImageFileService {
    static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    static final int MAX_SOURCE_EDGE = 8192;
    static final long MAX_SOURCE_PIXELS = 24_000_000L;
    static final int MAX_OUTPUT_EDGE = 1600;

    public NormalizedNoteImageBO normalize(MultipartFile file) {
        if (file == null || file.isEmpty()) throw BusinessException.badRequest("Note 图片不能为空");
        if (file.getSize() > MAX_FILE_SIZE) throw BusinessException.badRequest("Note 图片不能超过5MB");
        try {
            byte[] input = file.getBytes();
            ImageType type = detect(input);
            if (!type.contentType.equals(file.getContentType())) {
                throw BusinessException.badRequest("图片声明类型与文件内容不一致");
            }
            BufferedImage source = readBounded(input);
            int targetWidth = source.getWidth();
            int targetHeight = source.getHeight();
            double scale = Math.min(1D, (double) MAX_OUTPUT_EDGE / Math.max(targetWidth, targetHeight));
            targetWidth = Math.max(1, (int) Math.round(targetWidth * scale));
            targetHeight = Math.max(1, (int) Math.round(targetHeight * scale));
            int imageType = type == ImageType.PNG ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage target = new BufferedImage(targetWidth, targetHeight, imageType);
            Graphics2D graphics = target.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(target, type.format, output)) {
                throw BusinessException.badRequest("图片编码不受支持");
            }
            return new NormalizedNoteImageBO(output.toByteArray(), type.contentType, type.extension,
                    targetWidth, targetHeight);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw BusinessException.badRequest("Note 图片无法读取");
        }
    }

    private BufferedImage readBounded(byte[] content) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw BusinessException.badRequest("仅支持 JPEG 或 PNG 图片");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_SOURCE_EDGE || height > MAX_SOURCE_EDGE
                        || (long) width * height > MAX_SOURCE_PIXELS) {
                    throw BusinessException.badRequest("图片尺寸过大，最长边不能超过8192像素且总像素不能超过2400万");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw BusinessException.badRequest("Note 图片无法解析");
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private ImageType detect(byte[] content) {
        if (content.length >= 3 && (content[0] & 0xff) == 0xff && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff) return ImageType.JPEG;
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (content.length >= signature.length) {
            boolean png = true;
            for (int index = 0; index < signature.length; index++) png &= content[index] == signature[index];
            if (png) return ImageType.PNG;
        }
        throw BusinessException.badRequest("仅支持 JPEG 或 PNG 图片");
    }

    private enum ImageType {
        JPEG("image/jpeg", "jpg", "jpeg"), PNG("image/png", "png", "png");
        private final String contentType;
        private final String extension;
        private final String format;

        ImageType(String contentType, String extension, String format) {
            this.contentType = contentType;
            this.extension = extension;
            this.format = format;
        }
    }
}

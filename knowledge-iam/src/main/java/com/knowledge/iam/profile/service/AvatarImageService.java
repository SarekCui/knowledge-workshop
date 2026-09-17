package com.knowledge.iam.profile.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.iam.profile.bo.AvatarImageBO;
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
public class AvatarImageService {

    static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    static final int MAX_SOURCE_EDGE = 4096;
    static final int OUTPUT_EDGE = 256;

    public AvatarImageBO normalize(MultipartFile file) {
        if (file == null || file.isEmpty()) throw BusinessException.badRequest("头像文件不能为空");
        if (file.getSize() > MAX_FILE_SIZE) throw BusinessException.badRequest("头像文件不能超过2MB");
        try {
            byte[] input = file.getBytes();
            ImageType type = detect(input);
            if (!type.contentType.equals(file.getContentType())) {
                throw BusinessException.badRequest("头像声明类型与文件内容不一致");
            }
            BufferedImage source = readBounded(input);
            int edge = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - edge) / 2;
            int y = (source.getHeight() - edge) / 2;
            int imageType = type == ImageType.PNG ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage target = new BufferedImage(OUTPUT_EDGE, OUTPUT_EDGE, imageType);
            scalePixels(source, target, x, y, edge);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(target, type.format, output)) {
                throw BusinessException.badRequest("头像图片编码不受支持");
            }
            return new AvatarImageBO(output.toByteArray(), type.contentType, type.extension);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw BusinessException.badRequest("头像文件无法读取");
        }
    }

    private void scalePixels(BufferedImage source, BufferedImage target, int x, int y, int edge) {
        for (int targetY = 0; targetY < OUTPUT_EDGE; targetY++) {
            int sourceY = y + Math.min(edge - 1, targetY * edge / OUTPUT_EDGE);
            for (int targetX = 0; targetX < OUTPUT_EDGE; targetX++) {
                int sourceX = x + Math.min(edge - 1, targetX * edge / OUTPUT_EDGE);
                target.setRGB(targetX, targetY, source.getRGB(sourceX, sourceY));
            }
        }
    }

    private BufferedImage readBounded(byte[] content) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw BusinessException.badRequest("仅支持 JPEG 或 PNG 头像");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_SOURCE_EDGE || height > MAX_SOURCE_EDGE) {
                    throw BusinessException.badRequest("头像尺寸必须在1到4096像素之间");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw BusinessException.badRequest("头像图片无法解析");
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
            for (int i = 0; i < signature.length; i++) png &= content[i] == signature[i];
            if (png) return ImageType.PNG;
        }
        throw BusinessException.badRequest("仅支持 JPEG 或 PNG 头像");
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

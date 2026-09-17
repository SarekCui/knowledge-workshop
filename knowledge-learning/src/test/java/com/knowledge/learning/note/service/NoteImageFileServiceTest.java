package com.knowledge.learning.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.knowledge.common.exception.BusinessException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class NoteImageFileServiceTest {

    private final NoteImageFileService service = new NoteImageFileService();

    @Test
    void downscalesAndReencodesLargePng() throws Exception {
        byte[] content = image("png", 2000, 1000);
        var result = service.normalize(new MockMultipartFile("file", "diagram.png", "image/png", content));

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.extension()).isEqualTo("png");
        assertThat(result.width()).isEqualTo(1600);
        assertThat(result.height()).isEqualTo(800);
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(result.content())).getWidth()).isEqualTo(1600);
    }

    @Test
    void acceptsJpegAndPreservesSmallDimensions() throws Exception {
        byte[] content = image("jpg", 320, 180);
        var result = service.normalize(new MockMultipartFile("file", "architecture.jpg", "image/jpeg", content));

        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.width()).isEqualTo(320);
        assertThat(result.height()).isEqualTo(180);
    }

    @Test
    void rejectsSpoofedOrUnsupportedContent() throws Exception {
        byte[] png = image("png", 10, 10);
        assertThatThrownBy(() -> service.normalize(
                new MockMultipartFile("file", "spoofed.jpg", "image/jpeg", png)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("声明类型");
        assertThatThrownBy(() -> service.normalize(
                new MockMultipartFile("file", "note.txt", "text/plain", "not an image".getBytes())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JPEG 或 PNG");
    }

    private byte[] image(String format, int width, int height) throws Exception {
        int type = "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}

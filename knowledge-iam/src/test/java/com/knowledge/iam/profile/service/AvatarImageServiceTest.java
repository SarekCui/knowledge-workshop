package com.knowledge.iam.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.iam.profile.bo.AvatarImageBO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AvatarImageServiceTest {

    private final AvatarImageService service = new AvatarImageService();

    @Test
    void centerCropsAndScalesValidPng() throws Exception {
        BufferedImage source = new BufferedImage(600, 300, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) source.setRGB(x, y, 0xff245bd6);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(source, "png", bytes);

        AvatarImageBO result = service.normalize(new MockMultipartFile(
                "file", "avatar.png", "image/png", bytes.toByteArray()));

        BufferedImage normalized = ImageIO.read(new ByteArrayInputStream(result.content()));
        assertThat(normalized.getWidth()).isEqualTo(256);
        assertThat(normalized.getHeight()).isEqualTo(256);
        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsDeclaredTypeThatDoesNotMatchMagicBytes() {
        assertThatThrownBy(() -> service.normalize(new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3, 4})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅支持 JPEG 或 PNG 头像");
    }
}

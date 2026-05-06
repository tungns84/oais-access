package com.poc.oais.access.service;

import com.poc.oais.access.config.AccessProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WatermarkServiceTest {

    private WatermarkService service;

    @BeforeEach
    void setUp() {
        AccessProperties props = new AccessProperties(
                new AccessProperties.Storage("samples", "cache"),
                new AccessProperties.AntiDownload(1, 30L),
                new AccessProperties.Watermark("{viewerId}-{timestamp}", List.of(2, 3)),
                new AccessProperties.Rendering(150, 60L, 300L, "soffice", "ffmpeg"),
                new AccessProperties.Audit("audit.log"),
                new AccessProperties.Cors("*")
        );
        service = new WatermarkService(props);
    }

    @Test
    void shouldWatermarkInModesTwoAndThree() {
        assertThat(service.shouldWatermark(1)).isFalse();
        assertThat(service.shouldWatermark(2)).isTrue();
        assertThat(service.shouldWatermark(3)).isTrue();
    }

    @Test
    void overlayPngProducesValidImageOfSameSize(@TempDir Path tmp) throws IOException {
        // Create a 400x300 white PNG
        BufferedImage src = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = src.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 400, 300);
        g.dispose();
        Path file = tmp.resolve("input.png");
        ImageIO.write(src, "png", file.toFile());

        byte[] watermarked = service.overlayPng(file, "viewer-abc");

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(watermarked));
        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(400);
        assertThat(result.getHeight()).isEqualTo(300);
    }

    @Test
    void overlayPngOutputDiffersFromInput(@TempDir Path tmp) throws IOException {
        BufferedImage src = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = src.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();
        Path file = tmp.resolve("input.png");
        ImageIO.write(src, "png", file.toFile());

        byte[] watermarked = service.overlayPng(file, "viewer-x");
        byte[] original = Files.readAllBytes(file);

        // Watermarked output bytes phải khác file gốc (đã overlay)
        assertThat(watermarked).isNotEqualTo(original);
        // Watermarked thường lớn hơn vì có nhiều pixel non-white
        assertThat(watermarked.length).isGreaterThan(0);
    }

    @Test
    void overlayPngThrowsOnInvalidImage(@TempDir Path tmp) throws IOException {
        Path notImage = tmp.resolve("not-an-image.png");
        Files.writeString(notImage, "this is not a png");

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> service.overlayPng(notImage, "viewer"));
    }
}

package com.poc.oais.access.service;

import com.poc.oais.access.config.AccessProperties;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class WatermarkService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final AccessProperties props;

    public WatermarkService(AccessProperties props) {
        this.props = props;
    }

    public boolean shouldWatermark(int mode) {
        return props.watermark().enabledForModes() != null
                && props.watermark().enabledForModes().contains(mode);
    }

    public byte[] overlayPng(Path imageFile, String viewerId) throws IOException {
        BufferedImage src = ImageIO.read(imageFile.toFile());
        if (src == null) throw new IOException("Cannot read image: " + imageFile);

        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, null);

        String timestamp = TS_FMT.format(Instant.now());
        String text = props.watermark().template()
                .replace("{viewerId}", viewerId)
                .replace("{timestamp}", timestamp);

        int fontSize = Math.max(18, src.getWidth() / 40);
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        g.setColor(new Color(255, 0, 0, 90));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));

        AffineTransform original = g.getTransform();
        g.rotate(Math.toRadians(-30), src.getWidth() / 2.0, src.getHeight() / 2.0);

        int stepY = fontSize * 6;
        int stepX = fontSize * (text.length() + 6);
        for (int y = -src.getHeight(); y < src.getHeight() * 2; y += stepY) {
            for (int x = -src.getWidth(); x < src.getWidth() * 2; x += stepX) {
                g.drawString(text, x, y);
            }
        }
        g.setTransform(original);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "png", baos);
        return baos.toByteArray();
    }
}

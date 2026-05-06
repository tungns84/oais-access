package com.poc.oais.access.service.renderer;

import com.poc.oais.access.config.AccessProperties;
import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DocumentKind;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Component
public class PdfRenderer implements Renderer {

    private static final Logger log = LoggerFactory.getLogger(PdfRenderer.class);

    private final AccessProperties props;

    public PdfRenderer(AccessProperties props) {
        this.props = props;
    }

    @Override
    public DocumentKind supports() {
        return DocumentKind.PDF;
    }

    @Override
    public int prepare(AipMetadata aip) {
        return prepareFromPdfFile(aip.id(), aip.sourcePath());
    }

    /**
     * Allow OfficeRenderer to reuse this pipeline after converting Office → PDF.
     */
    public int prepareFromPdfFile(String aipId, Path pdfFile) {
        Path cacheDir = cacheDirFor(aipId);
        Path readyMarker = cacheDir.resolve(".ready");
        Path manifestFile = cacheDir.resolve("manifest.properties");

        try {
            if (Files.exists(readyMarker) && Files.exists(manifestFile)) {
                Properties p = new Properties();
                try (var in = Files.newInputStream(manifestFile)) {
                    p.load(in);
                }
                int cached = Integer.parseInt(p.getProperty("pageCount", "0"));
                if (cached > 0) return cached;
            }
            Files.createDirectories(cacheDir);

            try (PDDocument doc = Loader.loadPDF(pdfFile.toFile())) {
                int pageCount = doc.getNumberOfPages();
                org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
                int dpi = props.rendering().pdfDpi();
                for (int i = 0; i < pageCount; i++) {
                    BufferedImage img = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                    Path out = cacheDir.resolve("page-" + (i + 1) + ".png");
                    ImageIO.write(img, "png", out.toFile());
                }
                Properties out = new Properties();
                out.setProperty("pageCount", String.valueOf(pageCount));
                out.setProperty("kind", "PDF");
                try (var os = Files.newOutputStream(manifestFile)) {
                    out.store(os, "DIP manifest");
                }
                Files.writeString(readyMarker, "ready\n");
                log.info("Prepared {} pages for AIP {}", pageCount, aipId);
                return pageCount;
            }
        } catch (IOException e) {
            log.error("PdfRenderer failed for AIP {}: {}", aipId, e.getMessage());
            throw new RuntimeException("PDF render failed", e);
        }
    }

    @Override
    public Path resolvePage(AipMetadata aip, int pageNumber) {
        Path cacheDir = cacheDirFor(aip.id());
        Path file = cacheDir.resolve("page-" + pageNumber + ".png");
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Page out of range: " + pageNumber);
        }
        return file;
    }

    private Path cacheDirFor(String aipId) {
        return Paths.get(props.storage().derivedCache()).resolve(aipId).toAbsolutePath().normalize();
    }
}

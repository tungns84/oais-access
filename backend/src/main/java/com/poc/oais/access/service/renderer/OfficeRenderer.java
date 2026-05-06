package com.poc.oais.access.service.renderer;

import com.poc.oais.access.config.AccessProperties;
import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DocumentKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OfficeRenderer implements Renderer {

    private static final Logger log = LoggerFactory.getLogger(OfficeRenderer.class);

    private final AccessProperties props;
    private final PdfRenderer pdfRenderer;

    public OfficeRenderer(AccessProperties props, PdfRenderer pdfRenderer) {
        this.props = props;
        this.pdfRenderer = pdfRenderer;
    }

    @Override
    public DocumentKind supports() {
        return DocumentKind.OFFICE;
    }

    @Override
    public int prepare(AipMetadata aip) {
        Path cacheDir = cacheDirFor(aip.id());
        Path intermediatePdf = cacheDir.resolve("_intermediate.pdf");
        try {
            Files.createDirectories(cacheDir);
            if (!Files.exists(intermediatePdf)) {
                runLibreOfficeConvert(aip.sourcePath(), cacheDir);
                Path produced = findProducedPdf(cacheDir, intermediatePdf);
                if (produced == null) {
                    throw new RuntimeException("RENDERER_OUTPUT_MISSING in " + cacheDir);
                }
                Files.move(produced, intermediatePdf, StandardCopyOption.REPLACE_EXISTING);
            }
            return pdfRenderer.prepareFromPdfFile(aip.id(), intermediatePdf);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("OfficeRenderer failed for AIP {}: {}", aip.id(), e.getMessage());
            throw new RuntimeException("RENDERER_UNAVAILABLE: " + e.getMessage(), e);
        }
    }

    @Override
    public Path resolvePage(AipMetadata aip, int pageNumber) {
        return pdfRenderer.resolvePage(aip, pageNumber);
    }

    private void runLibreOfficeConvert(Path source, Path outDir) throws IOException, InterruptedException {
        String binary = props.rendering().libreofficeBinary();
        long timeout = props.rendering().officeConvertTimeoutSeconds();

        ProcessBuilder pb = new ProcessBuilder(List.of(
                binary, "--headless", "--norestore", "--nofirststartwizard",
                "--convert-to", "pdf",
                "--outdir", outDir.toAbsolutePath().toString(),
                source.toAbsolutePath().toString()
        )).redirectErrorStream(true);
        log.info("Running LibreOffice convert: {}", String.join(" ", pb.command()));
        Process p = pb.start();
        boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("RENDER_TIMEOUT: LibreOffice convert exceeded " + timeout + "s");
        }
        if (p.exitValue() != 0) {
            String stdout = new String(p.getInputStream().readAllBytes());
            throw new IOException("LibreOffice convert exit=" + p.exitValue() + " out=" + stdout);
        }
    }

    private static Path findProducedPdf(Path dir, Path skip) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .filter(p -> !p.equals(skip))
                    .findFirst()
                    .orElse(null);
        }
    }

    private Path cacheDirFor(String aipId) {
        return Paths.get(props.storage().derivedCache()).resolve(aipId).toAbsolutePath().normalize();
    }
}

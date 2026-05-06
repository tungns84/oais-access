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
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class VideoRenderer implements Renderer {

    private static final Logger log = LoggerFactory.getLogger(VideoRenderer.class);

    private final AccessProperties props;

    public VideoRenderer(AccessProperties props) {
        this.props = props;
    }

    @Override
    public DocumentKind supports() {
        return DocumentKind.VIDEO;
    }

    @Override
    public int prepare(AipMetadata aip) {
        Path cacheDir = cacheDirFor(aip.id());
        Path masterPlaylist = cacheDir.resolve("master.m3u8");
        try {
            Files.createDirectories(cacheDir);
            if (!Files.exists(masterPlaylist)) {
                runFfmpegHls(aip.sourcePath(), cacheDir);
            }
            return 1;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("VideoRenderer failed for AIP {}: {}", aip.id(), e.getMessage());
            throw new RuntimeException("RENDERER_UNAVAILABLE: " + e.getMessage(), e);
        }
    }

    @Override
    public Path resolveHlsAsset(AipMetadata aip, String fileName) {
        Path cacheDir = cacheDirFor(aip.id());
        Path target = cacheDir.resolve(fileName).normalize();
        if (!target.startsWith(cacheDir)) {
            throw new IllegalArgumentException("HLS_PATH_INVALID");
        }
        return target;
    }

    private void runFfmpegHls(Path source, Path outDir) throws IOException, InterruptedException {
        String binary = props.rendering().ffmpegBinary();
        long timeout = props.rendering().ffmpegTimeoutSeconds();

        Path master = outDir.resolve("master.m3u8");
        Path segPattern = outDir.resolve("seg-%d.ts");

        ProcessBuilder pb = new ProcessBuilder(List.of(
                binary, "-y", "-i", source.toAbsolutePath().toString(),
                "-codec:", "copy",
                "-start_number", "0",
                "-hls_time", "6",
                "-hls_list_size", "0",
                "-hls_segment_filename", segPattern.toAbsolutePath().toString(),
                "-f", "hls",
                master.toAbsolutePath().toString()
        )).redirectErrorStream(true);
        log.info("Running ffmpeg HLS segment: {}", String.join(" ", pb.command()));
        Process p = pb.start();
        boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("RENDER_TIMEOUT: ffmpeg exceeded " + timeout + "s");
        }
        if (p.exitValue() != 0) {
            String stdout = new String(p.getInputStream().readAllBytes());
            throw new IOException("ffmpeg exit=" + p.exitValue() + " out=" + stdout);
        }
    }

    private Path cacheDirFor(String aipId) {
        return Paths.get(props.storage().derivedCache()).resolve(aipId).toAbsolutePath().normalize();
    }
}

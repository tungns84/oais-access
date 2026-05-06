package com.poc.oais.access.service;

import com.poc.oais.access.config.AccessProperties;
import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DocumentKind;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Service
public class ArchivalStorageService {

    private static final Logger log = LoggerFactory.getLogger(ArchivalStorageService.class);

    private final AccessProperties props;
    private final Map<String, AipMetadata> index = new LinkedHashMap<>();

    public ArchivalStorageService(AccessProperties props) {
        this.props = props;
    }

    @PostConstruct
    public synchronized void scan() {
        Path root = Paths.get(props.storage().archivalRoot()).toAbsolutePath().normalize();
        log.info("Scanning archival storage root: {}", root);

        index.clear();

        if (!Files.isDirectory(root)) {
            log.warn("Archival root does not exist or is not a directory: {}", root);
            return;
        }

        try (Stream<Path> stream = Files.walk(root, 2)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> tryRegister(path).ifPresent(meta -> {
                        index.put(meta.id(), meta);
                        log.info("Registered AIP: {} kind={} size={}",
                                meta.title(), meta.kind(), meta.sizeBytes());
                    }));
        } catch (IOException e) {
            log.error("Failed to walk archival root", e);
        }

        log.info("Scan complete — {} AIP(s) indexed", index.size());
    }

    public synchronized int rescan() {
        scan();
        return index.size();
    }

    public Optional<AipMetadata> findById(String id) {
        return Optional.ofNullable(index.get(id));
    }

    public List<AipMetadata> listAll() {
        return new ArrayList<>(index.values());
    }

    private Optional<AipMetadata> tryRegister(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.startsWith(".") || fileName.equalsIgnoreCase("README.txt")) {
            return Optional.empty();
        }
        String ext = extensionOf(fileName);
        Optional<DocumentKind> kind = DocumentKind.fromExtension(ext);
        if (kind.isEmpty()) {
            log.debug("Skipping unsupported file: {}", fileName);
            return Optional.empty();
        }
        try {
            long size = Files.size(path);
            String mime = Files.probeContentType(path);
            String id = sha1Hex(fileName);
            return Optional.of(new AipMetadata(
                    id, fileName, kind.get(),
                    mime != null ? mime : "application/octet-stream",
                    size, path, Instant.now()
            ));
        } catch (IOException e) {
            log.error("Failed to read file metadata: {}", path, e);
            return Optional.empty();
        }
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1);
    }

    private static String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

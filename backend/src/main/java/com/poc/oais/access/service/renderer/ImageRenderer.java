package com.poc.oais.access.service.renderer;

import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DocumentKind;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ImageRenderer implements Renderer {

    @Override
    public DocumentKind supports() {
        return DocumentKind.IMAGE;
    }

    @Override
    public int prepare(AipMetadata aip) {
        return 1;
    }

    @Override
    public Path resolvePage(AipMetadata aip, int pageNumber) {
        if (pageNumber != 1) {
            throw new IllegalArgumentException("IMAGE has only 1 page");
        }
        return aip.sourcePath();
    }
}

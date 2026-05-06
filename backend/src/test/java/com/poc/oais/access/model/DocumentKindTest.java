package com.poc.oais.access.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentKindTest {

    @ParameterizedTest
    @CsvSource({
            "pdf,    PDF",
            "PDF,    PDF",
            "docx,   OFFICE",
            "DOCX,   OFFICE",
            "xlsx,   OFFICE",
            "pptx,   OFFICE",
            "doc,    OFFICE",
            "xls,    OFFICE",
            "ppt,    OFFICE",
            "png,    IMAGE",
            "PNG,    IMAGE",
            "jpg,    IMAGE",
            "jpeg,   IMAGE",
            "mp4,    VIDEO",
            "MP4,    VIDEO"
    })
    void recognizesSupportedExtensions(String ext, DocumentKind expected) {
        assertThat(DocumentKind.fromExtension(ext)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"txt", "exe", "gif", "svg", "tiff", "mp3", "zip", "json", "xml"})
    void rejectsUnsupportedExtensions(String ext) {
        assertThat(DocumentKind.fromExtension(ext)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void handlesNullAndEmpty(String ext) {
        assertThat(DocumentKind.fromExtension(ext)).isEmpty();
    }

    @Test
    void caseInsensitiveMatching() {
        assertThat(DocumentKind.fromExtension("Pdf")).contains(DocumentKind.PDF);
        assertThat(DocumentKind.fromExtension("JpEg")).contains(DocumentKind.IMAGE);
    }
}

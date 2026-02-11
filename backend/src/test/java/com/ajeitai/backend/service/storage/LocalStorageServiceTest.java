package com.ajeitai.backend.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    LocalStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalStorageService(tempDir.resolve("uploads").toString(), tempDir.resolve("media").toString());
    }

    @Test
    void salvar_documents_createsFileAndReturnsKey() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});
        String key = service.salvar(StorageService.BUCKET_DOCUMENTS, "prestador-1/abc.pdf", file);
        assertThat(key).isEqualTo("prestador-1/abc.pdf");
        Resource r = service.obterRecurso(StorageService.BUCKET_DOCUMENTS, key);
        assertThat(r).isNotNull();
        assertThat(r.exists()).isTrue();
    }

    @Test
    void salvar_throwsWhenFileEmpty() {
        MultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[0]);
        assertThatThrownBy(() -> service.salvar(StorageService.BUCKET_DOCUMENTS, "k", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void excluir_removesFile() throws IOException {
        MultipartFile file = new MockMultipartFile("f", "x.pdf", "application/pdf", new byte[]{1});
        String key = service.salvar(StorageService.BUCKET_DOCUMENTS, "p1/x.pdf", file);
        assertThat(service.obterRecurso(StorageService.BUCKET_DOCUMENTS, key)).isNotNull();
        service.excluir(StorageService.BUCKET_DOCUMENTS, key);
        assertThat(service.obterRecurso(StorageService.BUCKET_DOCUMENTS, key)).isNull();
    }
}

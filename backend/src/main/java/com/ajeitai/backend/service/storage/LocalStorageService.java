package com.ajeitai.backend.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path documentsBasePath;
    private final Path avatarsBasePath;

    public LocalStorageService(
            @Value("${app.upload-dir:uploads}") String uploadDir,
            @Value("${app.media-dir:media}") String mediaDir) {
        this.documentsBasePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.avatarsBasePath = Paths.get(mediaDir).toAbsolutePath().normalize();
    }

    @Override
    public String salvar(String bucket, String key, MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode ser vazio.");
        }
        Path base = bucket.equals(BUCKET_DOCUMENTS) ? documentsBasePath : avatarsBasePath;
        Path fullPath = base.resolve(key).normalize();
        if (!fullPath.startsWith(base)) {
            throw new IllegalArgumentException("Chave inválida.");
        }
        Files.createDirectories(fullPath.getParent());
        arquivo.transferTo(fullPath.toFile());
        return key;
    }

    @Override
    public void excluir(String bucket, String key) throws IOException {
        Path base = bucket.equals(BUCKET_DOCUMENTS) ? documentsBasePath : avatarsBasePath;
        Path fullPath = base.resolve(key).normalize();
        if (!fullPath.startsWith(base)) {
            return;
        }
        if (Files.exists(fullPath)) {
            Files.delete(fullPath);
        }
    }

    @Override
    public Resource obterRecurso(String bucket, String key) throws IOException {
        Path base = bucket.equals(BUCKET_DOCUMENTS) ? documentsBasePath : avatarsBasePath;
        Path fullPath = base.resolve(key).normalize();
        if (!fullPath.startsWith(base) || !Files.exists(fullPath)) {
            return null;
        }
        return new UrlResource(fullPath.toUri());
    }
}

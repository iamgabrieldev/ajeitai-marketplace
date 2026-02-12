package com.ajeitai.backend.service.storage;

import com.ajeitai.backend.infra.storage.MagaluS3HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementação de {@link StorageService} usando o Object Storage da Magalu Cloud,
 * que é compatível com a API S3 e utiliza autenticação por API Key.
 *
 * Ativada quando app.storage.type=magalu.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "magalu")
public class MagaluStorageService implements StorageService {

    private final MagaluS3HttpClient client;
    private final String bucketDocuments;
    private final String bucketAvatars;

    public MagaluStorageService(
            @Value("${app.storage.magalu.endpoint}") String endpoint,
            @Value("${app.storage.magalu.access-key}") String accessKey,
            @Value("${app.storage.magalu.api-key-header:X-API-Key}") String apiKeyHeader,
            @Value("${app.storage.bucket-documents:ajeitai-documents}") String bucketDocuments,
            @Value("${app.storage.bucket-avatars:ajeitai-avatars}") String bucketAvatars
    ) {
        this.client = new MagaluS3HttpClient(endpoint, accessKey, apiKeyHeader);
        this.bucketDocuments = bucketDocuments;
        this.bucketAvatars = bucketAvatars;
    }

    private String resolveBucket(String logical) {
        return BUCKET_DOCUMENTS.equals(logical) ? bucketDocuments : bucketAvatars;
    }

    @Override
    public String salvar(String bucket, String key, MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode ser vazio.");
        }
        String bucketName = resolveBucket(bucket);
        String contentType = arquivo.getContentType() != null ? arquivo.getContentType() : "application/octet-stream";
        try (InputStream in = arquivo.getInputStream()) {
            client.putObject(bucketName, key, in, arquivo.getSize(), contentType);
        }
        return key;
    }

    @Override
    public void excluir(String bucket, String key) throws IOException {
        String bucketName = resolveBucket(bucket);
        client.deleteObject(bucketName, key);
    }

    @Override
    public Resource obterRecurso(String bucket, String key) throws IOException {
        String bucketName = resolveBucket(bucket);
        InputStream in = client.getObject(bucketName, key);
        if (in == null) {
            return null;
        }
        return new InputStreamResource(in);
    }
}


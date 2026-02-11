package com.ajeitai.backend.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

/**
 * Armazenamento S3-compatível (AWS S3 ou Magalu Cloud Object Storage com API S3).
 * Configurar app.storage.type=s3 e propriedades de endpoint/credenciais.
 */
@Service
@S3StorageCondition
public class S3CompatibleStorageService implements StorageService {

    private final S3Client s3Client;
    private final String bucketDocuments;
    private final String bucketAvatars;

    public S3CompatibleStorageService(
            S3Client s3Client,
            @Value("${app.storage.bucket-documents:ajeitai-documents}") String bucketDocuments,
            @Value("${app.storage.bucket-avatars:ajeitai-avatars}") String bucketAvatars) {
        this.s3Client = s3Client;
        this.bucketDocuments = bucketDocuments;
        this.bucketAvatars = bucketAvatars;
    }

    private String bucket(String logical) {
        return BUCKET_DOCUMENTS.equals(logical) ? bucketDocuments : bucketAvatars;
    }

    @Override
    public String salvar(String bucket, String key, MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode ser vazio.");
        }
        String bucketName = bucket(bucket);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(arquivo.getContentType() != null ? arquivo.getContentType() : "application/octet-stream")
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(arquivo.getInputStream(), arquivo.getSize()));
        return key;
    }

    @Override
    public void excluir(String bucket, String key) throws IOException {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket(bucket))
                    .key(key)
                    .build());
        } catch (Exception e) {
            throw new IOException("Falha ao excluir objeto no storage", e);
        }
    }

    @Override
    public Resource obterRecurso(String bucket, String key) throws IOException {
        try {
            InputStream stream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket(bucket))
                    .key(key)
                    .build());
            return new InputStreamResource(stream);
        } catch (NoSuchKeyException e) {
            return null;
        }
    }
}

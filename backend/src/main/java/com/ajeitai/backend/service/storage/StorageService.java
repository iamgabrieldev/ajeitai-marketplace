package com.ajeitai.backend.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Abstração para armazenamento de arquivos (local, S3-compatível ou API nativa).
 * Dois "buckets" lógicos: documentos (prestador) e avatares/mídia.
 */
public interface StorageService {

    String BUCKET_DOCUMENTS = "documents";
    String BUCKET_AVATARS = "avatars";

    /**
     * Salva o arquivo no bucket lógico e retorna a chave (path) para referência.
     */
    String salvar(String bucket, String key, MultipartFile arquivo) throws IOException;

    /**
     * Remove o objeto pelo bucket e chave.
     */
    void excluir(String bucket, String key) throws IOException;

    /**
     * Obtém o recurso para leitura (download). Retorna null se não existir.
     */
    Resource obterRecurso(String bucket, String key) throws IOException;
}

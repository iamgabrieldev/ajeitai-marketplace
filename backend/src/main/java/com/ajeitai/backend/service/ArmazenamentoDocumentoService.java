package com.ajeitai.backend.service;

import com.ajeitai.backend.service.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ArmazenamentoDocumentoService {

    private final StorageService storage;

    public ArmazenamentoDocumentoService(StorageService storage) {
        this.storage = storage;
    }

    /**
     * Salva o arquivo no bucket de documentos (path: prestador-{id}/{uuid}.ext).
     * Retorna o caminho relativo para persistir no banco.
     */
    public String salvar(Long prestadorId, MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode ser vazio.");
        }
        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            nomeOriginal = "documento";
        }
        String extensao = "";
        int i = nomeOriginal.lastIndexOf('.');
        if (i > 0) {
            extensao = nomeOriginal.substring(i);
        }
        String nomeUnico = UUID.randomUUID().toString() + extensao;
        String key = "prestador-" + prestadorId + "/" + nomeUnico;
        return storage.salvar(StorageService.BUCKET_DOCUMENTS, key, arquivo);
    }

    /**
     * Obtém recurso para download. Retorna null se não existir.
     */
    public Resource obterRecurso(String caminhoRelativo) throws IOException {
        return storage.obterRecurso(StorageService.BUCKET_DOCUMENTS, caminhoRelativo);
    }

    public void excluir(String caminhoRelativo) throws IOException {
        storage.excluir(StorageService.BUCKET_DOCUMENTS, caminhoRelativo);
    }
}

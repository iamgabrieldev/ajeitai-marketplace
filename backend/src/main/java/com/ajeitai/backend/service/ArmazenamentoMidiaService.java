package com.ajeitai.backend.service;

import com.ajeitai.backend.service.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ArmazenamentoMidiaService {

    private final StorageService storage;

    public ArmazenamentoMidiaService(StorageService storage) {
        this.storage = storage;
    }

    /**
     * Salva no bucket de avatars/mídia. path = pasta + "/" + uuid.ext (ex.: avatares/xyz.jpg).
     */
    public String salvar(String pasta, MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode ser vazio.");
        }
        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            nomeOriginal = "arquivo";
        }
        String extensao = "";
        int i = nomeOriginal.lastIndexOf('.');
        if (i > 0) {
            extensao = nomeOriginal.substring(i);
        }
        String nomeUnico = UUID.randomUUID().toString() + extensao;
        String key = pasta + "/" + nomeUnico;
        return storage.salvar(StorageService.BUCKET_AVATARS, key, arquivo);
    }
}

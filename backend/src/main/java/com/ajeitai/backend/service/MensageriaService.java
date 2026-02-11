package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.cliente.Cliente;
import org.springframework.stereotype.Service;

@Service
public class MensageriaService {

    public void enviarLinkPagamento(Cliente cliente, String linkPagamento) {
        // Stub: integração real com WhatsApp e email será adicionada depois.
    }
}

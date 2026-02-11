package com.ajeitai.backend.domain.prestador;

import com.ajeitai.backend.domain.endereco.DadosEndereco;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.br.CNPJ;

import java.math.BigDecimal;

public record DadosCadastroPrestador(
        @NotBlank(message = "O nome fantasia é obrigatório")
        String nomeFantasia,
        @Email(message = "Email inválido")
        String email,
        @Pattern(regexp = "\\d{10,11}")
        String telefone,
        @NotBlank
        @CNPJ
        String cnpj,
        @NotNull
        CategoriaAtuacao categoria,
        @DecimalMin(value = "0", message = "O valor do serviço não pode ser negativo")
        BigDecimal valorServico,  // opcional no cadastro
        @NotNull
        @Valid
        DadosEndereco endereco
) {
}

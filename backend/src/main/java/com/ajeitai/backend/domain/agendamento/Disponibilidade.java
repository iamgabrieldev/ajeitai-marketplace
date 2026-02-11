package com.ajeitai.backend.domain.agendamento;

import com.ajeitai.backend.domain.prestador.Prestador;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalTime;

/**
 * Agenda semanal recorrente do prestador.
 * diaSemana: 1 = Segunda, 7 = Domingo (padrão java.time.DayOfWeek).
 */
@Entity
@Table(name = "disponibilidades", indexes = {
        @Index(name = "idx_disponibilidade_prestador", columnList = "prestador_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disponibilidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    /**
     * Dia da semana (1 = Segunda-feira, 7 = Domingo).
     */
    @Column(nullable = false)
    private Integer diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;
}

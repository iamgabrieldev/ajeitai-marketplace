"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/providers/auth-provider";
import { disponibilidadeApi, type Disponibilidade } from "@/lib/api";
import { PageHeader } from "@/components/layout/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { Save, Plus, Trash2, Clock } from "lucide-react";

const diasSemana = [
  "Domingo",
  "Segunda",
  "Terça",
  "Quarta",
  "Quinta",
  "Sexta",
  "Sábado",
];

export default function DisponibilidadePage() {
  const { token } = useAuth();
  const { toast } = useToast();
  const [slots, setSlots] = useState<Disponibilidade[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const fetchDisponibilidade = useCallback(async () => {
    if (!token) return;
    try {
      const data = await disponibilidadeApi.get(token);
      setSlots(data);
    } catch (err) {
      console.error("Erro ao buscar disponibilidade:", err);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchDisponibilidade();
  }, [fetchDisponibilidade]);

  const handleSave = async () => {
    if (!token) return;
    setSaving(true);
    try {
      await disponibilidadeApi.update(token, slots);
      toast("Disponibilidade salva com sucesso!", "success");
    } catch {
      toast("Erro ao salvar disponibilidade", "error");
    } finally {
      setSaving(false);
    }
  };

  const addSlot = (dia: number) => {
    setSlots((prev) => [
      ...prev,
      { diaSemana: dia, horaInicio: "08:00", horaFim: "18:00" },
    ]);
  };

  const removeSlot = (index: number) => {
    setSlots((prev) => prev.filter((_, i) => i !== index));
  };

  const updateSlot = (
    index: number,
    field: "horaInicio" | "horaFim",
    value: string
  ) => {
    setSlots((prev) =>
      prev.map((s, i) => (i === index ? { ...s, [field]: value } : s))
    );
  };

  // Agrupa slots por dia
  const slotsPorDia = diasSemana.map((nome, dia) => ({
    nome,
    dia,
    slots: slots
      .map((s, idx) => ({ ...s, _idx: idx }))
      .filter((s) => s.diaSemana === dia),
  }));

  return (
    <div className="flex flex-col">
      <PageHeader
        title="Disponibilidade"
        subtitle="Configure seus horários de atendimento"
      />

      <div className="flex flex-col gap-3 px-4 py-4">
        {loading ? (
          [1, 2, 3].map((i) => (
            <Card key={i} className="animate-pulse">
              <div className="h-16 rounded bg-secondary-200 dark:bg-secondary-700" />
            </Card>
          ))
        ) : (
          <>
            {slotsPorDia.map(({ nome, dia, slots: diaSlots }) => (
              <Card key={dia}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Clock className="h-5 w-5 text-primary" />
                    <h3 className="font-semibold text-text dark:text-text-dark">
                      {nome}
                    </h3>
                  </div>
                  <button
                    onClick={() => addSlot(dia)}
                    className="flex items-center gap-1 rounded-lg px-2 py-1 text-sm text-primary hover:bg-primary-50 dark:hover:bg-primary-900/20"
                  >
                    <Plus className="h-4 w-4" />
                    Adicionar
                  </button>
                </div>

                {diaSlots.length === 0 ? (
                  <p className="mt-2 text-sm text-text-muted dark:text-text-dark-muted">
                    Sem horários configurados
                  </p>
                ) : (
                  <div className="mt-3 flex flex-col gap-2">
                    {diaSlots.map((slot) => (
                      <div
                        key={slot._idx}
                        className="flex items-center gap-2"
                      >
                        <Input
                          type="time"
                          value={slot.horaInicio}
                          onChange={(e) =>
                            updateSlot(slot._idx, "horaInicio", e.target.value)
                          }
                          className="flex-1"
                        />
                        <span className="text-text-muted dark:text-text-dark-muted">
                          até
                        </span>
                        <Input
                          type="time"
                          value={slot.horaFim}
                          onChange={(e) =>
                            updateSlot(slot._idx, "horaFim", e.target.value)
                          }
                          className="flex-1"
                        />
                        <button
                          onClick={() => removeSlot(slot._idx)}
                          className="rounded-lg p-2 text-danger hover:bg-red-50 dark:hover:bg-red-900/20"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            ))}

            <Button
              onClick={handleSave}
              loading={saving}
              size="lg"
              className="mt-2 w-full"
              icon={<Save className="h-5 w-5" />}
            >
              Salvar Disponibilidade
            </Button>
          </>
        )}
      </div>
    </div>
  );
}

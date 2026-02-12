"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/providers/auth-provider";
import { adminApi, type Agendamento } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";

const STATUS_OPTIONS = ["", "PENDENTE", "ACEITO", "CONFIRMADO", "REALIZADO", "CANCELADO", "RECUSADO"];

export default function AdminAgendamentosPage() {
  const { token } = useAuth();
  const [list, setList] = useState<Agendamento[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("");

  const fetchData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const res = await adminApi.agendamentos(token, statusFilter || undefined);
      setList(res);
    } catch {
      setList([]);
    } finally {
      setLoading(false);
    }
  }, [token, statusFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-bold text-text dark:text-text-dark">Agendamentos</h1>
      <div className="flex items-center gap-2">
        <label className="text-sm text-text-muted dark:text-text-dark-muted">Status:</label>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="rounded-lg border border-secondary-200 bg-white px-3 py-2 text-sm dark:border-secondary-700 dark:bg-surface-dark"
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s || "todos"} value={s}>
              {s || "Todos"}
            </option>
          ))}
        </select>
      </div>
      {loading ? (
        <p className="text-text-muted dark:text-text-dark-muted">Carregando...</p>
      ) : (
        <div className="flex flex-col gap-2">
          {list.length === 0 ? (
            <Card>
              <CardContent className="py-8 text-center text-text-muted dark:text-text-dark-muted">
                Nenhum agendamento encontrado.
              </CardContent>
            </Card>
          ) : (
            list.map((ag) => (
              <Card key={ag.id}>
                <CardContent className="pt-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <p className="font-medium text-text dark:text-text-dark">
                        #{ag.id} · {ag.prestadorNome ?? "Prestador"} → {ag.clienteNome ?? "Cliente"}
                      </p>
                      <p className="text-sm text-text-muted dark:text-text-dark-muted">
                        {new Date(ag.dataHora).toLocaleString("pt-BR")} · {ag.status}
                      </p>
                    </div>
                    {ag.valorServico != null && (
                      <p className="font-semibold text-text dark:text-text-dark">
                        R$ {Number(ag.valorServico).toFixed(2)}
                      </p>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </div>
      )}
    </div>
  );
}

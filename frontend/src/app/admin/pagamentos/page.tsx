"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/providers/auth-provider";
import { adminApi, type AdminPagamento, type AdminSaque } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";

export default function AdminPagamentosPage() {
  const { token } = useAuth();
  const [pagamentos, setPagamentos] = useState<AdminPagamento[]>([]);
  const [saques, setSaques] = useState<AdminSaque[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!token) return;
    try {
      const [pags, saqs] = await Promise.all([
        adminApi.pagamentos(token),
        adminApi.saques(token),
      ]);
      setPagamentos(pags);
      setSaques(saqs);
    } catch {
      setPagamentos([]);
      setSaques([]);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-bold text-text dark:text-text-dark">Pagamentos e saques</h1>
      {loading ? (
        <p className="text-text-muted dark:text-text-dark-muted">Carregando...</p>
      ) : (
        <>
          <section>
            <h2 className="mb-2 text-lg font-semibold text-text dark:text-text-dark">Pagamentos (últimos)</h2>
            <div className="flex flex-col gap-2">
              {pagamentos.length === 0 ? (
                <Card>
                  <CardContent className="py-6 text-center text-text-muted dark:text-text-dark-muted">
                    Nenhum pagamento.
                  </CardContent>
                </Card>
              ) : (
                pagamentos.map((p) => (
                  <Card key={p.id}>
                    <CardContent className="flex flex-wrap items-center justify-between gap-2 pt-4">
                      <div>
                        <p className="text-sm font-medium">#{p.id} · Agendamento #{p.agendamentoId} · {p.status}</p>
                        {p.billingId && (
                          <p className="text-xs text-text-muted dark:text-text-dark-muted">Billing: {p.billingId}</p>
                        )}
                        <p className="text-xs text-text-muted dark:text-text-dark-muted">
                          Criado: {new Date(p.criadoEm).toLocaleString("pt-BR")}
                          {p.confirmadoEm && ` · Confirmado: ${new Date(p.confirmadoEm).toLocaleString("pt-BR")}`}
                        </p>
                      </div>
                    </CardContent>
                  </Card>
                ))
              )}
            </div>
          </section>
          <section>
            <h2 className="mb-2 text-lg font-semibold text-text dark:text-text-dark">Saques (últimos)</h2>
            <div className="flex flex-col gap-2">
              {saques.length === 0 ? (
                <Card>
                  <CardContent className="py-6 text-center text-text-muted dark:text-text-dark-muted">
                    Nenhum saque.
                  </CardContent>
                </Card>
              ) : (
                saques.map((s) => (
                  <Card key={s.id}>
                    <CardContent className="flex flex-wrap items-center justify-between gap-2 pt-4">
                      <div>
                        <p className="text-sm font-medium">
                          #{s.id} · {s.prestadorNome ?? `Prestador ${s.prestadorId}`} · {s.status}
                        </p>
                        <p className="text-xs text-text-muted dark:text-text-dark-muted">
                          Solicitado: {new Date(s.solicitadoEm).toLocaleString("pt-BR")}
                          {s.concluidoEm && ` · Concluído: ${new Date(s.concluidoEm).toLocaleString("pt-BR")}`}
                        </p>
                      </div>
                      <p className="font-semibold text-text dark:text-text-dark">
                        {formatCurrency(s.valorLiquido)}
                      </p>
                    </CardContent>
                  </Card>
                ))
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}

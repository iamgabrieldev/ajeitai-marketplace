"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/providers/auth-provider";
import { adminApi, type AdminPrestador } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";

export default function AdminPrestadoresPage() {
  const { token } = useAuth();
  const [list, setList] = useState<AdminPrestador[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!token) return;
    try {
      const res = await adminApi.prestadores(token);
      setList(res);
    } catch {
      setList([]);
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
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-bold text-text dark:text-text-dark">Prestadores</h1>
      {loading ? (
        <p className="text-text-muted dark:text-text-dark-muted">Carregando...</p>
      ) : (
        <div className="flex flex-col gap-2">
          {list.length === 0 ? (
            <Card>
              <CardContent className="py-8 text-center text-text-muted dark:text-text-dark-muted">
                Nenhum prestador encontrado.
              </CardContent>
            </Card>
          ) : (
            list.map((p) => (
              <Card key={p.id}>
                <CardContent className="flex flex-wrap items-center justify-between gap-2 pt-4">
                  <div>
                    <p className="font-medium text-text dark:text-text-dark">{p.nomeFantasia}</p>
                    {p.email && (
                      <p className="text-sm text-text-muted dark:text-text-dark-muted">{p.email}</p>
                    )}
                    <div className="mt-1 flex gap-2 text-xs">
                      <span className="rounded bg-secondary-100 px-2 py-0.5 dark:bg-secondary-800">
                        Assinatura: {p.statusAssinatura ?? "—"}
                      </span>
                      {p.dataFimAssinatura && (
                        <span>Válida até: {new Date(p.dataFimAssinatura).toLocaleDateString("pt-BR")}</span>
                      )}
                    </div>
                  </div>
                  <p className="font-semibold text-text dark:text-text-dark">
                    Saldo: {formatCurrency(p.saldoDisponivel)}
                  </p>
                </CardContent>
              </Card>
            ))
          )}
        </div>
      )}
    </div>
  );
}

"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/providers/auth-provider";
import { adminApi, type AdminVisaoGeral } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Users, Briefcase, CalendarCheck, DollarSign, TrendingUp } from "lucide-react";

export default function AdminVisaoGeralPage() {
  const { token } = useAuth();
  const [data, setData] = useState<AdminVisaoGeral | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!token) return;
    try {
      const res = await adminApi.visaoGeral(token);
      setData(res);
    } catch {
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

  if (loading) {
    return (
      <div className="flex flex-col gap-4">
        <h1 className="text-xl font-bold text-text dark:text-text-dark">Visão geral</h1>
        <p className="text-text-muted dark:text-text-dark-muted">Carregando...</p>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex flex-col gap-4">
        <h1 className="text-xl font-bold text-text dark:text-text-dark">Visão geral</h1>
        <p className="text-text-muted dark:text-text-dark-muted">Erro ao carregar dados.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-bold text-text dark:text-text-dark">Visão geral</h1>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Card>
          <CardContent className="flex items-center gap-3 pt-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
              <Users className="h-5 w-5 text-primary" />
            </div>
            <div>
              <p className="text-xs text-text-muted dark:text-text-dark-muted">Clientes</p>
              <p className="text-lg font-bold text-text dark:text-text-dark">{data.totalClientes}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 pt-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-secondary/20">
              <Briefcase className="h-5 w-5 text-secondary" />
            </div>
            <div>
              <p className="text-xs text-text-muted dark:text-text-dark-muted">Prestadores</p>
              <p className="text-lg font-bold text-text dark:text-text-dark">{data.totalPrestadores}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 pt-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-green-100 dark:bg-green-900/30">
              <Briefcase className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
            <div>
              <p className="text-xs text-text-muted dark:text-text-dark-muted">Assinaturas ativas</p>
              <p className="text-lg font-bold text-text dark:text-text-dark">{data.prestadoresComAssinaturaAtiva}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 pt-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100 dark:bg-blue-900/30">
              <CalendarCheck className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-xs text-text-muted dark:text-text-dark-muted">Agendamentos total</p>
              <p className="text-lg font-bold text-text dark:text-text-dark">
                {Object.values(data.agendamentosPorStatus || {}).reduce((a, b) => a + b, 0)}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <Card>
          <CardContent className="pt-4">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-green-100 dark:bg-green-900/30">
                <DollarSign className="h-6 w-6 text-green-600 dark:text-green-400" />
              </div>
              <div>
                <p className="text-sm text-text-muted dark:text-text-dark-muted">GMV</p>
                <p className="text-2xl font-bold text-text dark:text-text-dark">{formatCurrency(data.gmv)}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
                <TrendingUp className="h-6 w-6 text-primary" />
              </div>
              <div>
                <p className="text-sm text-text-muted dark:text-text-dark-muted">Receita plataforma</p>
                <p className="text-2xl font-bold text-text dark:text-text-dark">{formatCurrency(data.receitaPlataforma)}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
      <Card>
        <CardContent className="pt-4">
          <p className="mb-2 text-sm font-medium text-text dark:text-text-dark">Agendamentos por status</p>
          <div className="flex flex-wrap gap-2">
            {data.agendamentosPorStatus &&
              Object.entries(data.agendamentosPorStatus).map(([status, count]) => (
                <span key={status} className="rounded-full bg-secondary-100 px-3 py-1 text-sm dark:bg-secondary-800">
                  {status}: {count}
                </span>
              ))}
          </div>
        </CardContent>
      </Card>
      <div className="flex gap-4 text-sm text-text-muted dark:text-text-dark-muted">
        <span>Pendentes: <strong className="text-text dark:text-text-dark">{data.pagamentosPendentes}</strong></span>
        <span>Confirmados: <strong className="text-text dark:text-text-dark">{data.pagamentosConfirmados}</strong></span>
      </div>
    </div>
  );
}

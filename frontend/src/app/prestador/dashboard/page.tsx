"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/providers/auth-provider";
import { dashboardApi, prestadoresApi, ApiError, type DashboardMetrics } from "@/lib/api";
import { ThemeToggle } from "@/components/theme-toggle";
import { Card, CardContent } from "@/components/ui/card";
import { StarRating } from "@/components/ui/star-rating";
import {
  Briefcase,
  DollarSign,
  TrendingUp,
  Star,
  CalendarCheck,
  AlertTriangle,
  User,
} from "lucide-react";

interface PrestadorMe {
  telefone?: string;
  endereco?: { logradouro?: string; numero?: string; cidade?: string; uf?: string };
}

export default function DashboardPage() {
  const { user, token } = useAuth();
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [perfilIncompleto, setPerfilIncompleto] = useState(false);

  const checkPerfil = useCallback(async () => {
    if (!token) return;
    try {
      const data = (await prestadoresApi.getMe(token)) as PrestadorMe;
      const semTelefone = !data?.telefone?.trim();
      const semEndereco = !data?.endereco?.logradouro?.trim() || !data?.endereco?.numero?.trim();
      setPerfilIncompleto(semTelefone || semEndereco);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) setPerfilIncompleto(true);
      else setPerfilIncompleto(true);
    }
  }, [token]);

  useEffect(() => {
    checkPerfil();
  }, [checkPerfil]);

  const fetchMetrics = useCallback(async () => {
    if (!token) return;
    try {
      const data = await dashboardApi.getMetrics(token);
      setMetrics(data);
    } catch (err) {
      console.error("Erro ao buscar métricas:", err);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchMetrics();
  }, [fetchMetrics]);

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);

  const currentMonth = new Date().toLocaleDateString("pt-BR", {
    month: "long",
    year: "numeric",
  });

  return (
    <div className="flex flex-col">
      {/* Aviso perfil incompleto */}
      {perfilIncompleto && (
        <div className="bg-amber-500/15 px-4 py-3 dark:bg-amber-600/20">
          <div className="flex items-center gap-3">
            <AlertTriangle className="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-amber-800 dark:text-amber-200">
                Complete seu perfil
              </p>
              <p className="text-xs text-amber-700 dark:text-amber-300">
                Preencha telefone e endereço em Meu Perfil para receber solicitações.
              </p>
            </div>
            <Link
              href="/prestador/perfil"
              className="flex shrink-0 items-center gap-1 rounded-lg bg-amber-600 px-3 py-2 text-sm font-medium text-white hover:bg-amber-700 dark:bg-amber-500 dark:hover:bg-amber-600"
            >
              <User className="h-4 w-4" />
              Meu perfil
            </Link>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="bg-gradient-to-br from-secondary to-secondary-800 px-4 pb-8 pt-6 text-white dark:from-surface-dark dark:to-surface-dark-alt">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <p className="text-sm text-white/70">Painel do prestador</p>
            <h1 className="text-xl font-bold">
              Olá, {user?.firstName || "Prestador"}!
            </h1>
          </div>
          <ThemeToggle className="bg-white/10 text-white hover:bg-white/20 dark:bg-white/10 dark:text-white dark:hover:bg-white/20" />
        </div>

        {/* Resumo rápido */}
        <Card className="bg-white/10 backdrop-blur-sm dark:bg-white/5 dark:ring-white/10">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/20">
              <CalendarCheck className="h-6 w-6 text-white" />
            </div>
            <div>
              <p className="text-sm text-white/70">
                Resumo de {currentMonth}
              </p>
              <p className="text-lg font-bold text-white">
                {loading ? "..." : `${metrics?.trabalhosDoMes || 0} trabalhos`}
              </p>
            </div>
          </div>
        </Card>
      </div>

      {/* Métricas */}
      <div className="-mt-4 px-4">
        <div className="grid grid-cols-2 gap-3">
          {/* Ganho Bruto */}
          <Card>
            <div className="flex flex-col items-center text-center">
              <div className="mb-2 flex h-10 w-10 items-center justify-center rounded-xl bg-green-100 dark:bg-green-900/30">
                <DollarSign className="h-5 w-5 text-green-600 dark:text-green-400" />
              </div>
              <p className="text-xs text-text-muted dark:text-text-dark-muted">
                Ganho Bruto
              </p>
              {loading ? (
                <div className="mt-1 h-6 w-20 animate-pulse rounded bg-secondary-200 dark:bg-secondary-700" />
              ) : (
                <p className="text-lg font-bold text-text dark:text-text-dark">
                  {formatCurrency(metrics?.ganhoBruto || 0)}
                </p>
              )}
            </div>
          </Card>

          {/* Lucro Líquido */}
          <Card>
            <div className="flex flex-col items-center text-center">
              <div className="mb-2 flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100 dark:bg-blue-900/30">
                <TrendingUp className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              </div>
              <p className="text-xs text-text-muted dark:text-text-dark-muted">
                Lucro Líquido
              </p>
              {loading ? (
                <div className="mt-1 h-6 w-20 animate-pulse rounded bg-secondary-200 dark:bg-secondary-700" />
              ) : (
                <p className="text-lg font-bold text-text dark:text-text-dark">
                  {formatCurrency(metrics?.lucroLiquido || 0)}
                </p>
              )}
            </div>
          </Card>

          {/* Trabalhos */}
          <Card>
            <div className="flex flex-col items-center text-center">
              <div className="mb-2 flex h-10 w-10 items-center justify-center rounded-xl bg-primary-100 dark:bg-primary-900/30">
                <Briefcase className="h-5 w-5 text-primary dark:text-primary-400" />
              </div>
              <p className="text-xs text-text-muted dark:text-text-dark-muted">
                Trabalhos
              </p>
              {loading ? (
                <div className="mt-1 h-6 w-12 animate-pulse rounded bg-secondary-200 dark:bg-secondary-700" />
              ) : (
                <p className="text-lg font-bold text-text dark:text-text-dark">
                  {metrics?.trabalhosDoMes || 0}
                </p>
              )}
            </div>
          </Card>

          {/* Avaliação */}
          <Card>
            <div className="flex flex-col items-center text-center">
              <div className="mb-2 flex h-10 w-10 items-center justify-center rounded-xl bg-yellow-100 dark:bg-yellow-900/30">
                <Star className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
              </div>
              <p className="text-xs text-text-muted dark:text-text-dark-muted">
                Avaliação
              </p>
              {loading ? (
                <div className="mt-1 h-6 w-16 animate-pulse rounded bg-secondary-200 dark:bg-secondary-700" />
              ) : (
                <div className="flex flex-col items-center">
                  <StarRating
                    rating={metrics?.avaliacaoMedia || 0}
                    size="sm"
                  />
                  <p className="mt-1 text-xs text-text-muted dark:text-text-dark-muted">
                    {metrics?.avaliacaoMedia?.toFixed(1) || "0.0"} (
                    {metrics?.totalAvaliacoes || 0})
                  </p>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>

      {/* Dicas */}
      <div className="px-4 py-6">
        <Card className="border-l-4 border-l-primary bg-primary-50 dark:border-l-primary-500 dark:bg-primary-900/10">
          <CardContent>
            <p className="text-sm font-medium text-primary-800 dark:text-primary-300">
              Dica do dia
            </p>
            <p className="mt-1 text-sm text-primary-700 dark:text-primary-400">
              Mantenha sua disponibilidade atualizada para receber mais
              solicitações de clientes na sua região!
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

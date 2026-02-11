"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "@/providers/auth-provider";
import { agendamentosApi, prestadoresApi, type Agendamento } from "@/lib/api";
import { formatEndereco } from "@/lib/utils";
import { useToast } from "@/components/ui/toast";
import { PageHeader } from "@/components/layout/page-header";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { BottomSheet } from "@/components/ui/bottom-sheet";
import {
  Calendar,
  Clock,
  CheckCircle,
  XCircle,
  User,
  Inbox,
  MapPin,
  LogIn,
  LogOut,
  Navigation,
  Camera,
} from "lucide-react";

const statusConfig: Record<
  string,
  { label: string; variant: "info" | "warning" | "success" | "danger" | "primary" }
> = {
  solicitado: { label: "Pendente", variant: "warning" },
  agendado: { label: "Aceito", variant: "info" },
  em_andamento: { label: "Em andamento", variant: "primary" },
  concluido: { label: "Concluído", variant: "success" },
  cancelado: { label: "Cancelado", variant: "danger" },
  PENDENTE: { label: "Pendente", variant: "warning" },
  ACEITO: { label: "Aceito", variant: "info" },
  CONFIRMADO: { label: "Confirmado", variant: "primary" },
  REALIZADO: { label: "Concluído", variant: "success" },
  CANCELADO: { label: "Cancelado", variant: "danger" },
  RECUSADO: { label: "Recusado", variant: "danger" },
};

const tabToStatus: Record<string, string> = {
  solicitado: "PENDENTE",
  agendado: "ACEITO",
  em_andamento: "CONFIRMADO",
  concluido: "REALIZADO",
};

type TabKey = "solicitado" | "agendado" | "em_andamento" | "concluido";

function isStatus(ag: Agendamento, key: string): boolean {
  const s = ag.status?.toUpperCase?.() ?? ag.status;
  if (key === "solicitado") return s === "PENDENTE";
  if (key === "agendado") return s === "ACEITO" || s === "CONFIRMADO";
  if (key === "em_andamento") return s === "CONFIRMADO";
  if (key === "concluido") return s === "REALIZADO";
  return false;
}

export default function SolicitacoesPage() {
  const { token } = useAuth();
  const { toast } = useToast();
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<TabKey>("solicitado");
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [checkoutAgendamento, setCheckoutAgendamento] = useState<Agendamento | null>(null);
  const [checkoutFoto, setCheckoutFoto] = useState<File | null>(null);
  const checkoutFotoInputRef = useRef<HTMLInputElement>(null);

  const fetchAgendamentos = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const statusParam = tabToStatus[tab];
      const data = await prestadoresApi.solicitacoes(
        token,
        statusParam ? { status: statusParam } : undefined
      );
      setAgendamentos(data);
    } catch {
      // silently handle
    } finally {
      setLoading(false);
    }
  }, [token, tab]);

  useEffect(() => {
    fetchAgendamentos();
  }, [fetchAgendamentos]);

  const handleAceitar = async (id: string) => {
    if (!token) return;
    setActionLoading(id);
    try {
      await agendamentosApi.aceitar(token, id);
      toast("Agendamento aceito! O cliente será notificado.", "success");
      fetchAgendamentos();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Erro ao aceitar agendamento";
      toast(msg, "error");
      fetchAgendamentos();
    } finally {
      setActionLoading(null);
    }
  };

  const handleRecusar = async (id: string) => {
    if (!token) return;
    setActionLoading(id);
    try {
      await agendamentosApi.recusar(token, id);
      toast("Agendamento recusado", "info");
      fetchAgendamentos();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Erro ao recusar agendamento";
      toast(msg, "error");
      fetchAgendamentos();
    } finally {
      setActionLoading(null);
    }
  };

  const getCurrentPosition = (): Promise<{ lat: number; lng: number }> => {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error("Geolocalização não suportada"));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (pos) =>
          resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        (err) => reject(err),
        { enableHighAccuracy: true, timeout: 10000 }
      );
    });
  };

  const handleCheckin = async (id: string) => {
    if (!token) return;
    setActionLoading(id);
    try {
      const pos = await getCurrentPosition();
      await agendamentosApi.checkin(token, id, pos.lat, pos.lng);
      toast("Check-in realizado! Bom trabalho!", "success");
      fetchAgendamentos();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Erro ao fazer check-in. Verifique se a localização está ativada.";
      toast(msg, "error");
    } finally {
      setActionLoading(null);
    }
  };

  const openCheckoutSheet = (ag: Agendamento) => {
    setCheckoutAgendamento(ag);
    setCheckoutFoto(null);
  };

  const closeCheckoutSheet = () => {
    setCheckoutAgendamento(null);
    setCheckoutFoto(null);
    if (checkoutFotoInputRef.current) checkoutFotoInputRef.current.value = "";
  };

  const handleCheckoutComFoto = async () => {
    if (!token || !checkoutAgendamento || !checkoutFoto) {
      toast("Selecione uma foto do trabalho realizado para concluir.", "error");
      return;
    }
    setActionLoading(checkoutAgendamento.id);
    try {
      const pos = await getCurrentPosition();
      await agendamentosApi.checkoutComFoto(token, checkoutAgendamento.id, pos.lat, pos.lng, checkoutFoto);
      toast("Serviço concluído! Foto anexada.", "success");
      closeCheckoutSheet();
      fetchAgendamentos();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Erro ao concluir. Verifique a localização e tente novamente.";
      toast(msg, "error");
    } finally {
      setActionLoading(null);
    }
  };

  const openMaps = (endereco: string) => {
    const encoded = encodeURIComponent(endereco);
    window.open(`https://www.google.com/maps/search/?api=1&query=${encoded}`, "_blank");
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formaPagamentoLabel: Record<string, string> = {
    ONLINE: "PIX / Cartão (online)",
    DINHEIRO: "Dinheiro",
    pix: "PIX",
    cartao: "Cartão",
    dinheiro: "Dinheiro",
  };

  return (
    <div className="flex flex-col">
      <PageHeader title="Solicitações" />

      {/* Tabs */}
      <div className="flex overflow-x-auto border-b border-secondary-100 px-4 dark:border-secondary-800">
        {(["solicitado", "agendado", "em_andamento", "concluido"] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`shrink-0 flex-1 border-b-2 py-3 text-center text-sm font-medium transition-colors ${
              tab === t
                ? "border-primary text-primary"
                : "border-transparent text-text-muted dark:text-text-dark-muted"
            }`}
          >
            {statusConfig[t].label}
          </button>
        ))}
      </div>

      {/* Lista */}
      <div className="flex flex-col gap-3 px-4 py-4">
        {loading ? (
          [1, 2, 3].map((i) => (
            <Card key={i} className="animate-pulse">
              <div className="space-y-3">
                <div className="h-4 w-3/4 rounded bg-secondary-200 dark:bg-secondary-700" />
                <div className="h-3 w-1/2 rounded bg-secondary-200 dark:bg-secondary-700" />
              </div>
            </Card>
          ))
        ) : agendamentos.length === 0 ? (
          <Card className="py-12 text-center">
            <Inbox className="mx-auto mb-3 h-12 w-12 text-secondary-300 dark:text-secondary-600" />
            <p className="font-medium text-text dark:text-text-dark">
              Nenhuma solicitação
            </p>
            <p className="mt-1 text-sm text-text-muted dark:text-text-dark-muted">
              {tab === "solicitado"
                ? "Quando um cliente solicitar um serviço, aparecerá aqui."
                : "Nenhum agendamento nesta categoria."}
            </p>
          </Card>
        ) : (
          agendamentos.map((ag) => {
            const status = statusConfig[ag.status] ?? statusConfig[String(ag.status).toUpperCase()] ?? statusConfig.solicitado;
            const isLoading = actionLoading === ag.id;
            const checkinAt = ag.checkinAt ?? (ag as { checkinEm?: string }).checkinEm;
            const checkoutAt = ag.checkoutAt ?? (ag as { checkoutEm?: string }).checkoutEm;
            return (
              <Card key={ag.id}>
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-secondary-100 dark:bg-secondary-800">
                      <User className="h-5 w-5 text-secondary-500" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-text dark:text-text-dark">
                        {ag.clienteNome || "Cliente"}
                      </h3>
                      <Badge variant={status.variant}>{status.label}</Badge>
                    </div>
                  </div>
                  {ag.formaPagamento && (
                    <Badge variant="default">
                      {formaPagamentoLabel[ag.formaPagamento] || ag.formaPagamento}
                    </Badge>
                  )}
                </div>

                <CardContent className="mt-3 space-y-1.5">
                  <p className="flex items-center gap-2 text-sm">
                    <Calendar className="h-4 w-4 text-primary" />
                    {formatDate(ag.dataHora)}
                  </p>
                  <p className="flex items-center gap-2 text-sm">
                    <Clock className="h-4 w-4 text-primary" />
                    {ag.duracao != null ? `${ag.duracao} min` : "Duração não informada"}
                  </p>
                  {ag.endereco && (
                    <button
                      onClick={() => openMaps(formatEndereco(ag.endereco))}
                      className="flex items-center gap-2 text-sm text-primary hover:underline"
                    >
                      <MapPin className="h-4 w-4" />
                      {formatEndereco(ag.endereco)}
                      <Navigation className="h-3 w-3" />
                    </button>
                  )}
                  {(ag.observacao || ag.descricao) && (
                    <p className="mt-2 rounded-lg bg-secondary-50 p-2 text-sm dark:bg-secondary-900/30">
                      {ag.observacao || ag.descricao}
                    </p>
                  )}
                  {checkinAt && (
                    <p className="flex items-center gap-2 text-xs text-success">
                      <LogIn className="h-3.5 w-3.5" />
                      Check-in: {formatDate(checkinAt)}
                    </p>
                  )}
                  {checkoutAt && (
                    <p className="flex items-center gap-2 text-xs text-success">
                      <LogOut className="h-3.5 w-3.5" />
                      Check-out: {formatDate(checkoutAt)}
                    </p>
                  )}
                </CardContent>

                <CardFooter>
                  {/* Aceitar/Recusar — pendentes */}
                  {isStatus(ag, "solicitado") && (
                    <>
                      <Button
                        size="sm"
                        icon={<CheckCircle className="h-4 w-4" />}
                        onClick={() => handleAceitar(ag.id)}
                        loading={isLoading}
                        className="flex-1"
                      >
                        Aceitar
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        icon={<XCircle className="h-4 w-4" />}
                        onClick={() => handleRecusar(ag.id)}
                        loading={isLoading}
                        className="flex-1"
                      >
                        Recusar
                      </Button>
                    </>
                  )}

                  {/* Check-in — agendados */}
                  {isStatus(ag, "agendado") && !checkinAt && (
                    <Button
                      size="sm"
                      icon={<LogIn className="h-4 w-4" />}
                      onClick={() => handleCheckin(ag.id)}
                      loading={isLoading}
                      className="flex-1"
                    >
                      Fazer Check-in
                    </Button>
                  )}

                  {/* Check-out — em andamento (exige check-in feito) */}
                  {(isStatus(ag, "agendado") || isStatus(ag, "em_andamento")) &&
                    checkinAt &&
                    !checkoutAt && (
                      <Button
                        size="sm"
                        variant="secondary"
                        icon={<LogOut className="h-4 w-4" />}
                        onClick={() => openCheckoutSheet(ag)}
                        loading={isLoading}
                        className="flex-1"
                      >
                        Concluir (anexar foto)
                      </Button>
                    )}
                </CardFooter>
              </Card>
            );
          })
        )}
      </div>

      {/* Sheet: Checkout com foto do trabalho */}
      <BottomSheet
        open={!!checkoutAgendamento}
        onClose={closeCheckoutSheet}
        title="Concluir atendimento"
      >
        {checkoutAgendamento && (
          <div className="flex flex-col gap-4">
            <p className="text-sm text-text-muted dark:text-text-dark-muted">
              Anexe uma foto do trabalho realizado para dar o atendimento como concluído.
            </p>
            <input
              ref={checkoutFotoInputRef}
              type="file"
              accept="image/*"
              capture="environment"
              className="block w-full text-sm text-text-muted dark:text-text-dark-muted file:mr-3 file:rounded file:border-0 file:bg-primary file:px-4 file:py-2 file:text-sm file:font-medium file:text-white"
              onChange={(e) => setCheckoutFoto(e.target.files?.[0] ?? null)}
            />
            {checkoutFoto && (
              <p className="text-xs text-success">
                <Camera className="inline h-3.5 w-3.5 mr-1" />
                {checkoutFoto.name}
              </p>
            )}
            <div className="flex gap-2">
              <Button variant="outline" className="flex-1" onClick={closeCheckoutSheet}>
                Cancelar
              </Button>
              <Button
                className="flex-1"
                icon={<LogOut className="h-4 w-4" />}
                onClick={handleCheckoutComFoto}
                disabled={!checkoutFoto}
                loading={actionLoading === checkoutAgendamento.id}
              >
                Concluir
              </Button>
            </div>
          </div>
        )}
      </BottomSheet>
    </div>
  );
}

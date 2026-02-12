"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/providers/auth-provider";
import { agendamentosApi, avaliacoesApi, type Agendamento } from "@/lib/api";
import { formatEndereco } from "@/lib/utils";
import { useToast } from "@/components/ui/toast";
import { PageHeader } from "@/components/layout/page-header";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { BottomSheet } from "@/components/ui/bottom-sheet";
import { StarRating } from "@/components/ui/star-rating";
import { Textarea } from "@/components/ui/textarea";
import {
  Calendar,
  Clock,
  CreditCard,
  XCircle,
  Star,
  MapPin,
  MessageCircle,
} from "lucide-react";

const statusConfig: Record<
  string,
  { label: string; variant: "info" | "warning" | "success" | "danger" | "primary" }
> = {
  solicitado: { label: "Solicitado", variant: "warning" },
  agendado: { label: "Agendado", variant: "info" },
  em_andamento: { label: "Em andamento", variant: "primary" },
  concluido: { label: "Concluído", variant: "success" },
  cancelado: { label: "Cancelado", variant: "danger" },
  PENDENTE: { label: "Solicitado", variant: "warning" },
  ACEITO: { label: "Agendado", variant: "info" },
  CONFIRMADO: { label: "Agendado", variant: "info" },
  REALIZADO: { label: "Concluído", variant: "success" },
  CANCELADO: { label: "Cancelado", variant: "danger" },
  RECUSADO: { label: "Recusado", variant: "danger" },
};

const tabs = ["todos", "solicitado", "agendado", "em_andamento", "concluido"];

function isStatus(ag: Agendamento, key: string): boolean {
  const s = ag.status?.toUpperCase?.() ?? ag.status;
  if (key === "solicitado") return s === "PENDENTE";
  if (key === "agendado") return s === "ACEITO" || s === "CONFIRMADO";
  if (key === "em_andamento") return s === "CONFIRMADO";
  if (key === "concluido") return s === "REALIZADO";
  if (key === "cancelado") return s === "CANCELADO" || s === "RECUSADO";
  return false;
}

export default function AgendamentosPage() {
  const { token } = useAuth();
  const { toast } = useToast();
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([]);
  const [loading, setLoading] = useState(true);
  const [tabAtiva, setTabAtiva] = useState("todos");

  // Confirmar pagamento (loading por agendamento)
  const [confirmarPagamentoLoading, setConfirmarPagamentoLoading] = useState<string | null>(null);
  // Pagar agora (abrir link) — loading por agendamento
  const [pagarAgoraLoading, setPagarAgoraLoading] = useState<string | null>(null);

  // Avaliação
  const [showAvaliacao, setShowAvaliacao] = useState(false);
  const [avalAgendamento, setAvalAgendamento] = useState<Agendamento | null>(null);
  const [avalNota, setAvalNota] = useState(5);
  const [avalComentario, setAvalComentario] = useState("");
  const [avalSending, setAvalSending] = useState(false);

  const fetchAgendamentos = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const params: Record<string, string> = {};
      if (tabAtiva !== "todos") params.status = tabAtiva;
      const data = await agendamentosApi.listar(token, params);
      setAgendamentos(data);
    } catch {
      // silently handle
    } finally {
      setLoading(false);
    }
  }, [token, tabAtiva]);

  useEffect(() => {
    fetchAgendamentos();
  }, [fetchAgendamentos]);

  const handleCancelar = async (id: string) => {
    if (!token) return;
    try {
      await agendamentosApi.cancelar(token, id);
      toast("Agendamento cancelado", "success");
      fetchAgendamentos();
    } catch {
      toast("Erro ao cancelar agendamento", "error");
    }
  };

  const handleConfirmarPagamento = async (id: string) => {
    if (!token) return;
    setConfirmarPagamentoLoading(id);
    try {
      await agendamentosApi.confirmarPagamento(token, id);
      toast("Pagamento confirmado! O prestador poderá fazer o check-in.", "success");
      fetchAgendamentos();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Erro ao confirmar pagamento";
      toast(msg, "error");
    } finally {
      setConfirmarPagamentoLoading(null);
    }
  };

  /** Menos de 1 hora até o horário do agendamento (pagamento deve ser feito antes). */
  const menosDe1h = (dataHoraStr: string) => {
    const ag = new Date(dataHoraStr).getTime();
    const umaHora = 60 * 60 * 1000;
    return ag - Date.now() < umaHora;
  };

  const handlePagarAgora = async (ag: Agendamento) => {
    if (!token) return;
    if (menosDe1h(ag.dataHora)) {
      toast("Pagamento não disponível: faltam menos de 1 hora para o atendimento.", "error");
      return;
    }
    setPagarAgoraLoading(ag.id);
    try {
      const pag = await agendamentosApi.getPagamento(token, ag.id);
      if (pag?.linkPagamento) {
        window.open(pag.linkPagamento, "_blank");
      } else {
        toast("Link de pagamento não disponível.", "error");
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Erro ao obter link de pagamento";
      toast(msg, "error");
    } finally {
      setPagarAgoraLoading(null);
    }
  };

  const openAvaliacao = (ag: Agendamento) => {
    setAvalAgendamento(ag);
    setAvalNota(5);
    setAvalComentario("");
    setShowAvaliacao(true);
  };

  const handleAvaliar = async () => {
    if (!token || !avalAgendamento) return;
    setAvalSending(true);
    try {
      await avaliacoesApi.criar(token, {
        agendamentoId: avalAgendamento.id,
        nota: avalNota,
        comentario: avalComentario || undefined,
      });
      toast("Avaliação enviada com sucesso!", "success");
      setShowAvaliacao(false);
      fetchAgendamentos();
    } catch {
      toast("Erro ao enviar avaliação", "error");
    } finally {
      setAvalSending(false);
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "short",
      year: "numeric",
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
      <PageHeader title="Meus Agendamentos" />

      {/* Tabs de status */}
      <div className="overflow-x-auto border-b border-secondary-100 px-4 dark:border-secondary-800">
        <div className="flex gap-1">
          {tabs.map((tab) => (
            <button
              key={tab}
              onClick={() => setTabAtiva(tab)}
              className={`shrink-0 border-b-2 px-3 py-2.5 text-sm font-medium capitalize transition-colors ${
                tabAtiva === tab
                  ? "border-primary text-primary"
                  : "border-transparent text-text-muted dark:text-text-dark-muted"
              }`}
            >
              {tab === "todos"
                ? "Todos"
                : tab === "em_andamento"
                  ? "Em andamento"
                  : statusConfig[tab]?.label || tab}
            </button>
          ))}
        </div>
      </div>

      {/* Lista */}
      <div className="flex flex-col gap-3 px-4 py-4">
        {loading ? (
          [1, 2, 3].map((i) => (
            <Card key={i} className="animate-pulse">
              <div className="space-y-3">
                <div className="h-4 w-3/4 rounded bg-secondary-200 dark:bg-secondary-700" />
                <div className="h-3 w-1/2 rounded bg-secondary-200 dark:bg-secondary-700" />
                <div className="h-3 w-1/3 rounded bg-secondary-200 dark:bg-secondary-700" />
              </div>
            </Card>
          ))
        ) : agendamentos.length === 0 ? (
          <Card className="py-12 text-center">
            <Calendar className="mx-auto mb-3 h-12 w-12 text-secondary-300 dark:text-secondary-600" />
            <p className="font-medium text-text dark:text-text-dark">
              Nenhum agendamento encontrado
            </p>
            <p className="mt-1 text-sm text-text-muted dark:text-text-dark-muted">
              Busque um prestador e solicite um serviço.
            </p>
          </Card>
        ) : (
          agendamentos.map((ag) => {
            const status = statusConfig[ag.status] ?? statusConfig[String(ag.status).toUpperCase()] ?? statusConfig.solicitado;
            return (
              <Card key={ag.id}>
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="font-semibold text-text dark:text-text-dark">
                      {ag.prestadorNome || "Prestador"}
                    </h3>
                    <Badge variant={status.variant} className="mt-1">
                      {status.label}
                    </Badge>
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
                    <p className="flex items-center gap-2 text-sm">
                      <MapPin className="h-4 w-4 text-primary" />
                      {formatEndereco(ag.endereco)}
                    </p>
                  )}
                  {(ag.observacao || ag.descricao) && (
                    <p className="mt-2 rounded-lg bg-secondary-50 p-2 text-sm dark:bg-secondary-900/30">
                      {ag.observacao || ag.descricao}
                    </p>
                  )}
                </CardContent>

                <CardFooter className="flex-wrap">
                  {/* Pagar agora — ACEITO + ONLINE: obtém link e abre; desabilitado se < 1h */}
                  {(ag.status?.toUpperCase?.() ?? ag.status) === "ACEITO" &&
                    ag.formaPagamento === "ONLINE" && (
                    <>
                      {menosDe1h(ag.dataHora) && (
                        <p className="w-full text-xs text-amber-600 dark:text-amber-400">
                          Pagamento indisponível: faltam menos de 1 hora para o atendimento.
                        </p>
                      )}
                      <Button
                        size="sm"
                        icon={<CreditCard className="h-4 w-4" />}
                        onClick={() => handlePagarAgora(ag)}
                        loading={pagarAgoraLoading === ag.id}
                        disabled={menosDe1h(ag.dataHora)}
                      >
                        Pagar agora
                      </Button>
                    </>
                  )}

                  {/* Confirmar pagamento — cliente informa que já pagou (ACEITO → CONFIRMADO) */}
                  {(ag.status?.toUpperCase?.() ?? ag.status) === "ACEITO" && (
                    <Button
                      size="sm"
                      variant="outline"
                      icon={<CreditCard className="h-4 w-4" />}
                      onClick={() => handleConfirmarPagamento(ag.id)}
                      loading={confirmarPagamentoLoading === ag.id}
                      disabled={menosDe1h(ag.dataHora)}
                    >
                      Já paguei
                    </Button>
                  )}

                  {/* WhatsApp */}
                  {(isStatus(ag, "agendado") || isStatus(ag, "em_andamento")) && (
                    <Button
                      size="sm"
                      variant="outline"
                      icon={<MessageCircle className="h-4 w-4" />}
                      onClick={() =>
                        window.open(
                          `https://wa.me/?text=${encodeURIComponent(
                            `Olá! Sobre o agendamento do dia ${formatDate(ag.dataHora)}`
                          )}`,
                          "_blank"
                        )
                      }
                    >
                      WhatsApp
                    </Button>
                  )}

                  {/* Cancelar */}
                  {(isStatus(ag, "solicitado") || isStatus(ag, "agendado")) && (
                    <Button
                      size="sm"
                      variant="danger"
                      icon={<XCircle className="h-4 w-4" />}
                      onClick={() => handleCancelar(ag.id)}
                    >
                      Cancelar
                    </Button>
                  )}

                  {/* Avaliar — disponível em concluídos sem avaliação */}
                  {isStatus(ag, "concluido") &&
                    ag.podeFazerAvaliacao &&
                    !ag.avaliacaoId && (
                      <Button
                        size="sm"
                        variant="secondary"
                        icon={<Star className="h-4 w-4" />}
                        onClick={() => openAvaliacao(ag)}
                      >
                        Avaliar
                      </Button>
                    )}

                  {ag.avaliacaoId && (
                    <Badge variant="success">Avaliado</Badge>
                  )}
                </CardFooter>
              </Card>
            );
          })
        )}
      </div>

      {/* ───── Bottom Sheet de Avaliação ───── */}
      <BottomSheet
        open={showAvaliacao}
        onClose={() => setShowAvaliacao(false)}
        title="Avaliar serviço"
      >
        {avalAgendamento && (
          <div className="flex flex-col items-center gap-4">
            <p className="text-sm text-text-muted dark:text-text-dark-muted">
              Como foi o serviço de{" "}
              <strong>{avalAgendamento.prestadorNome}</strong>?
            </p>

            <StarRating
              rating={avalNota}
              size="lg"
              interactive
              onChange={setAvalNota}
            />

            <p className="text-2xl font-bold text-primary">
              {avalNota === 5
                ? "Excelente!"
                : avalNota === 4
                  ? "Muito bom"
                  : avalNota === 3
                    ? "Regular"
                    : avalNota === 2
                      ? "Ruim"
                      : "Péssimo"}
            </p>

            <Textarea
              label="Deixe um depoimento (opcional)"
              placeholder="Conte como foi sua experiência..."
              rows={3}
              value={avalComentario}
              onChange={(e) => setAvalComentario(e.target.value)}
            />

            <Button
              onClick={handleAvaliar}
              loading={avalSending}
              size="lg"
              className="w-full"
              icon={<Star className="h-5 w-5" />}
            >
              Enviar avaliação
            </Button>
          </div>
        )}
      </BottomSheet>
    </div>
  );
}

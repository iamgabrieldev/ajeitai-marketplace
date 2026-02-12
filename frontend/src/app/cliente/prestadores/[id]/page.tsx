"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Image from "next/image";
import { useAuth } from "@/providers/auth-provider";
import { prestadoresApi, agendamentosApi, chatApi, type PrestadorDetalhe as PrestadorDetalheApi } from "@/lib/api";
import { PageHeader } from "@/components/layout/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { StarRating } from "@/components/ui/star-rating";
import { Select } from "@/components/ui/select";
import { useToast } from "@/components/ui/toast";
import {
  MapPin,
  Phone,
  MessageCircle,
  Calendar,
  Clock,
  X,
} from "lucide-react";

interface PrestadorDetalhe {
  id: string;
  nomeFantasia: string;
  categoria: string;
  descricao?: string;
  valorHora?: number;
  avaliacao: number;
  totalAvaliacoes: number;
  fotoUrl?: string;
  cidade?: string;
  telefone?: string;
  portfolio?: string[];
  keycloakId?: string;
}

export default function PrestadorPerfilPage() {
  const params = useParams();
  const router = useRouter();
  const { token } = useAuth();
  const [prestador, setPrestador] = useState<PrestadorDetalhe | null>(null);
  const [loading, setLoading] = useState(true);
  const [showAgendamento, setShowAgendamento] = useState(false);
  const [chatLoading, setChatLoading] = useState(false);
  const { toast } = useToast();
  const [agendForm, setAgendForm] = useState({
    dataHora: "",
    duracao: "60",
    descricao: "",
    formaPagamento: "pix",
  });
  const [submitting, setSubmitting] = useState(false);

  const fetchPrestador = useCallback(async () => {
    if (!token || !params.id) return;
    setLoading(true);
    try {
      const data = await prestadoresApi.getById(token, params.id as string) as PrestadorDetalheApi;
      setPrestador({
        id: String(data.id),
        nomeFantasia: data.nomeFantasia,
        categoria: String(data.categoria),
        valorHora: data.valorServico,
        avaliacao: data.mediaAvaliacao ?? 0,
        totalAvaliacoes: data.totalAvaliacoes ?? 0,
        fotoUrl: data.avatarUrl,
        cidade: data.cidade,
        portfolio: (data.portfolio ?? []).map((p) => p.imagemUrl).filter(Boolean) as string[],
        keycloakId: data.keycloakId,
      });
    } catch (err) {
      console.error("Erro ao buscar prestador:", err);
    } finally {
      setLoading(false);
    }
  }, [token, params.id]);

  useEffect(() => {
    fetchPrestador();
  }, [fetchPrestador]);

  const handleAgendar = async () => {
    if (!token || !prestador) return;
    setSubmitting(true);
    try {
      // Validate 30 min advance
      const agendDate = new Date(agendForm.dataHora);
      const now = new Date();
      const diffMin = (agendDate.getTime() - now.getTime()) / 60000;
      if (diffMin < 30) {
        toast("O agendamento deve ser com pelo menos 30 minutos de antecedência", "warning");
        setSubmitting(false);
        return;
      }

      await agendamentosApi.criar(token, {
        prestadorId: Number(prestador.id),
        dataHora: agendForm.dataHora,
        formaPagamento: agendForm.formaPagamento === "dinheiro" ? "DINHEIRO" : "ONLINE",
        observacao: agendForm.descricao || undefined,
      });
      toast("Solicitação enviada! O prestador será notificado.", "success");
      setShowAgendamento(false);
      router.push("/cliente/agendamentos");
    } catch {
      toast("Erro ao solicitar agendamento", "error");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="animate-pulse space-y-4 p-4">
        <div className="h-48 rounded-2xl bg-secondary-200 dark:bg-secondary-700" />
        <div className="h-6 w-2/3 rounded bg-secondary-200 dark:bg-secondary-700" />
        <div className="h-4 w-1/2 rounded bg-secondary-200 dark:bg-secondary-700" />
      </div>
    );
  }

  if (!prestador) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-text-muted dark:text-text-dark-muted">
          Prestador não encontrado.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col">
      <PageHeader title={prestador.nomeFantasia} back />

      {/* Foto / Banner */}
      <div className="relative h-48 w-full bg-secondary-100 dark:bg-secondary-800">
        {prestador.fotoUrl ? (
          <Image
            src={prestador.fotoUrl}
            alt={prestador.nomeFantasia}
            fill
            className="object-cover"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-6xl font-bold text-primary/30">
            {prestador.nomeFantasia.charAt(0)}
          </div>
        )}
      </div>

      <div className="px-4 py-4">
        {/* Info básica */}
        <div className="mb-4">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-xl font-bold text-text dark:text-text-dark">
                {prestador.nomeFantasia}
              </h1>
              <Badge variant="primary" className="mt-1">
                {prestador.categoria}
              </Badge>
            </div>
            {prestador.valorHora && (
              <div className="text-right">
                <p className="text-lg font-bold text-primary">
                  R$ {prestador.valorHora.toFixed(2)}
                </p>
                <span className="text-xs text-text-muted dark:text-text-dark-muted">
                  por hora
                </span>
              </div>
            )}
          </div>

          <div className="mt-3 flex items-center gap-3">
            <StarRating rating={prestador.avaliacao} size="md" />
            <span className="text-sm text-text-muted dark:text-text-dark-muted">
              {prestador.avaliacao?.toFixed(1)} ({prestador.totalAvaliacoes}{" "}
              avaliações)
            </span>
          </div>

          {prestador.cidade && (
            <p className="mt-2 flex items-center gap-1 text-sm text-text-muted dark:text-text-dark-muted">
              <MapPin className="h-4 w-4" />
              {prestador.cidade}
            </p>
          )}
        </div>

        {/* Descrição */}
        {prestador.descricao && (
          <Card className="mb-4">
            <CardContent>
              <p className="text-sm leading-relaxed">{prestador.descricao}</p>
            </CardContent>
          </Card>
        )}

        {/* Portfólio */}
        {prestador.portfolio && prestador.portfolio.length > 0 && (
          <div className="mb-4">
            <h2 className="mb-2 text-base font-semibold text-text dark:text-text-dark">
              Portfólio
            </h2>
            <div className="grid grid-cols-3 gap-2">
              {prestador.portfolio.map((img, idx) => (
                <div
                  key={idx}
                  className="relative aspect-square overflow-hidden rounded-xl"
                >
                  <Image
                    src={img}
                    alt={`Trabalho ${idx + 1}`}
                    fill
                    className="object-cover"
                  />
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Ações */}
        <div className="flex gap-3">
          {prestador.telefone && (
            <Button
              variant="outline"
              className="flex-1"
              icon={<Phone className="h-5 w-5" />}
              onClick={() =>
                window.open(`tel:${prestador.telefone}`, "_self")
              }
            >
              Ligar
            </Button>
          )}
          <Button
            variant="outline"
            className="flex-1"
            icon={<MessageCircle className="h-5 w-5" />}
            loading={chatLoading}
            disabled={!prestador.keycloakId}
            onClick={async () => {
              if (!token || !prestador.keycloakId) {
                if (!prestador.keycloakId) toast("Chat indisponível para este prestador.", "info");
                return;
              }
              setChatLoading(true);
              try {
                const conv = await chatApi.criarOuBuscarConversa(token, prestador.keycloakId, undefined);
                router.push(`/cliente/conversas/${conv.id}`);
              } catch (e) {
                toast(e instanceof Error ? e.message : "Erro ao abrir o chat.", "error");
              } finally {
                setChatLoading(false);
              }
            }}
          >
            Chat
          </Button>
        </div>

        <Button
          className="mt-3 w-full"
          size="lg"
          icon={<Calendar className="h-5 w-5" />}
          onClick={() => setShowAgendamento(true)}
        >
          Solicitar Agendamento
        </Button>
      </div>

      {/* Modal de agendamento */}
      {showAgendamento && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 p-4">
          <div className="w-full max-w-lg rounded-t-3xl bg-white p-6 dark:bg-surface-dark-alt">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-lg font-bold text-text dark:text-text-dark">
                Solicitar Agendamento
              </h3>
              <button
                onClick={() => setShowAgendamento(false)}
                className="rounded-full p-1 hover:bg-secondary-100 dark:hover:bg-secondary-800"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="flex flex-col gap-4">
              <Input
                label="Data e Hora"
                type="datetime-local"
                icon={<Calendar className="h-4 w-4" />}
                value={agendForm.dataHora}
                onChange={(e) =>
                  setAgendForm((prev) => ({
                    ...prev,
                    dataHora: e.target.value,
                  }))
                }
              />
              <Input
                label="Duração (minutos)"
                type="number"
                min="30"
                step="30"
                icon={<Clock className="h-4 w-4" />}
                value={agendForm.duracao}
                onChange={(e) =>
                  setAgendForm((prev) => ({
                    ...prev,
                    duracao: e.target.value,
                  }))
                }
              />
              <Textarea
                label="Descrição do serviço"
                placeholder="Descreva o que precisa ser feito..."
                rows={3}
                value={agendForm.descricao}
                onChange={(e) =>
                  setAgendForm((prev) => ({
                    ...prev,
                    descricao: e.target.value,
                  }))
                }
              />
              <Select
                label="Forma de pagamento"
                options={[
                  { value: "pix", label: "PIX" },
                  { value: "cartao", label: "Cartão de crédito/débito" },
                  { value: "dinheiro", label: "Dinheiro (direto com prestador)" },
                ]}
                value={agendForm.formaPagamento}
                onChange={(e) =>
                  setAgendForm((prev) => ({
                    ...prev,
                    formaPagamento: e.target.value,
                  }))
                }
              />
              <Button
                onClick={handleAgendar}
                loading={submitting}
                size="lg"
                className="w-full"
              >
                Confirmar Solicitação
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

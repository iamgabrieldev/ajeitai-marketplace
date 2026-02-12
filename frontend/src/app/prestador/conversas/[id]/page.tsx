"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/providers/auth-provider";
import { chatApi, type MensagemChat } from "@/lib/api";
import { PageHeader } from "@/components/layout/page-header";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/toast";
import { ArrowLeft, Send } from "lucide-react";
import Link from "next/link";

export default function PrestadorConversaPage() {
  const params = useParams();
  const router = useRouter();
  const { token, user } = useAuth();
  const { toast } = useToast();
  const conversaId = params.id as string;
  const [mensagens, setMensagens] = useState<MensagemChat[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  const meuId = user?.id ?? "";

  const carregar = useCallback(async () => {
    if (!token || !conversaId) return;
    setLoading(true);
    setErro(null);
    try {
      const list = await chatApi.listarMensagens(token, conversaId);
      setMensagens(list);
      setTimeout(() => listRef.current?.scrollTo(0, listRef.current.scrollHeight), 50);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar mensagens");
    } finally {
      setLoading(false);
    }
  }, [token, conversaId]);

  useEffect(() => {
    carregar();
    const t = setInterval(carregar, 5000);
    return () => clearInterval(t);
  }, [carregar]);

  const handleEnviar = async () => {
    const msg = texto.trim();
    if (!token || !conversaId || !msg || enviando) return;
    setEnviando(true);
    try {
      const nova = await chatApi.enviarMensagem(token, conversaId, msg);
      setMensagens((prev) => [...prev, nova]);
      setTexto("");
      setTimeout(() => listRef.current?.scrollTo(0, listRef.current.scrollHeight), 50);
    } catch (e) {
      toast(e instanceof Error ? e.message : "Erro ao enviar", "error");
    } finally {
      setEnviando(false);
    }
  };

  if (!token) {
    router.replace("/");
    return null;
  }

  return (
    <>
      <PageHeader
        title="Chat"
        leftAction={
          <Link
            href="/prestador/conversas"
            className="flex items-center gap-1 text-primary dark:text-primary-400"
          >
            <ArrowLeft className="h-5 w-5" />
            Voltar
          </Link>
        }
      />
      <div className="flex h-[calc(100vh-8rem)] flex-col p-4">
        {erro && (
          <p className="mb-2 text-sm text-danger" role="alert">
            {erro}
          </p>
        )}
        <div
          ref={listRef}
          className="flex-1 space-y-3 overflow-y-auto rounded-xl border border-secondary-200 bg-secondary-50/50 p-3 dark:border-secondary-700 dark:bg-secondary-900/30"
        >
          {loading && mensagens.length === 0 && (
            <p className="text-center text-sm text-text-muted dark:text-text-dark-muted">
              Carregando...
            </p>
          )}
          {!loading && mensagens.length === 0 && !erro && (
            <p className="text-center text-sm text-text-muted dark:text-text-dark-muted">
              Nenhuma mensagem ainda. Envie uma mensagem para iniciar.
            </p>
          )}
          {mensagens.map((m) => {
            const souEu = m.remetenteId === meuId;
            return (
              <div
                key={m.id}
                className={`flex ${souEu ? "justify-end" : "justify-start"}`}
              >
                <div
                  className={`max-w-[85%] rounded-2xl px-4 py-2 text-sm ${
                    souEu
                      ? "bg-primary text-white dark:bg-primary-500"
                      : "bg-white text-text dark:bg-surface-dark dark:text-text-dark border border-secondary-200 dark:border-secondary-700"
                  }`}
                >
                  <p className="whitespace-pre-wrap break-words">{m.texto}</p>
                  <p
                    className={`mt-1 text-xs ${souEu ? "text-white/80" : "text-text-muted dark:text-text-dark-muted"}`}
                  >
                    {new Date(m.enviadaEm).toLocaleTimeString("pt-BR", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
        <div className="mt-3 flex gap-2">
          <input
            type="text"
            placeholder="Digite sua mensagem..."
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && handleEnviar()}
            className="flex-1 rounded-xl border border-secondary-200 bg-white px-4 py-3 text-text outline-none focus:ring-2 focus:ring-primary/30 dark:border-secondary-700 dark:bg-surface-dark dark:text-text-dark"
            disabled={enviando || !!erro}
          />
          <Button
            size="lg"
            icon={<Send className="h-5 w-5" />}
            onClick={handleEnviar}
            loading={enviando}
            disabled={!texto.trim() || !!erro}
          >
            Enviar
          </Button>
        </div>
      </div>
    </>
  );
}

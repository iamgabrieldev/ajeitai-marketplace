"use client";

import { useAuth } from "@/providers/auth-provider";
import { chatApi, type ConversaChat } from "@/lib/api";
import { PageHeader } from "@/components/layout/page-header";
import { useEffect, useState } from "react";
import Link from "next/link";

export default function ClienteConversasPage() {
  const { token, isAuthenticated } = useAuth();
  const [conversas, setConversas] = useState<ConversaChat[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !token) {
      setLoading(false);
      return;
    }
    chatApi
      .listarConversas(token)
      .then(setConversas)
      .catch((e) => setErro(e.message || "Erro ao carregar conversas"))
      .finally(() => setLoading(false));
  }, [isAuthenticated, token]);

  return (
    <>
      <PageHeader title="Conversas" />
      <div className="p-4">
        {loading && (
          <p className="text-secondary-600 dark:text-secondary-400">
            Carregando...
          </p>
        )}
        {erro && (
          <p className="text-danger dark:text-red-400" role="alert">
            {erro}
          </p>
        )}
        {!loading && !erro && conversas.length === 0 && (
          <p className="text-secondary-600 dark:text-secondary-400">
            Nenhuma conversa ainda. Inicie uma conversa pelo perfil do
            prestador após um agendamento.
          </p>
        )}
        {!loading && conversas.length > 0 && (
          <ul className="space-y-2">
            {conversas.map((c) => (
              <li key={c.id}>
                <Link
                  href={`/cliente/conversas/${c.id}`}
                  className="block rounded-xl border border-secondary-200 bg-white p-4 dark:border-secondary-700 dark:bg-surface-dark"
                >
                  <span className="font-medium">Conversa #{c.id.slice(-6)}</span>
                  <span className="ml-2 text-sm text-secondary-600 dark:text-secondary-400">
                    Prestador: {c.prestadorId.slice(0, 8)}...
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  );
}

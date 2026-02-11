"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { useAuth } from "@/providers/auth-provider";
import { clientesApi, ApiError, type DadosEndereco } from "@/lib/api";
import { PageHeader } from "@/components/layout/page-header";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { useToast } from "@/components/ui/toast";
import {
  Camera,
  Save,
  LogOut,
} from "lucide-react";

interface EnderecoApi {
  logradouro?: string;
  bairro?: string;
  cep?: string;
  numero?: string;
  complemento?: string;
  cidade?: string;
  uf?: string;
}

interface ClienteProfile {
  nome?: string;
  email?: string;
  telefone?: string;
  cpf?: string;
  endereco?: EnderecoApi;
  avatarUrl?: string;
}

export default function PerfilClientePage() {
  const router = useRouter();
  const { user, token, logout } = useAuth();
  const { toast } = useToast();
  const fileRef = useRef<HTMLInputElement>(null);
  const [profile, setProfile] = useState<ClienteProfile>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const fetchProfile = useCallback(async () => {
    if (!token) return;
    try {
      const data = (await clientesApi.getMe(token)) as ClienteProfile & { fotoUrl?: string };
      setProfile({
        ...data,
        endereco: data.endereco ?? {},
        avatarUrl: data.avatarUrl ?? data.fotoUrl,
      });
    } catch (err) {
      const isClienteNaoEncontrado =
        err instanceof ApiError &&
        (err.status === 404 || err.status === 400) &&
        (err.message?.includes("não encontrado") ||
          (err.data as { mensagem?: string } | undefined)?.mensagem?.includes("não encontrado"));
      if (isClienteNaoEncontrado) {
        toast("Complete seu cadastro para acessar o perfil.", "warning");
        router.replace("/cliente/welcome");
        return;
      }
      console.error("Erro ao buscar perfil:", err);
      toast("Erro ao carregar perfil.", "error");
    } finally {
      setLoading(false);
    }
  }, [token, router, toast]);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  const handleSave = async () => {
    if (!token) return;
    const e = profile.endereco ?? {};
    setSaving(true);
    try {
      const endereco: DadosEndereco = {
        logradouro: e.logradouro ?? "",
        bairro: e.bairro ?? "",
        cep: (e.cep ?? "").replace(/\D/g, "").slice(0, 8),
        cidade: e.cidade ?? "",
        uf: (e.uf ?? "").slice(0, 2),
        numero: e.numero ?? "",
        ...(e.complemento && { complemento: e.complemento }),
      };
      await clientesApi.updateMe(token, {
        nome: profile.nome,
        telefone: profile.telefone,
        endereco,
      });
      toast("Perfil atualizado com sucesso!", "success");
    } catch {
      toast("Erro ao salvar perfil", "error");
    } finally {
      setSaving(false);
    }
  };

  const handleUploadFoto = async (file: File) => {
    if (!token) return;
    try {
      await clientesApi.uploadFoto(token, file);
      fetchProfile();
    } catch (err) {
      console.error("Erro ao enviar foto:", err);
    }
  };

  const updateField = (field: keyof ClienteProfile, value: string) => {
    setProfile((prev) => ({ ...prev, [field]: value }));
  };

  const updateEndereco = (field: keyof EnderecoApi, value: string) => {
    setProfile((prev) => ({
      ...prev,
      endereco: { ...prev.endereco, [field]: value },
    }));
  };

  const end = profile.endereco ?? {};

  return (
    <div className="flex flex-col">
      <PageHeader
        title="Meu Perfil"
        actions={<ThemeToggle />}
      />

      {loading ? (
        <div className="animate-pulse space-y-4 p-4">
          <div className="mx-auto h-24 w-24 rounded-full bg-secondary-200 dark:bg-secondary-700" />
          <div className="h-4 w-1/2 rounded bg-secondary-200 dark:bg-secondary-700" />
          <div className="h-10 rounded bg-secondary-200 dark:bg-secondary-700" />
          <div className="h-10 rounded bg-secondary-200 dark:bg-secondary-700" />
        </div>
      ) : (
        <div className="px-4 py-4">
          {/* Avatar */}
          <div className="mb-6 flex flex-col items-center">
            <div className="relative">
              <div className="h-24 w-24 overflow-hidden rounded-full bg-secondary-100 dark:bg-secondary-800">
                {(profile.avatarUrl ?? (profile as { fotoUrl?: string }).fotoUrl) ? (
                  <Image
                    src={profile.avatarUrl ?? (profile as { fotoUrl?: string }).fotoUrl ?? ""}
                    alt="Avatar"
                    width={96}
                    height={96}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="flex h-full w-full items-center justify-center text-3xl font-bold text-primary">
                    {user?.firstName?.charAt(0) || "?"}
                  </div>
                )}
              </div>
              <button
                onClick={() => fileRef.current?.click()}
                className="absolute bottom-0 right-0 rounded-full bg-primary p-2 text-white shadow-md"
              >
                <Camera className="h-4 w-4" />
              </button>
              <input
                ref={fileRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) handleUploadFoto(file);
                }}
              />
            </div>
            <h2 className="mt-3 text-lg font-semibold text-text dark:text-text-dark">
              {user?.firstName} {user?.lastName}
            </h2>
            <p className="text-sm text-text-muted dark:text-text-dark-muted">
              {user?.email}
            </p>
          </div>

          {/* Formulário */}
          <Card className="mb-4">
            <div className="flex flex-col gap-4 p-2">
              <Input
                label="Nome"
                value={profile.nome || ""}
                onChange={(e) => updateField("nome", e.target.value)}
              />
              <Input
                label="Telefone"
                value={profile.telefone || ""}
                onChange={(e) => updateField("telefone", e.target.value)}
              />
              <Input
                label="CPF (somente leitura)"
                value={profile.cpf || ""}
                readOnly
                className="opacity-80"
              />
              <Input
                label="CEP"
                value={end.cep || ""}
                onChange={(e) => updateEndereco("cep", e.target.value)}
              />
              <Input
                label="Logradouro"
                value={end.logradouro || ""}
                onChange={(e) => updateEndereco("logradouro", e.target.value)}
              />
              <Input
                label="Número"
                value={end.numero || ""}
                onChange={(e) => updateEndereco("numero", e.target.value)}
              />
              <Input
                label="Bairro"
                value={end.bairro || ""}
                onChange={(e) => updateEndereco("bairro", e.target.value)}
              />
              <Input
                label="Complemento"
                value={end.complemento || ""}
                onChange={(e) => updateEndereco("complemento", e.target.value)}
              />
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Cidade"
                  value={end.cidade || ""}
                  onChange={(e) => updateEndereco("cidade", e.target.value)}
                />
                <Input
                  label="UF"
                  value={end.uf || ""}
                  onChange={(e) => updateEndereco("uf", e.target.value.toUpperCase().slice(0, 2))}
                />
              </div>
            </div>
          </Card>

          <Button
            onClick={handleSave}
            loading={saving}
            size="lg"
            className="w-full"
            icon={<Save className="h-5 w-5" />}
          >
            Salvar Alterações
          </Button>

          <Button
            variant="ghost"
            className="mt-4 w-full text-danger"
            icon={<LogOut className="h-5 w-5" />}
            onClick={logout}
          >
            Sair da conta
          </Button>
        </div>
      )}
    </div>
  );
}

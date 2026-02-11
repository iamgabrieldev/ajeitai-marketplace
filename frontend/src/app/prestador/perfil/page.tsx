"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Image from "next/image";
import { useAuth } from "@/providers/auth-provider";
import { prestadoresApi, type DadosCadastroPrestador, type DadosEndereco } from "@/lib/api";
import { PageHeader } from "@/components/layout/page-header";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Select } from "@/components/ui/select";
import { Card } from "@/components/ui/card";
import { useToast } from "@/components/ui/toast";
import { Camera, Save, LogOut } from "lucide-react";

const categoriaOptions = [
  { value: "eletricista", label: "Eletricista" },
  { value: "encanador", label: "Encanador" },
  { value: "pintor", label: "Pintor" },
  { value: "diarista", label: "Diarista" },
  { value: "jardineiro", label: "Jardineiro" },
  { value: "pedreiro", label: "Pedreiro" },
  { value: "marceneiro", label: "Marceneiro" },
  { value: "outros", label: "Outros" },
];

interface PrestadorProfileForm {
  nomeFantasia?: string;
  telefone?: string;
  cnpj?: string;
  categoria?: string; // valor em minúsculas para bater com as opções do select
  valorServico?: number;
  descricao?: string;
  cep?: string;
  logradouro?: string;
  numero?: string;
  bairro?: string;
  complemento?: string;
  cidade?: string;
  estado?: string;
  fotoUrl?: string;
}

export default function PerfilPrestadorPage() {
  const { user, token, logout } = useAuth();
  const { toast } = useToast();
  const fileRef = useRef<HTMLInputElement>(null);
  const [profile, setProfile] = useState<PrestadorProfileForm>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const fetchProfile = useCallback(async () => {
    if (!token) return;
    try {
      const data = (await prestadoresApi.getMe(token)) as any;
      const endereco = (data?.endereco ?? undefined) as DadosEndereco | undefined;

      setProfile({
        nomeFantasia: data?.nomeFantasia ?? "",
        telefone: data?.telefone ?? "",
        cnpj: data?.cnpj ?? "",
        // backend envia categoria em MAIÚSCULO (ex: ELETRICISTA), select usa minúsculo
        categoria: data?.categoria ? String(data.categoria).toLowerCase() : "",
        // backend usa valorServico, a UI chama de "valor por hora"
        valorServico: data?.valorServico ?? undefined,
        descricao: data?.descricao ?? "",
        cep: endereco?.cep ?? "",
        logradouro: endereco?.logradouro ?? "",
        numero: endereco?.numero ?? "",
        bairro: endereco?.bairro ?? "",
        complemento: endereco?.complemento ?? "",
        cidade: endereco?.cidade ?? "",
        estado: endereco?.uf ?? "",
        fotoUrl: data?.avatarUrl ?? data?.fotoUrl ?? "",
      });
    } catch (err) {
      console.error("Erro ao buscar perfil:", err);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  const handleSave = async () => {
    if (!token) return;
    setSaving(true);
    try {
      const endereco: DadosEndereco = {
        logradouro: profile.logradouro || "",
        bairro: profile.bairro || "",
        cep: profile.cep || "",
        cidade: profile.cidade || "",
        uf: profile.estado || "",
        numero: profile.numero || "",
        complemento: profile.complemento || "",
      };

      const payload: DadosCadastroPrestador = {
        nomeFantasia: profile.nomeFantasia || "",
        email: user?.email || "",
        telefone: profile.telefone,
        cnpj: profile.cnpj || "",
        // converter de volta para o enum esperado pelo backend
        categoria: ((profile.categoria || "outros").toUpperCase() as DadosCadastroPrestador["categoria"]),
        valorServico: profile.valorServico,
        endereco,
      };

      await prestadoresApi.updateMe(token, payload as unknown as Record<string, unknown>);
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
      await prestadoresApi.uploadFoto(token, file);
      fetchProfile();
    } catch (err) {
      console.error("Erro ao enviar foto:", err);
    }
  };

  const updateField = (field: keyof PrestadorProfileForm, value: string | number) => {
    setProfile((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <div className="flex flex-col">
      <PageHeader title="Meu Perfil" actions={<ThemeToggle />} />

      {loading ? (
        <div className="animate-pulse space-y-4 p-4">
          <div className="mx-auto h-24 w-24 rounded-full bg-secondary-200 dark:bg-secondary-700" />
          <div className="h-4 w-1/2 rounded bg-secondary-200 dark:bg-secondary-700" />
          <div className="h-10 rounded bg-secondary-200 dark:bg-secondary-700" />
        </div>
      ) : (
        <div className="px-4 py-4">
          {/* Avatar */}
          <div className="mb-6 flex flex-col items-center">
            <div className="relative">
              <div className="h-24 w-24 overflow-hidden rounded-full bg-secondary-100 dark:bg-secondary-800">
                {profile.fotoUrl ? (
                  <Image
                    src={profile.fotoUrl}
                    alt="Avatar"
                    width={96}
                    height={96}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="flex h-full w-full items-center justify-center text-3xl font-bold text-primary">
                    {profile.nomeFantasia?.charAt(0) ||
                      user?.firstName?.charAt(0) ||
                      "?"}
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
              {profile.nomeFantasia || user?.firstName}
            </h2>
            <p className="text-sm text-text-muted dark:text-text-dark-muted">
              {user?.email}
            </p>
          </div>

          <Card className="mb-4">
            <div className="flex flex-col gap-4 p-2">
              <Input
                label="Nome Fantasia"
                value={profile.nomeFantasia || ""}
                onChange={(e) => updateField("nomeFantasia", e.target.value)}
              />
              <Input
                label="Telefone"
                value={profile.telefone || ""}
                onChange={(e) => updateField("telefone", e.target.value)}
              />
              <Input
                label="CNPJ / CPF"
                value={profile.cnpj || ""}
                onChange={(e) => updateField("cnpj", e.target.value)}
              />
              <Select
                label="Categoria"
                options={categoriaOptions}
                value={profile.categoria || ""}
                onChange={(e) => updateField("categoria", e.target.value)}
              />
              <Input
                label="Valor por hora"
                type="number"
                value={profile.valorServico?.toString() || ""}
                onChange={(e) =>
                  updateField("valorServico", parseFloat(e.target.value) || 0)
                }
              />
              <Textarea
                label="Descrição"
                value={profile.descricao || ""}
                onChange={(e) => updateField("descricao", e.target.value)}
                rows={3}
              />
              <Input
                label="CEP"
                value={profile.cep || ""}
                onChange={(e) => updateField("cep", e.target.value)}
              />
              <Input
                label="Endereço"
                value={profile.logradouro || ""}
                onChange={(e) => updateField("logradouro", e.target.value)}
              />
              <Input
                label="Número"
                value={profile.numero || ""}
                onChange={(e) => updateField("numero", e.target.value)}
              />
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Cidade"
                  value={profile.cidade || ""}
                  onChange={(e) => updateField("cidade", e.target.value)}
                />
                <Input
                  label="Estado"
                  value={profile.estado || ""}
                  onChange={(e) => updateField("estado", e.target.value)}
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

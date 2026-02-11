"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/providers/auth-provider";
import {
  prestadoresApi,
  type DadosCadastroPrestador,
  type DadosEndereco,
  type CategoriaPrestador,
} from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Briefcase, ArrowRight } from "lucide-react";

function onlyDigits(s: string) {
  return s.replace(/\D/g, "");
}

const categoriaOptions: { value: CategoriaPrestador; label: string }[] = [
  { value: "ELETRICISTA", label: "Eletricista" },
  { value: "ENCANADOR", label: "Encanador" },
  { value: "PINTOR", label: "Pintor" },
  { value: "PEDREIRO", label: "Pedreiro" },
  { value: "MARCENEIRO", label: "Marceneiro" },
  { value: "AR_CONDICIONADO", label: "Ar condicionado" },
  { value: "JARDINAGEM", label: "Jardinagem" },
  { value: "LIMPEZA", label: "Limpeza" },
  { value: "DEDETIZACAO", label: "Dedetização" },
  { value: "SERRALHERIA", label: "Serralheria" },
  { value: "VIDRACEIRO", label: "Vidraceiro" },
  { value: "GESSO", label: "Gesso" },
  { value: "PISO", label: "Piso" },
  { value: "REFORMAS_GERAIS", label: "Reformas gerais" },
  { value: "OUTROS", label: "Outros" },
];

export default function PrestadorWelcomePage() {
  const { user, token } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState(0);
  const [form, setForm] = useState({
    nomeFantasia: "",
    telefone: "",
    cnpj: "",
    categoria: "" as CategoriaPrestador | "",
    valorServico: "",
    logradouro: "",
    bairro: "",
    cep: "",
    numero: "",
    complemento: "",
    cidade: "",
    uf: "",
  });

  const handleStart = () => setStep(1);

  const buildEndereco = (): DadosEndereco => ({
    logradouro: form.logradouro.trim(),
    bairro: form.bairro.trim(),
    cep: onlyDigits(form.cep).slice(0, 8),
    cidade: form.cidade.trim(),
    uf: form.uf.trim().toUpperCase().slice(0, 2),
    numero: form.numero.trim(),
    ...(form.complemento.trim() && { complemento: form.complemento.trim() }),
  });

  const handleVincular = async () => {
    if (!token || !form.categoria) return;
    const endereco = buildEndereco();
    const payload: DadosCadastroPrestador = {
      nomeFantasia: form.nomeFantasia.trim(),
      email: user?.email,
      telefone: form.telefone.trim() ? onlyDigits(form.telefone).slice(0, 11) : undefined,
      cnpj: onlyDigits(form.cnpj),
      categoria: form.categoria as CategoriaPrestador,
      valorServico: form.valorServico ? parseFloat(form.valorServico.replace(",", ".")) : undefined,
      endereco,
    };
    setLoading(true);
    try {
      await prestadoresApi.vincular(token, payload);
      router.replace("/prestador/dashboard");
    } catch (err) {
      console.error("Erro ao vincular prestador:", err);
    } finally {
      setLoading(false);
    }
  };

  const updateField = (field: string, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const isValid =
    form.nomeFantasia.trim() &&
    form.cnpj.replace(/\D/g, "").length >= 14 &&
    form.categoria &&
    form.logradouro.trim() &&
    form.bairro.trim() &&
    onlyDigits(form.cep).length === 8 &&
    form.numero.trim() &&
    form.cidade.trim() &&
    form.uf.trim().length === 2;

  if (step === 0) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center px-6">
        <div className="mb-8 flex flex-col items-center gap-4 text-center">
          <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-secondary/10">
            <Briefcase className="h-10 w-10 text-secondary dark:text-primary" />
          </div>
          <h1 className="text-2xl font-bold text-text dark:text-text-dark">
            Seja um Prestador Ajeita<span className="text-primary">i</span>
          </h1>
          <p className="max-w-xs text-text-muted dark:text-text-dark-muted">
            Olá, {user?.firstName}! Cadastre-se como prestador e comece a
            receber clientes na sua região.
          </p>
        </div>
        <Button onClick={handleStart} size="lg" className="w-full max-w-xs">
          Começar cadastro
          <ArrowRight className="h-5 w-5" />
        </Button>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col px-6 py-8">
      <h1 className="mb-2 text-xl font-bold text-text dark:text-text-dark">
        Cadastro de Prestador
      </h1>
      <p className="mb-6 text-sm text-text-muted dark:text-text-dark-muted">
        Preencha os dados para ativar seu perfil profissional.
      </p>

      <div className="flex flex-col gap-4">
        <Input
          label="Nome Fantasia"
          placeholder="Nome do seu negócio"
          value={form.nomeFantasia}
          onChange={(e) => updateField("nomeFantasia", e.target.value)}
        />
        <Input
          label="Telefone"
          placeholder="(00) 00000-0000"
          value={form.telefone}
          onChange={(e) => updateField("telefone", e.target.value)}
        />
        <Input
          label="CNPJ"
          placeholder="00.000.000/0000-00"
          value={form.cnpj}
          onChange={(e) => updateField("cnpj", e.target.value)}
        />
        <Select
          label="Categoria"
          options={categoriaOptions}
          placeholder="Selecione sua categoria"
          value={form.categoria}
          onChange={(e) => updateField("categoria", e.target.value as CategoriaPrestador | "")}
        />
        <Input
          label="Valor do serviço (opcional)"
          placeholder="R$ 0,00"
          type="number"
          min={0}
          value={form.valorServico}
          onChange={(e) => updateField("valorServico", e.target.value)}
        />
        <Input
          label="CEP"
          placeholder="00000-000"
          value={form.cep}
          onChange={(e) => updateField("cep", e.target.value)}
        />
        <Input
          label="Logradouro"
          placeholder="Rua, avenida..."
          value={form.logradouro}
          onChange={(e) => updateField("logradouro", e.target.value)}
        />
        <Input
          label="Número"
          placeholder="Nº"
          value={form.numero}
          onChange={(e) => updateField("numero", e.target.value)}
        />
        <Input
          label="Bairro"
          placeholder="Bairro"
          value={form.bairro}
          onChange={(e) => updateField("bairro", e.target.value)}
        />
        <Input
          label="Complemento"
          placeholder="Sala, loja..."
          value={form.complemento}
          onChange={(e) => updateField("complemento", e.target.value)}
        />
        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Cidade"
            value={form.cidade}
            onChange={(e) => updateField("cidade", e.target.value)}
          />
          <Input
            label="Estado"
            placeholder="UF"
            value={form.uf}
            onChange={(e) => updateField("uf", e.target.value.toUpperCase().slice(0, 2))}
          />
        </div>
      </div>

      <div className="mt-8">
        <Button
          onClick={handleVincular}
          loading={loading}
          disabled={!isValid}
          size="lg"
          className="w-full"
        >
          Ativar meu perfil
        </Button>
      </div>
    </div>
  );
}

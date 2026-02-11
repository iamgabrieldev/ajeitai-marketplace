import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** Formata endereço vindo da API (string ou objeto) para exibição. */
export function formatEndereco(
  endereco: string | { logradouro?: string; numero?: string; bairro?: string; cidade?: string; uf?: string; cep?: string } | null | undefined
): string {
  if (!endereco) return "";
  if (typeof endereco === "string") return endereco;
  const parts = [
    [endereco.logradouro, endereco.numero].filter(Boolean).join(", "),
    endereco.bairro,
    [endereco.cidade, endereco.uf].filter(Boolean).join(" - "),
    endereco.cep,
  ].filter(Boolean);
  return parts.join(", ");
}

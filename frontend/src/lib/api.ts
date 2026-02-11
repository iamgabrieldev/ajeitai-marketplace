const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:5000/api";

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  token?: string;
}

class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public data?: unknown
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { body, token, headers: customHeaders, ...rest } = options;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(customHeaders as Record<string, string>),
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const config: RequestInit = {
    ...rest,
    headers,
  };

  if (body !== undefined) {
    config.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

  if (!response.ok) {
    const errorData = await response.json().catch(() => null) as { message?: string; mensagem?: string } | null;
    const message = errorData?.message ?? errorData?.mensagem ?? `Request failed with status ${response.status}`;
    throw new ApiError(response.status, message, errorData);
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

async function uploadFile(
  endpoint: string,
  file: File,
  token?: string,
  fieldName = "file"
): Promise<unknown> {
  const formData = new FormData();
  formData.append(fieldName, file);

  const headers: Record<string, string> = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: "POST",
    headers,
    body: formData,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      errorData?.message || "Upload failed",
      errorData
    );
  }

  return response.json();
}

// ─── Schemas da API (Swagger) ───

export interface DadosEndereco {
  logradouro: string;
  bairro: string;
  cep: string;
  cidade: string;
  uf: string;
  numero: string;
  complemento?: string;
  latitude?: number;
  longitude?: number;
}

export interface DadosCadastroCliente {
  nome: string;
  email: string;
  telefone: string;
  cpf: string;
  endereco: DadosEndereco;
}

export type CategoriaPrestador =
  | "ELETRICISTA"
  | "ENCANADOR"
  | "PINTOR"
  | "PEDREIRO"
  | "MARCENEIRO"
  | "AR_CONDICIONADO"
  | "JARDINAGEM"
  | "LIMPEZA"
  | "DEDETIZACAO"
  | "SERRALHERIA"
  | "VIDRACEIRO"
  | "GESSO"
  | "PISO"
  | "REFORMAS_GERAIS"
  | "OUTROS";

export interface DadosCadastroPrestador {
  nomeFantasia: string;
  email?: string;
  telefone?: string;
  cnpj: string;
  categoria: CategoriaPrestador;
  valorServico?: number;
  endereco: DadosEndereco;
}

// ─── Clientes ───

export const clientesApi = {
  vincular: (token: string, data: DadosCadastroCliente) =>
    request("/clientes/vincular", { method: "POST", token, body: data }),

  getMe: (token: string) =>
    request("/clientes/me", { method: "GET", token }),

  updateMe: (token: string, data: Record<string, unknown>) =>
    request("/clientes/me", { method: "PUT", token, body: data }),

  uploadFoto: (token: string, file: File) =>
    uploadFile("/clientes/me/avatar", file, token),
};

// ─── Prestadores (listagem paginada) ───

export interface PrestadorResumo {
  id: number;
  nomeFantasia: string;
  categoria: string;
  cidade?: string;
  uf?: string;
  valorServico?: number;
  mediaAvaliacao: number;
  totalAvaliacoes: number;
  distanciaKm?: number;
  totalServicos: number;
  avatarUrl?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface PrestadorDetalhe {
  id: number;
  nomeFantasia: string;
  categoria: string;
  cidade?: string;
  uf?: string;
  valorServico?: number;
  mediaAvaliacao?: number;
  totalAvaliacoes?: number;
  avatarUrl?: string;
  portfolio?: { id: number; titulo?: string; descricao?: string; imagemUrl?: string }[];
}

export const prestadoresApi = {
  vincular: (token: string, data: DadosCadastroPrestador) =>
    request("/prestadores/vincular", { method: "POST", token, body: data }),

  /**
   * Lista prestadores paginados (GET /api/prestadores).
   * Params: page, size, search, categoria, avaliacaoMin, orderBy, latitude, longitude.
   */
  listar: (token: string, params?: Record<string, string | number>) => {
    const searchParams = new URLSearchParams();
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== "" && v !== null) {
          searchParams.set(k, String(v));
        }
      });
    }
    const query = searchParams.toString() ? "?" + searchParams.toString() : "";
    return request<PaginatedResponse<PrestadorResumo>>(`/prestadores${query}`, {
      method: "GET",
      token,
    });
  },

  getById: (token: string, id: string) =>
    request<PrestadorDetalhe>(`/catalogo/prestadores/${id}`, { method: "GET", token }),

  getMe: (token: string) =>
    request("/prestadores/me", { method: "GET", token }),

  updateMe: (token: string, data: Record<string, unknown>) =>
    request("/prestadores/me", { method: "PUT", token, body: data }),

  uploadFoto: (token: string, file: File) =>
    uploadFile("/prestadores/me/avatar", file, token),

  /** Lista solicitações do prestador (GET /api/prestadores/me/solicitacoes). Params: status = PENDENTE | ACEITO | ... */
  solicitacoes: (token: string, params?: Record<string, string>) => {
    const query = params ? "?" + new URLSearchParams(params).toString() : "";
    return request<Agendamento[]>(`/prestadores/me/solicitacoes${query}`, { method: "GET", token });
  },
};

// ─── Agendamentos ───

export interface EnderecoAgendamento {
  logradouro?: string;
  numero?: string;
  bairro?: string;
  cidade?: string;
  uf?: string;
  cep?: string;
}

export interface Agendamento {
  id: string;
  clienteId?: string;
  prestadorId?: string;
  dataHora: string;
  duracao?: number;
  /** Backend: PENDENTE | ACEITO | CONFIRMADO | REALIZADO | CANCELADO | RECUSADO */
  status: string;
  observacao?: string;
  descricao?: string;
  /** Backend enum: DINHEIRO | ONLINE */
  formaPagamento?: "DINHEIRO" | "ONLINE";
  linkPagamento?: string;
  prestadorNome?: string;
  clienteNome?: string;
  /** Backend pode enviar string ou objeto com logradouro, numero, bairro, cidade, uf, cep */
  endereco?: string | EnderecoAgendamento;
  /** Backend retorna checkinEm / checkoutEm; aceitamos os dois nomes. */
  checkinAt?: string;
  checkoutAt?: string;
  checkinEm?: string;
  checkoutEm?: string;
  /** URL da foto do trabalho anexada no checkout (backend: fotoTrabalhoUrl). */
  fotoTrabalhoUrl?: string;
  avaliacaoId?: string;
  podeFazerAvaliacao?: boolean;
}

export const agendamentosApi = {
  criar: (token: string, data: Record<string, unknown>) =>
    request<Agendamento>("/agendamentos", { method: "POST", token, body: data }),

  listar: (token: string, params?: Record<string, string>) => {
    const query = params ? "?" + new URLSearchParams(params).toString() : "";
    return request<Agendamento[]>(`/agendamentos${query}`, { method: "GET", token });
  },

  getById: (token: string, id: string) =>
    request<Agendamento>(`/agendamentos/${id}`, { method: "GET", token }),

  aceitar: (token: string, id: string) =>
    request(`/agendamentos/${id}/aceitar`, { method: "PUT", token }),

  recusar: (token: string, id: string) =>
    request(`/agendamentos/${id}/recusar`, { method: "PUT", token }),

  cancelar: (token: string, id: string) =>
    request(`/agendamentos/${id}/cancelar`, { method: "PUT", token }),

  confirmarPagamento: (token: string, id: string) =>
    request(`/agendamentos/${id}/confirmar-pagamento`, { method: "PUT", token }),

  checkin: (token: string, id: string, lat: number, lng: number) =>
    request(`/agendamentos/${id}/checkin`, {
      method: "PUT",
      token,
      body: { latitude: lat, longitude: lng },
    }),

  checkout: (token: string, id: string, lat: number, lng: number) =>
    request(`/agendamentos/${id}/checkout`, {
      method: "PUT",
      token,
      body: { latitude: lat, longitude: lng },
    }),

  /** Checkout com foto do trabalho (obrigatória). */
  checkoutComFoto: async (token: string, id: string, lat: number, lng: number, foto: File): Promise<void> => {
    const formData = new FormData();
    formData.append("latitude", String(lat));
    formData.append("longitude", String(lng));
    formData.append("foto", foto);
    const response = await fetch(`${API_BASE_URL}/agendamentos/${id}/checkout-com-foto`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}` },
      body: formData,
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => null) as { message?: string } | null;
      throw new ApiError(response.status, errorData?.message ?? "Erro ao concluir com foto", errorData);
    }
  },
};

// ─── Avaliações ───

export interface Avaliacao {
  id: string;
  agendamentoId: string;
  clienteId: string;
  prestadorId: string;
  nota: number;
  comentario?: string;
  createdAt: string;
}

export const avaliacoesApi = {
  criar: (token: string, data: { agendamentoId: string; nota: number; comentario?: string }) =>
    request<Avaliacao>("/avaliacoes", { method: "POST", token, body: data }),

  listarPorPrestador: (token: string, prestadorId: string) =>
    request<Avaliacao[]>(`/avaliacoes/prestador/${prestadorId}`, {
      method: "GET",
      token,
    }),
};

// ─── Disponibilidade ───

export interface Disponibilidade {
  diaSemana: number; // 0=DOM ... 6=SAB
  horaInicio: string;
  horaFim: string;
}

export const disponibilidadeApi = {
  get: (token: string) =>
    request<Disponibilidade[]>("/prestadores/me/disponibilidade", {
      method: "GET",
      token,
    }),

  update: (token: string, data: Disponibilidade[]) =>
    request("/prestadores/me/disponibilidade", {
      method: "PUT",
      token,
      body: data,
    }),
};

// ─── Documentos ───

export interface Documento {
  id: string;
  nome: string;
  tipo: string;
  url: string;
  createdAt: string;
}

export const documentosApi = {
  listar: (token: string) =>
    request<Documento[]>("/prestadores/me/documentos", {
      method: "GET",
      token,
    }),

  upload: (token: string, file: File) =>
    uploadFile("/prestadores/me/documentos", file, token),

  excluir: (token: string, id: string) =>
    request(`/prestadores/me/documentos/${id}`, {
      method: "DELETE",
      token,
    }),

  download: (token: string, id: string) =>
    `${API_BASE_URL}/prestadores/me/documentos/${id}/download?token=${token}`,
};

// ─── Dashboard ───

export interface DashboardMetrics {
  trabalhosDoMes: number;
  ganhoBruto: number;
  lucroLiquido: number;
  avaliacaoMedia: number;
  totalAvaliacoes: number;
}

export const dashboardApi = {
  getMetrics: (token: string) =>
    request<DashboardMetrics>("/prestadores/me/dashboard", {
      method: "GET",
      token,
    }),
};

const CHAT_API_BASE =
  process.env.NEXT_PUBLIC_CHAT_API_URL || "http://localhost:8080/api/chat";

async function chatRequest<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { body, token, headers: customHeaders, ...rest } = options;
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(customHeaders as Record<string, string>),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const config: RequestInit = { ...rest, headers };
  if (body !== undefined) config.body = JSON.stringify(body);
  const response = await fetch(`${CHAT_API_BASE}${endpoint}`, config);
  if (!response.ok) {
    const err = await response.json().catch(() => ({})) as { erro?: string };
    throw new ApiError(response.status, err.erro || "Erro no chat", err);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

export interface ConversaChat {
  id: string;
  clienteId: string;
  prestadorId: string;
  agendamentoId?: string;
  criadaEm: string;
  atualizadaEm: string;
}

export interface MensagemChat {
  id: string;
  conversaId: string;
  remetenteId: string;
  texto: string;
  enviadaEm: string;
  lida: boolean;
}

export const chatApi = {
  listarConversas: (token: string) =>
    chatRequest<ConversaChat[]>("/conversas", { method: "GET", token }),

  criarOuBuscarConversa: (
    token: string,
    prestadorId: string,
    agendamentoId?: string
  ) =>
    chatRequest<ConversaChat>("/conversas", {
      method: "POST",
      token,
      body: { prestadorId, agendamentoId: agendamentoId || "" },
    }),

  listarMensagens: (token: string, conversaId: string) =>
    chatRequest<MensagemChat[]>(`/conversas/${conversaId}/mensagens`, {
      method: "GET",
      token,
    }),

  enviarMensagem: (token: string, conversaId: string, texto: string) =>
    chatRequest<MensagemChat>(`/conversas/${conversaId}/mensagens`, {
      method: "POST",
      token,
      body: { texto },
    }),
};

export { ApiError };

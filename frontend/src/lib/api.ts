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
    let message =
      errorData?.message ??
      errorData?.mensagem ??
      `Request failed with status ${response.status}`;

    if (response.status === 401) {
      message = "Sua sessão expirou ou você não está autenticado.";
    } else if (response.status === 402) {
      message = message || "Sua assinatura está inativa. Regularize para continuar atendendo.";
    } else if (response.status === 403) {
      message = "Você não tem permissão para acessar este recurso.";
    }

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
    request("/api/clientes/vincular", { method: "POST", token, body: data }),

  getMe: (token: string) =>
    request("/api/clientes/me", { method: "GET", token }),

  updateMe: (token: string, data: Record<string, unknown>) =>
    request("/clientes/me", { method: "PUT", token, body: data }),

  uploadFoto: (token: string, file: File) =>
    uploadFile("/api/clientes/me/avatar", file, token),
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
  /** Keycloak ID do prestador; usado pelo chat interno. */
  keycloakId?: string;
}

export const prestadoresApi = {
  vincular: (token: string, data: DadosCadastroPrestador) =>
    request("/api/prestadores/vincular", { method: "POST", token, body: data }),

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
    request("/api/prestadores/me", { method: "GET", token }),

  updateMe: (token: string, data: Record<string, unknown>) =>
    request("/prestadores/me", { method: "PUT", token, body: data }),

  uploadFoto: (token: string, file: File) =>
    uploadFile("/api/prestadores/me/avatar", file, token),

  /** Lista solicitações do prestador (GET /api/prestadores/me/solicitacoes). Params: status = PENDENTE | ACEITO | ... */
  solicitacoes: (token: string, params?: Record<string, string>) => {
    const query = params ? "?" + new URLSearchParams(params).toString() : "";
    return request<Agendamento[]>(`/api/prestadores/me/solicitacoes${query}`, { method: "GET", token });
  },

  /** Assinatura: iniciar/gerar link de pagamento (POST /api/prestadores/me/assinatura). */
  iniciarAssinatura: (token: string) =>
    request<AssinaturaResumo>("/api/prestadores/me/assinatura", { method: "POST", token }),

  /** Assinatura: status atual (GET /api/prestadores/me/assinatura). */
  statusAssinatura: (token: string) =>
    request<AssinaturaResumo>("/api/prestadores/me/assinatura", { method: "GET", token }),

  /** Wallet: saldo, último saque, próximo saque disponível (GET /api/prestadores/me/wallet). */
  getWallet: (token: string) =>
    request<WalletResumo>("/api/prestadores/me/wallet", { method: "GET", token }),

  /** Solicitar saque (POST /api/prestadores/me/saques). */
  solicitarSaque: (token: string) =>
    request<SaquePrestador>("/api/prestadores/me/saques", { method: "POST", token }),

  /** Listar saques (GET /api/prestadores/me/saques). */
  listarSaques: (token: string) =>
    request<SaquePrestador[]>("/api/prestadores/me/saques", { method: "GET", token }),
};

export interface AssinaturaResumo {
  status: string;
  dataInicio?: string;
  dataFim?: string;
  paymentUrl?: string | null;
}

export interface WalletResumo {
  saldoDisponivel: number;
  dataUltimoSaque: string | null;
  proximoSaqueDisponivelEm: string;
  podeSolicitarSaque: boolean;
}

export interface SaquePrestador {
  id: number;
  valorSolicitado: number;
  valorLiquido: number;
  status: string;
  solicitadoEm: string;
  concluidoEm?: string | null;
}

// ─── Agendamentos ───

export interface EnderecoAgendamento {
  logradouro?: string;
  numero?: string;
  bairro?: string;
  cidade?: string;
  uf?: string;
  cep?: string;
}

export interface PagamentoAgendamento {
  id: number;
  status: string;
  linkPagamento?: string | null;
  billingId?: string | null;
}

export interface Agendamento {
  id: string;
  clienteId?: string;
  prestadorId?: string;
  /** Keycloak ID do prestador; usado pelo chat interno para criar conversa. */
  prestadorKeycloakId?: string;
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
  /** Valor do serviço do agendamento (backend: valorServico). */
  valorServico?: number;
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
    request<Agendamento>("/api/agendamentos", { method: "POST", token, body: data }),

  listar: (token: string, params?: Record<string, string>) => {
    const query = params ? "?" + new URLSearchParams(params).toString() : "";
    return request<Agendamento[]>(`/api/agendamentos${query}`, { method: "GET", token });
  },

  getById: (token: string, id: string) =>
    request<Agendamento>(`/api/agendamentos/${id}`, { method: "GET", token }),

  aceitar: (token: string, id: string) =>
    request(`/api/agendamentos/${id}/aceitar`, { method: "PUT", token }),

  recusar: (token: string, id: string) =>
    request(`/api/agendamentos/${id}/recusar`, { method: "PUT", token }),

  cancelar: (token: string, id: string) =>
    request(`/api/agendamentos/${id}/cancelar`, { method: "PUT", token }),

  /** Retorna o pagamento do agendamento (inclui linkPagamento para PIX/cartão). */
  getPagamento: (token: string, id: string) =>
    request<PagamentoAgendamento>(`/api/agendamentos/${id}/pagamento`, { method: "GET", token }),

  confirmarPagamento: (token: string, id: string) =>
    request(`/api/agendamentos/${id}/confirmar-pagamento`, { method: "PUT", token }),

  checkin: (token: string, id: string, lat: number, lng: number) =>
    request(`/api/agendamentos/${id}/checkin`, {
      method: "PUT",
      token,
      body: { latitude: lat, longitude: lng },
    }),

  checkout: (token: string, id: string, lat: number, lng: number) =>
    request(`/api/agendamentos/${id}/checkout`, {
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
    request<Avaliacao>(`/api/clientes/me/avaliacoes/${data.agendamentoId}`, {
      method: "POST",
      token,
      body: { nota: data.nota, comentario: data.comentario },
    }),

  listarPorPrestador: (token: string, prestadorId: string) =>
    request<Avaliacao[]>(`/api/avaliacoes/prestador/${prestadorId}`, {
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
    request<Disponibilidade[]>("/api/prestadores/me/disponibilidade", {
      method: "GET",
      token,
    }),

  update: (token: string, data: Disponibilidade[]) =>
    request("/api/prestadores/me/disponibilidade", {
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
    request<Documento[]>("/api/prestadores/me/documentos", {
      method: "GET",
      token,
    }),

  upload: (token: string, file: File) =>
    uploadFile("/api/prestadores/me/documentos", file, token),

  excluir: (token: string, id: string) =>
    request(`/api/prestadores/me/documentos/${id}`, {
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
    request<DashboardMetrics>("/api/prestadores/me/dashboard", {
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
    chatRequest<ConversaChat[]>("/api/conversas", { method: "GET", token }),

  criarOuBuscarConversa: (
    token: string,
    prestadorId: string,
    agendamentoId?: string
  ) =>
    chatRequest<ConversaChat>("/api/conversas", {
      method: "POST",
      token,
      body: { prestadorId, agendamentoId: agendamentoId || "" },
    }),

  listarMensagens: (token: string, conversaId: string) =>
    chatRequest<MensagemChat[]>(`/api/conversas/${conversaId}/mensagens`, {
      method: "GET",
      token,
    }),

  enviarMensagem: (token: string, conversaId: string, texto: string) =>
    chatRequest<MensagemChat>(`/api/conversas/${conversaId}/mensagens`, {
      method: "POST",
      token,
      body: { texto },
    }),
};

// ─── Admin ───

export interface AdminVisaoGeral {
  totalClientes: number;
  totalPrestadores: number;
  prestadoresComAssinaturaAtiva: number;
  agendamentosPorStatus: Record<string, number>;
  gmv: number;
  receitaPlataforma: number;
  pagamentosPendentes: number;
  pagamentosConfirmados: number;
}

export interface AdminPrestador {
  id: number;
  nomeFantasia: string;
  email?: string;
  statusAssinatura: string | null;
  dataFimAssinatura: string | null;
  saldoDisponivel: number;
}

export interface AdminPagamento {
  id: number;
  agendamentoId: number | null;
  status: string;
  billingId: string | null;
  criadoEm: string;
  confirmadoEm: string | null;
}

export interface AdminSaque {
  id: number;
  prestadorId: number | null;
  prestadorNome: string | null;
  valorSolicitado: number;
  valorLiquido: number;
  status: string;
  solicitadoEm: string;
  concluidoEm: string | null;
}

export const adminApi = {
  visaoGeral: (token: string) =>
    request<AdminVisaoGeral>("/api/admin/visao-geral", { method: "GET", token }),

  prestadores: (token: string) =>
    request<AdminPrestador[]>("/api/admin/prestadores", { method: "GET", token }),

  agendamentos: (token: string, status?: string) => {
    const query = status ? `?status=${encodeURIComponent(status)}` : "";
    return request<Agendamento[]>(`/api/admin/agendamentos${query}`, { method: "GET", token });
  },

  pagamentos: (token: string) =>
    request<AdminPagamento[]>("/api/admin/pagamentos", { method: "GET", token }),

  saques: (token: string) =>
    request<AdminSaque[]>("/api/admin/saques", { method: "GET", token }),
};

export { ApiError };

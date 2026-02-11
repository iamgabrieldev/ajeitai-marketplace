"use client";

import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { useAuth } from "@/providers/auth-provider";
import { prestadoresApi, clientesApi, ApiError, type PrestadorResumo } from "@/lib/api";
import { ThemeToggle } from "@/components/theme-toggle";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { StarRating } from "@/components/ui/star-rating";
import { BottomSheet } from "@/components/ui/bottom-sheet";
import {
  Search,
  MapPin,
  SlidersHorizontal,
  Sparkles,
  Star,
  Navigation,
  TrendingDown,
  Award,
  Clock,
  Briefcase,
  CheckCircle,
  RotateCcw,
  AlertTriangle,
  User,
} from "lucide-react";

interface Prestador {
  id: string;
  nomeFantasia: string;
  categoria: string;
  valorHora?: number;
  avaliacao: number;
  totalAvaliacoes: number;
  fotoUrl?: string;
  cidade?: string;
  distancia?: number;
  totalServicos?: number;
}

const PAGE_SIZE = 10;

const categorias = [
  { id: "todos", label: "Todos", emoji: "🏠" },
  { id: "ELETRICISTA", label: "Eletricista", emoji: "⚡" },
  { id: "ENCANADOR", label: "Encanador", emoji: "🔧" },
  { id: "PINTOR", label: "Pintor", emoji: "🎨" },
  { id: "LIMPEZA", label: "Limpeza", emoji: "🧹" },
  { id: "JARDINAGEM", label: "Jardinagem", emoji: "🌿" },
  { id: "PEDREIRO", label: "Pedreiro", emoji: "🧱" },
  { id: "MARCENEIRO", label: "Marceneiro", emoji: "🪚" },
  { id: "OUTROS", label: "Outros", emoji: "🔧" },
];

interface FilterOption {
  id: string;
  label: string;
  icon: React.ReactNode;
  description: string;
}

const filterOptions: FilterOption[] = [
  {
    id: "melhor-avaliacao",
    label: "Melhor avaliação",
    icon: <Star className="h-5 w-5" />,
    description: "Prestadores com nota 4.5+",
  },
  {
    id: "mais-proximo",
    label: "Mais próximo",
    icon: <Navigation className="h-5 w-5" />,
    description: "Ordenar por distância",
  },
  {
    id: "menor-preco",
    label: "Menor preço",
    icon: <TrendingDown className="h-5 w-5" />,
    description: "Valor/hora mais acessível",
  },
  {
    id: "mais-experiente",
    label: "Mais experiente",
    icon: <Award className="h-5 w-5" />,
    description: "Mais serviços concluídos",
  },
  {
    id: "disponivel-agora",
    label: "Disponível agora",
    icon: <Clock className="h-5 w-5" />,
    description: "Pode atender hoje",
  },
  {
    id: "mais-contratados",
    label: "Mais contratados",
    icon: <Briefcase className="h-5 w-5" />,
    description: "Populares na sua região",
  },
  {
    id: "verificados",
    label: "Verificados",
    icon: <CheckCircle className="h-5 w-5" />,
    description: "Documentação aprovada",
  },
];

const avaliacaoMinOptions = [
  { value: "0", label: "Todas" },
  { value: "3", label: "3+" },
  { value: "3.5", label: "3.5+" },
  { value: "4", label: "4+" },
  { value: "4.5", label: "4.5+" },
];

function resumoToPrestador(r: PrestadorResumo): Prestador {
  return {
    id: String(r.id),
    nomeFantasia: r.nomeFantasia,
    categoria: r.categoria,
    valorHora: r.valorServico,
    avaliacao: r.mediaAvaliacao,
    totalAvaliacoes: r.totalAvaliacoes ?? 0,
    fotoUrl: r.avatarUrl,
    cidade: r.cidade,
    distancia: r.distanciaKm,
    totalServicos: r.totalServicos,
  };
}

function orderByFromFiltros(filtrosAtivos: string[]): string | undefined {
  if (filtrosAtivos.includes("melhor-avaliacao")) return "avaliacao";
  if (filtrosAtivos.includes("mais-proximo")) return "distancia";
  if (filtrosAtivos.includes("menor-preco")) return "valorHora";
  if (filtrosAtivos.includes("mais-experiente")) return "experiencia";
  if (filtrosAtivos.includes("mais-contratados")) return "popularidade";
  return undefined;
}

interface ClienteMe {
  telefone?: string;
  endereco?: { logradouro?: string; numero?: string; cidade?: string; uf?: string };
}

export default function HomePage() {
  return (
    <Suspense>
      <HomePageContent />
    </Suspense>
  );
}

function HomePageContent() {
  const searchParams = useSearchParams();
  const { user, token } = useAuth();
  const [prestadores, setPrestadores] = useState<Prestador[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [search, setSearch] = useState(() => searchParams.get("search") ?? "");
  const [categoriaAtiva, setCategoriaAtiva] = useState(() => searchParams.get("categoria") ?? "todos");
  const [perfilIncompleto, setPerfilIncompleto] = useState(false);

  // Filtros (sincronizados com URL)
  const [showFilters, setShowFilters] = useState(false);
  const [filtrosAtivos, setFiltrosAtivos] = useState<string[]>(() => {
    const o = searchParams.get("orderBy");
    if (!o) return [];
    const map: Record<string, string> = { avaliacao: "melhor-avaliacao", distancia: "mais-proximo", valorHora: "menor-preco", experiencia: "mais-experiente", popularidade: "mais-contratados" };
    return map[o] ? [map[o]] : [];
  });
  const [avaliacaoMin, setAvaliacaoMin] = useState(() => searchParams.get("avaliacaoMin") ?? "0");
  const sentinelRef = useRef<HTMLDivElement>(null);

  const totalFiltros =
    filtrosAtivos.length + (avaliacaoMin !== "0" ? 1 : 0);

  const updateUrl = useCallback((updates: Record<string, string>) => {
    const params = new URLSearchParams(searchParams.toString());
    Object.entries(updates).forEach(([k, v]) => {
      if (v === "" || v === "todos" || v === "0") params.delete(k);
      else params.set(k, v);
    });
    window.history.replaceState(null, "", `?${params.toString()}`);
  }, [searchParams]);

  const toggleFiltro = (id: string) => {
    setFiltrosAtivos((prev) => {
      const next = prev.includes(id) ? prev.filter((f) => f !== id) : [...prev, id];
      const orderBy = orderByFromFiltros(next);
      updateUrl({ orderBy: orderBy ?? "" });
      return next;
    });
  };

  const limparFiltros = () => {
    setFiltrosAtivos([]);
    setAvaliacaoMin("0");
    updateUrl({ orderBy: "", avaliacaoMin: "0" });
  };

  const aplicarFiltros = () => {
    setShowFilters(false);
    setPage(0);
    setHasMore(true);
    fetchPrestadores(0, false);
  };

  const fetchPrestadores = useCallback(async (pageNum: number, append: boolean) => {
    if (!token) return;
    if (append) setLoadingMore(true);
    else setLoading(true);
    try {
      const params: Record<string, string | number> = {
        page: pageNum,
        size: PAGE_SIZE,
      };
      if (search) params.search = search;
      if (categoriaAtiva !== "todos") params.categoria = categoriaAtiva;
      if (avaliacaoMin !== "0") params.avaliacaoMin = parseFloat(avaliacaoMin);
      const orderBy = orderByFromFiltros(filtrosAtivos);
      if (orderBy) params.orderBy = orderBy;

      const data = await prestadoresApi.listar(token, params);
      const list = (data.content ?? []).map(resumoToPrestador);
      if (append) {
        setPrestadores((prev) => [...prev, ...list]);
      } else {
        setPrestadores(list);
      }
      setHasMore(!data.last);
      setPage(pageNum);
    } catch {
      if (!append) setPrestadores([]);
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [token, search, categoriaAtiva, avaliacaoMin, filtrosAtivos]);

  const loadMore = useCallback(() => {
    if (loading || loadingMore || !hasMore) return;
    fetchPrestadores(page + 1, true);
  }, [fetchPrestadores, page, hasMore, loading, loadingMore]);

  const checkPerfil = useCallback(async () => {
    if (!token) return;
    try {
      const data = (await clientesApi.getMe(token)) as ClienteMe;
      const semTelefone = !data?.telefone?.trim();
      const semEndereco = !data?.endereco?.logradouro?.trim() || !data?.endereco?.numero?.trim();
      setPerfilIncompleto(semTelefone || semEndereco);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) setPerfilIncompleto(true);
      else setPerfilIncompleto(true);
    }
  }, [token]);

  useEffect(() => {
    checkPerfil();
  }, [checkPerfil]);

  useEffect(() => {
    fetchPrestadores(0, false);
  }, [fetchPrestadores]);

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;
    const obs = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) loadMore();
      },
      { rootMargin: "100px", threshold: 0.1 }
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, [loadMore]);

  return (
    <div className="flex flex-col">
      {/* Aviso perfil incompleto */}
      {perfilIncompleto && (
        <div className="bg-amber-500/15 px-4 py-3 dark:bg-amber-600/20">
          <div className="flex items-center gap-3">
            <AlertTriangle className="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-amber-800 dark:text-amber-200">
                Complete seu perfil
              </p>
              <p className="text-xs text-amber-700 dark:text-amber-300">
                Preencha telefone e endereço em Meu Perfil para usar todos os recursos.
              </p>
            </div>
            <Link
              href="/cliente/perfil"
              className="flex shrink-0 items-center gap-1 rounded-lg bg-amber-600 px-3 py-2 text-sm font-medium text-white hover:bg-amber-700 dark:bg-amber-500 dark:hover:bg-amber-600"
            >
              <User className="h-4 w-4" />
              Meu perfil
            </Link>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="bg-white px-4 pb-4 pt-6 dark:bg-surface-dark">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <p className="text-sm text-text-muted dark:text-text-dark-muted">
              Olá, {user?.firstName || "visitante"} 👋
            </p>
            <h1 className="text-xl font-bold text-text dark:text-text-dark">
              O que precisa hoje?
            </h1>
          </div>
          <ThemeToggle />
        </div>

        {/* Search */}
        <div className="relative">
          <Input
            placeholder="Buscar serviços ou prestadores..."
            icon={<Search className="h-5 w-5" />}
            value={search}
            onChange={(e) => {
              const v = e.target.value;
              setSearch(v);
              updateUrl({ search: v });
              setPage(0);
              setHasMore(true);
            }}
          />
          <button
            onClick={() => setShowFilters(true)}
            className="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg bg-primary p-1.5 text-white transition-transform active:scale-95"
          >
            <SlidersHorizontal className="h-4 w-4" />
            {totalFiltros > 0 && (
              <span className="absolute -right-1.5 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-danger text-[10px] font-bold text-white">
                {totalFiltros}
              </span>
            )}
          </button>
        </div>
      </div>

      {/* Filtros ativos */}
      {totalFiltros > 0 && (
        <div className="flex items-center gap-2 overflow-x-auto px-4 py-2">
          {filtrosAtivos.map((id) => {
            const opt = filterOptions.find((f) => f.id === id);
            if (!opt) return null;
            return (
              <button
                key={id}
                onClick={() => toggleFiltro(id)}
                className="flex shrink-0 items-center gap-1 rounded-full bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary dark:bg-primary-900/30 dark:text-primary-400"
              >
                {opt.label}
                <span className="ml-0.5">×</span>
              </button>
            );
          })}
          {avaliacaoMin !== "0" && (
            <span className="flex shrink-0 items-center gap-1 rounded-full bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary dark:bg-primary-900/30 dark:text-primary-400">
              ★ {avaliacaoMin}+
            </span>
          )}
          <button
            onClick={limparFiltros}
            className="shrink-0 text-xs font-medium text-text-muted underline dark:text-text-dark-muted"
          >
            Limpar
          </button>
        </div>
      )}

      {/* Categorias */}
      <div className="overflow-x-auto px-4 py-3">
        <div className="flex gap-2">
          {categorias.map((cat) => (
            <button
              key={cat.id}
              onClick={() => {
                setCategoriaAtiva(cat.id);
                updateUrl({ categoria: cat.id === "todos" ? "" : cat.id });
                setPage(0);
                setHasMore(true);
              }}
              className={`flex shrink-0 items-center gap-1.5 rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                categoriaAtiva === cat.id
                  ? "bg-primary text-white shadow-sm"
                  : "bg-white text-text-muted shadow-sm dark:bg-surface-dark-alt dark:text-text-dark-muted"
              }`}
            >
              <span>{cat.emoji}</span>
              {cat.label}
            </button>
          ))}
        </div>
      </div>

      {/* Destaque */}
      <div className="px-4 py-2">
        <Card className="bg-gradient-to-r from-primary to-primary-600 text-white">
          <div className="flex items-center gap-3">
            <Sparkles className="h-8 w-8 shrink-0" />
            <div>
              <p className="font-semibold">Primeira contratação?</p>
              <p className="text-sm text-white/80">
                Ganhe 10% de desconto no seu primeiro serviço!
              </p>
            </div>
          </div>
        </Card>
      </div>

      {/* Lista de prestadores */}
      <div className="px-4 pb-6 pt-2">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-base font-semibold text-text dark:text-text-dark">
            Prestadores próximos
          </h2>
          <span className="text-sm text-text-muted dark:text-text-dark-muted">
            {prestadores.length} encontrados
          </span>
        </div>

        {loading ? (
          <div className="flex flex-col gap-3">
            {[1, 2, 3].map((i) => (
              <Card key={i} className="animate-pulse">
                <div className="flex gap-3">
                  <div className="h-20 w-20 rounded-xl bg-secondary-200 dark:bg-secondary-700" />
                  <div className="flex-1 space-y-2 py-1">
                    <div className="h-4 w-3/4 rounded bg-secondary-200 dark:bg-secondary-700" />
                    <div className="h-3 w-1/2 rounded bg-secondary-200 dark:bg-secondary-700" />
                    <div className="h-3 w-1/3 rounded bg-secondary-200 dark:bg-secondary-700" />
                  </div>
                </div>
              </Card>
            ))}
          </div>
        ) : prestadores.length === 0 ? (
          <Card className="py-8 text-center">
            <Search className="mx-auto mb-3 h-10 w-10 text-secondary-300 dark:text-secondary-600" />
            <p className="font-medium text-text dark:text-text-dark">
              Nenhum prestador encontrado
            </p>
            <p className="mt-1 text-sm text-text-muted dark:text-text-dark-muted">
              Tente ajustar os filtros ou buscar outra categoria.
            </p>
          </Card>
        ) : (
          <div className="flex flex-col gap-3">
            {prestadores.map((p) => (
              <Link key={p.id} href={`/cliente/prestadores/${String(p.id)}`}>
                <Card hover>
                  <div className="flex gap-3">
                    <div className="relative h-20 w-20 shrink-0 overflow-hidden rounded-xl bg-secondary-100 dark:bg-secondary-800">
                      {p.fotoUrl ? (
                        <Image
                          src={p.fotoUrl}
                          alt={p.nomeFantasia}
                          fill
                          className="object-cover"
                        />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center text-2xl font-bold text-primary">
                          {p.nomeFantasia.charAt(0)}
                        </div>
                      )}
                    </div>
                    <div className="flex flex-1 flex-col justify-between py-0.5">
                      <div>
                        <h3 className="font-semibold text-text dark:text-text-dark">
                          {p.nomeFantasia}
                        </h3>
                        <div className="mt-1 flex items-center gap-2">
                          <Badge variant="primary">{p.categoria}</Badge>
                          {p.totalServicos && p.totalServicos > 10 && (
                            <Badge variant="success">Top</Badge>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-1">
                          <StarRating rating={p.avaliacao} size="sm" />
                          <span className="text-xs text-text-muted dark:text-text-dark-muted">
                            ({p.totalAvaliacoes})
                          </span>
                        </div>
                        {p.distancia ? (
                          <span className="flex items-center gap-0.5 text-xs text-text-muted dark:text-text-dark-muted">
                            <MapPin className="h-3 w-3" />
                            {p.distancia < 1
                              ? `${(p.distancia * 1000).toFixed(0)}m`
                              : `${p.distancia.toFixed(1)}km`}
                          </span>
                        ) : p.cidade ? (
                          <span className="flex items-center gap-0.5 text-xs text-text-muted dark:text-text-dark-muted">
                            <MapPin className="h-3 w-3" />
                            {p.cidade}
                          </span>
                        ) : null}
                      </div>
                    </div>
                  </div>
                  {p.valorHora && (
                    <div className="mt-2 text-right text-sm font-semibold text-primary">
                      R$ {p.valorHora.toFixed(2)}/h
                    </div>
                  )}
                </Card>
              </Link>
            ))}
          </div>
        )}

        {/* Sentinel para scroll infinito */}
        {hasMore && (prestadores.length > 0 || loading) && (
          <div ref={sentinelRef} className="flex justify-center py-6">
            {loadingMore && (
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            )}
          </div>
        )}
      </div>

      {/* ───── Bottom Sheet de Filtros ───── */}
      <BottomSheet
        open={showFilters}
        onClose={() => setShowFilters(false)}
        title="Filtros"
      >
        {/* Avaliação mínima */}
        <div className="mb-5">
          <h4 className="mb-2 text-sm font-semibold text-text dark:text-text-dark">
            Avaliação mínima
          </h4>
          <div className="flex gap-2">
            {avaliacaoMinOptions.map((opt) => (
              <button
                key={opt.value}
                onClick={() => {
                  setAvaliacaoMin(opt.value);
                  updateUrl({ avaliacaoMin: opt.value });
                }}
                className={`flex items-center gap-1 rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                  avaliacaoMin === opt.value
                    ? "bg-primary text-white"
                    : "bg-secondary-100 text-text-muted dark:bg-secondary-800 dark:text-text-dark-muted"
                }`}
              >
                {opt.value !== "0" && <Star className="h-3.5 w-3.5" />}
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Opções de filtro */}
        <div className="mb-5">
          <h4 className="mb-2 text-sm font-semibold text-text dark:text-text-dark">
            Ordenar e filtrar
          </h4>
          <div className="flex flex-col gap-2">
            {filterOptions.map((opt) => {
              const active = filtrosAtivos.includes(opt.id);
              return (
                <button
                  key={opt.id}
                  onClick={() => toggleFiltro(opt.id)}
                  className={`flex items-center gap-3 rounded-xl border-2 px-4 py-3 text-left transition-colors ${
                    active
                      ? "border-primary bg-primary/5 dark:bg-primary-900/15"
                      : "border-transparent bg-secondary-50 dark:bg-secondary-900/30"
                  }`}
                >
                  <div
                    className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
                      active
                        ? "bg-primary text-white"
                        : "bg-secondary-100 text-text-muted dark:bg-secondary-800 dark:text-text-dark-muted"
                    }`}
                  >
                    {opt.icon}
                  </div>
                  <div className="flex-1">
                    <p
                      className={`text-sm font-medium ${
                        active
                          ? "text-primary"
                          : "text-text dark:text-text-dark"
                      }`}
                    >
                      {opt.label}
                    </p>
                    <p className="text-xs text-text-muted dark:text-text-dark-muted">
                      {opt.description}
                    </p>
                  </div>
                  {active && (
                    <CheckCircle className="h-5 w-5 shrink-0 text-primary" />
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* Ações */}
        <div className="flex gap-3 pt-2">
          <Button
            variant="outline"
            onClick={limparFiltros}
            icon={<RotateCcw className="h-4 w-4" />}
            className="flex-1"
          >
            Limpar
          </Button>
          <Button onClick={aplicarFiltros} className="flex-1">
            Aplicar ({totalFiltros})
          </Button>
        </div>
      </BottomSheet>
    </div>
  );
}

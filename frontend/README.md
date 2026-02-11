# Ajeitai Frontend

PWA mobile-first para o marketplace de serviços domésticos **Ajeitai**. Desenvolvido com Next.js (App Router), TypeScript e Tailwind CSS.

## Tecnologias

- **Next.js 16** — App Router, SSR e otimizações de performance
- **React 19** — UI reativa
- **TypeScript** — Tipagem estática
- **Tailwind CSS v4** — Utilitário CSS com design tokens
- **Keycloak** — Autenticação via PKCE (sem telas de login próprias)
- **PWA** — Manifest, Service Worker, notificações push
- **Lucide React** — Ícones minimalistas

## Pré-requisitos

- Node.js 20+ (LTS)
- npm 10+
- Keycloak rodando (realm `ajeitai` configurado)
- Backend API rodando

## Configuração

1. Clone o repositório e entre na pasta `frontend`:

```bash
cd frontend
```

2. Copie o arquivo de variáveis de ambiente:

```bash
cp .env.example .env.local
```

3. Preencha as variáveis:

| Variável | Descrição |
|---|---|
| `NEXT_PUBLIC_KEYCLOAK_URL` | URL do servidor Keycloak |
| `NEXT_PUBLIC_KEYCLOAK_REALM` | Realm do Keycloak |
| `NEXT_PUBLIC_KEYCLOAK_CLIENT_ID` | Client ID registrado no Keycloak |
| `NEXT_PUBLIC_API_URL` | URL base da API backend |
| `NEXT_PUBLIC_VAPID_PUBLIC_KEY` | Chave pública VAPID para push notifications |

4. Instale as dependências e inicie:

```bash
npm install
npm run dev
```

O app estará disponível em `http://localhost:3000`.

## Estrutura do projeto

```
src/
├── app/
│   ├── cliente/            # Páginas do cliente (/cliente/...)
│   │   ├── welcome/        # Boas-vindas e cadastro
│   │   ├── home/           # Catálogo de prestadores
│   │   ├── prestadores/    # Perfil do prestador + agendamento
│   │   ├── agendamentos/   # Lista de agendamentos
│   │   └── perfil/         # Perfil do cliente
│   ├── prestador/          # Páginas do prestador (/prestador/...)
│   │   ├── welcome/        # Cadastro de prestador
│   │   ├── dashboard/      # Painel com métricas
│   │   ├── solicitacoes/   # Solicitações de agendamento
│   │   ├── disponibilidade/# Horários semanais
│   │   ├── documentos/     # Upload de documentos
│   │   └── perfil/         # Perfil do prestador
│   ├── layout.tsx          # Layout raiz (providers)
│   └── page.tsx            # Redirect por role
├── components/
│   ├── ui/                 # Button, Card, Input, Badge, StarRating, etc.
│   ├── layout/             # BottomNav, PageHeader, LoadingScreen
│   ├── theme-toggle.tsx    # Toggle dark/light
│   └── push-initializer.tsx# Inicializador de push
├── lib/
│   ├── keycloak.ts         # Cliente Keycloak e helpers de roles
│   ├── api.ts              # Wrapper fetch com tipagem para todos endpoints
│   ├── push.ts             # Service Worker e push notifications
│   └── utils.ts            # cn() utility
└── providers/
    ├── auth-provider.tsx   # Contexto de autenticação Keycloak
    └── theme-provider.tsx  # Contexto de tema dark/light
```

## Scripts

| Comando | Descrição |
|---|---|
| `npm run dev` | Inicia em modo desenvolvimento |
| `npm run build` | Build de produção |
| `npm run start` | Serve o build de produção |
| `npm run lint` | Executa ESLint |

## Fluxo de autenticação

1. Ao abrir o app, o `AuthProvider` inicializa o Keycloak com `login-required` e PKCE
2. Após autenticação, `realm_access.roles` é lido do token
3. Se tem role `prestador` → redireciona para `/prestador/dashboard`
4. Se tem role `cliente` → redireciona para `/cliente/home`
5. O token é usado em todas as chamadas à API via header `Authorization: Bearer`

## Design System

- **Primária:** Laranja (#e65c00) — CTA, botões, destaque
- **Secundária:** Azul marinho (#1a2b4a) — textos, fundos
- **Dark mode:** Laranja em botões/ícones; fundo escuro
- **Light mode:** Azul marinho em textos; fundo claro
- **Mobile-first:** Layout inspirado em iFood/Uber/99 com bottom navigation

## PWA

O app é instalável como PWA com:
- Manifest configurado (`public/manifest.json`)
- Service Worker via `@ducanh2912/next-pwa`
- Push notifications com VAPID
- Cache de fontes e assets estáticos

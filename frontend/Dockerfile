FROM node:20-alpine AS base

# Stage 1: Deps
FROM base AS deps
RUN apk add --no-cache libc6-compat
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# Stage 2: Builder
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED 1

ENV NEXT_PUBLIC_API_URL=https://api.iamgabrieldev.com.br
ENV NEXT_PUBLIC_KEYCLOAK_URL=https://auth.iamgabrieldev.com.br
ENV NEXT_PUBLIC_KEYCLOAK_REALM=ajeitai
ENV NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=ajeitai-web

RUN echo "NEXT_PUBLIC_API_URL=https://api.iamgabrieldev.com.br" > .env.production && \
    echo "NEXT_PUBLIC_KEYCLOAK_URL=https://auth.iamgabrieldev.com.br" >> .env.production && \
    echo "NEXT_PUBLIC_KEYCLOAK_REALM=ajeitai" >> .env.production && \
    echo "NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=ajeitai-web" >> .env.production && \
    echo "NEXT_PUBLIC_CHAT_API_URL=https://api.iamgabrieldev.com.br/api/chat" >> .env.production

RUN npm run build

# Stage 3: Runner
FROM base AS runner
WORKDIR /app
ENV NODE_ENV production
ENV NEXT_TELEMETRY_DISABLED 1
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
# Copia a pasta standalone gerada pelo Next
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
ENV PORT 3000
CMD ["node", "server.js"]

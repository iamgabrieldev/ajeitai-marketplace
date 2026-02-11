# Configuração completa AbacatePay (dashboard, API key, webhook, fluxo)

Este guia explica como sair do **dev mode**, onde colocar a chave de API, como configurar o webhook e como funcionam **Produto**, **Cliente** e **Cupom** na integração.

---

## 1. Onde colocar a chave de API

A chave de API é obrigatória para gerar links de pagamento reais. Você pode configurá-la de duas formas:

### Opção A: Arquivo `application.properties` (desenvolvimento)

No arquivo **`src/main/resources/application.properties`** (ou `application-dev.properties`):

```properties
# AbacatePay – substitua pela sua chave (dev ou produção)
app.abacatepay.api-key=SUA_CHAVE_AQUI
app.abacatepay.webhook-secret=SEU_WEBHOOK_SECRET
app.abacatepay.frontend-base-url=http://localhost:3000
```

- **Dev mode**: use a chave gerada no dashboard com **Dev mode** ativado.
- **Produção**: use a chave gerada após ativar produção (verificação da conta).

### Opção B: Variáveis de ambiente (recomendado em produção)

Não coloque a chave no código. Use variáveis de ambiente; o Spring Boot mapeia automaticamente:

| Variável de ambiente           | Propriedade no app              |
|--------------------------------|----------------------------------|
| `APP_ABACATEPAY_API_KEY`       | `app.abacatepay.api-key`         |
| `APP_ABACATEPAY_WEBHOOK_SECRET`| `app.abacatepay.webhook-secret`  |
| `APP_ABACATEPAY_FRONTEND_BASE_URL` | `app.abacatepay.frontend-base-url` |

Exemplo (Linux/macOS):

```bash
export APP_ABACATEPAY_API_KEY="sua_chave_api"
export APP_ABACATEPAY_WEBHOOK_SECRET="secret_do_webhook"
./mvnw spring-boot:run
```

Exemplo (Windows PowerShell):

```powershell
$env:APP_ABACATEPAY_API_KEY="sua_chave_api"
$env:APP_ABACATEPAY_WEBHOOK_SECRET="secret_do_webhook"
.\mvnw.cmd spring-boot:run
```

Em servidor (systemd, Docker, etc.), defina essas variáveis no ambiente do processo.

---

## 2. Dashboard AbacatePay: chave de API e dev mode

1. Acesse o dashboard: **https://abacatepay.com** (ou o link que a AbacatePay indicar).
2. Faça login na sua conta.
3. **Chave de API**:
   - Vá em configurações / API / Chaves de API.
   - Crie uma nova chave.
   - **Dev mode**: se o toggle **Dev mode** estiver ativo, a chave é de **testes** (transações não reais).
   - **Produção**: desative o Dev mode e complete o processo de **verificação da conta** (documentos da empresa, sócios, etc.). Após aprovação, crie uma chave em ambiente de produção.
4. Copie a chave e guarde em local seguro. Use-a no `app.abacatepay.api-key` ou em `APP_ABACATEPAY_API_KEY`.

Documentação oficial: [Autenticação](https://docs.abacatepay.com/pages/authentication.md), [Indo para produção](https://docs.abacatepay.com/pages/production.md).

---

## 3. Como configurar o Webhook

O webhook permite que a AbacatePay avise o backend quando um pagamento for confirmado, para que o agendamento seja confirmado automaticamente.

### No dashboard AbacatePay

1. Acesse a área de **Webhooks** no dashboard.
2. Crie um novo webhook:
   - **Nome**: por exemplo `Ajeitai - Pagamentos`.
   - **URL**: a URL pública do seu backend + o path do webhook, **incluindo o secret na query**:
     ```text
     https://SEU_DOMINIO_BACKEND/api/webhooks/abacatepay?webhookSecret=UM_SECRET_FORTE_QUE_VOCE_ESCOLHER
     ```
   - **Secret**: use exatamente o mesmo valor que você colocou em `webhookSecret` na URL (ex.: `UM_SECRET_FORTE_QUE_VOCE_ESCOLHER`).
3. Salve. A AbacatePay passará a chamar essa URL em eventos como `billing.paid`.

### No backend (application ou variável de ambiente)

Use **o mesmo secret** que você colocou na URL do webhook:

```properties
app.abacatepay.webhook-secret=UM_SECRET_FORTE_QUE_VOCE_ESCOLHER
```

ou:

```bash
export APP_ABACATEPAY_WEBHOOK_SECRET="UM_SECRET_FORTE_QUE_VOCE_ESCOLHER"
```

O controller do backend compara o `webhookSecret` da query com esse valor; se não bater, retorna 401.

Documentação: [Webhooks AbacatePay](https://docs.abacatepay.com/pages/webhooks.md).

---

## 4. Fluxo de criação de cobrança e link de pagamento

Resumo do que acontece na integração:

1. **Cliente** agenda um serviço com forma de pagamento **ONLINE** (PIX).
2. **Prestador** aceita o agendamento no app.
3. O **backend** chama a API AbacatePay **POST /billing/create** enviando:
   - **Cliente**: nome, celular, e-mail, CPF do cliente do agendamento. A AbacatePay **cria ou associa** esse cliente à cobrança (não é necessário criar cliente antes no dashboard).
   - **Produto**: um item por cobrança (ex.: “Atendimento - Nome do Prestador”, valor em centavos, `externalId`: `ag-{id}` para identificar o agendamento).
   - **returnUrl** e **completionUrl**: redirecionamento após pagamento (ex.: `{frontendBaseUrl}/cliente/agendamentos`).
   - Opcional: **allowCoupons** e **coupons** (cupons criados no dashboard).
4. A AbacatePay retorna um **link de pagamento** (e o `billingId`). O backend grava o link em `Pagamento.linkPagamento` e envia/exibe para o cliente.
5. O **cliente** acessa o link e paga (PIX) na página da AbacatePay.
6. A AbacatePay envia um **webhook** `billing.paid` para `POST /api/webhooks/abacatepay?webhookSecret=...`.
7. O **backend** recebe o webhook, identifica o agendamento pelo `externalId` (`ag-{id}`), confirma o pagamento e o agendamento (status CONFIRMADO). O prestador pode então fazer check-in.

Nenhum **Produto** ou **Cliente** precisa ser criado manualmente no dashboard para esse fluxo: produto e cliente são enviados a cada **POST /billing/create**.

---

## 5. Produto, Cupom e Cliente na integração

### Produto

- **Não é necessário** cadastrar produto no dashboard para o fluxo atual.
- A cada cobrança o backend envia **um produto** no próprio request:
  - Nome: “Atendimento - {Nome do Prestador}”
  - Descrição: data/hora e prestador
  - Valor: valor do agendamento (em centavos)
  - `externalId`: `ag-{id}` (para o webhook identificar o agendamento).

Se no futuro você quiser usar produtos pré-cadastrados na AbacatePay, será preciso ajustar o backend para enviar o identificador desse produto no request (conforme documentação da API).

### Cliente

- **Criado/vinculado automaticamente** a cada cobrança.
- O backend envia sempre os dados do cliente do agendamento (nome, telefone, e-mail, CPF) no **POST /billing/create**. A AbacatePay cria ou associa o cliente à cobrança. Não é necessário criar cliente antes no dashboard.

### Cupom (coupon)

- **Opcional.** Se quiser permitir cupons na página de pagamento:
  1. Crie os cupons no **dashboard AbacatePay** (ex.: código `PROMO10`, valor/regras definidos lá).
  2. No backend, habilite e opcionalmente liste os códigos em `application.properties`:
     ```properties
     app.abacatepay.allow-coupons=true
     app.abacatepay.coupon-codes=PROMO10,ABKT5
     ```
  - O backend envia `allowCoupons: true` e a lista `coupons` no request de criação da cobrança. Máximo 50 códigos por request.

---

## 6. Resumo das propriedades (onde colocar cada valor)

| Propriedade                     | Onde obter / o que é                    | Onde colocar |
|---------------------------------|----------------------------------------|--------------|
| `app.abacatepay.api-key`        | Chave de API (dashboard AbacatePay)    | `application.properties` ou `APP_ABACATEPAY_API_KEY` |
| `app.abacatepay.webhook-secret` | Secret que você definir para o webhook | Mesmo valor na URL do webhook no dashboard + aqui ou `APP_ABACATEPAY_WEBHOOK_SECRET` |
| `app.abacatepay.frontend-base-url` | URL base do front (ex.: https://meuapp.com) | `application.properties` ou `APP_ABACATEPAY_FRONTEND_BASE_URL` |
| `app.abacatepay.base-url`       | Só alterar se a API mudar (default: https://api.abacatepay.com/v1) | Normalmente não precisa alterar |
| `app.abacatepay.allow-coupons`  | true para permitir cupons               | `application.properties` |
| `app.abacatepay.coupon-codes`   | Códigos criados no dashboard           | `application.properties` (lista separada por vírgula) |

---

## 7. Saindo do dev mode (produção)

1. No dashboard AbacatePay, inicie o processo de migração (ex.: botão “Dev mode” / “Ir para produção”).
2. Preencha dados da empresa, sócios e anexe os documentos solicitados.
3. Após aprovação (até ~24h), crie uma **nova chave de API** no ambiente de **produção**.
4. Troque no backend: use essa nova chave em `app.abacatepay.api-key` (ou `APP_ABACATEPAY_API_KEY`).
5. Configure um **webhook de produção** (URL pública HTTPS do seu backend em produção) e o mesmo `webhook-secret` no app.
6. Ajuste `app.abacatepay.frontend-base-url` para a URL do front em produção.

Assim o dashboard e a integração passam a operar em produção, com a chave e o webhook configurados nos lugares corretos.
